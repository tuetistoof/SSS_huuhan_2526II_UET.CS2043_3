package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.server.controller.NotificationController;
import com.ssscloud.auction.server.dao.AdminDAO;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;

/**
 * Integration test cho race condition giữa cancelAuction() và worker đang xử lý bid.
 *
 * CHIẾN LƯỢC TIMING (quan trọng):
 *
 *   Test A dùng 2 latch (KHÔNG dùng cancelDoneLatch):
 *     - workerPausedLatch: worker báo "tôi đã vào lockBidderBalance"
 *     - workerResumeLatch: cho phép worker tiếp tục
 *
 *   Thứ tự đúng:
 *     1. workerPausedLatch.await()      → chờ worker dừng tại lock
 *     2. cancelThread.start()           → bắt đầu cancelAuction()
 *     3. sleep(100)                     → đủ để cancel set CANCELED
 *     4. workerResumeLatch.countDown()  → resume worker NGAY (không chờ cancel xong)
 *     5. cancelThread.join()            → chờ cancel hoàn thành
 *     6. workerThread.join()
 *
 *   LÝ DO không thể resume AFTER cancel:
 *   cancelAuction() → shutdown() → workerThread.join(5000).
 *   Worker đang block trong workerResumeLatch.await() → join deadlock 5 giây.
 *   Phải resume worker TRƯỚC để shutdown().join() unblock được.
 */
public class CancelAuctionRaceConditionTest {

    private static final String AUCTION_ID = "race-test-auction-001";
    private static final String SELLER_ID  = "seller-001";
    private static final String ITEM_ID    = "item-001";
    private static final String BIDDER_OLD = "bidder-old-001";
    private static final String BIDDER_NEW = "bidder-new-002";
    private static final String REASON     = "Test cancel reason";

    private static final long START_PRICE = 30_000L;
    private static final long OLD_BID     = 40_000L;
    private static final long NEW_BID     = 55_000L;

    private UserDAO                userDAO;
    private AuctionDAO             auctionDAO;
    private BidTransactionDAO      bidTransactionDAO;
    private AutoBidService         autoBidService;
    private NotificationController notificationController;
    private AdminDAO               adminDAO;

    private AdminService adminService;
    private Auction      auction;

    @BeforeEach
    void setUp() throws Exception {
        userDAO               = mock(UserDAO.class);
        auctionDAO            = mock(AuctionDAO.class);
        bidTransactionDAO     = mock(BidTransactionDAO.class);
        autoBidService        = mock(AutoBidService.class);
        notificationController = mock(NotificationController.class);
        adminDAO              = mock(AdminDAO.class);

        doNothing().when(notificationController).notifyWatchers(anyString(), anyString());
        adminService = new AdminService(adminDAO, auctionDAO, autoBidService, userDAO);

        AuctionConfig config = new AuctionConfig(
            AUCTION_ID, "Race Condition Test Auction",
            START_PRICE, 1_000L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        auction = new Auction(config, AuctionStatus.RUNNING, SELLER_ID, ITEM_ID);
        AuctionRegistry.getInstance().register(auction);

        ConcurrentBidManager.initialize(
            userDAO, bidTransactionDAO, autoBidService, auctionDAO, notificationController
        );
    }

    @AfterEach
    void tearDown() {
        AuctionRegistry.getInstance().remove(AUCTION_ID);
        ConcurrentBidManager.resetInstance();
    }

    // =========================================================================
    // Test A — cancelAuction khi worker đang giữa lockBidderBalance → placeBid
    // =========================================================================

    /**
     * KỊCH BẢN A:
     *   T=0  Worker: unlock BIDDER_OLD → lockBidderBalance(BIDDER_NEW) [DỪNG]
     *   T=1  workerPausedLatch.countDown()
     *   T=2  cancelThread.start() → auction.cancel() → status = CANCELED
     *   T=3  workerResumeLatch.countDown() → worker tiếp tục
     *   T=4  Checkpoint 2: status = CANCELED → rollback unlockBidderBalance(BIDDER_NEW)
     *   T=5  shutdown().join() unblock → cancelAuction() xong
     *
     * VERIFY:
     *   - unlockBidderBalance(BIDDER_OLD, OLD_BID): từ cancel refundWinner()
     *   - unlockBidderBalance(BIDDER_NEW, NEW_BID): từ Checkpoint 2 rollback
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testA_cancelWhileWorkerBetweenLockAndPlaceBid_bidderNewMoneyMustBeUnlocked() throws Exception {
        // ── Sắp xếp (Arrange) ────────────────────────────────────────────────
        // Đặt auction đang có BIDDER_OLD là highest bidder với OLD_BID
        auction.placeBid(new com.ssscloud.auction.common.model.BidTransaction(
                AUCTION_ID, BIDDER_OLD, "bidder_old_username", OLD_BID, LocalDateTime.now().minusMinutes(5), BidType.MANUAL
        ));
        assertEquals(BIDDER_OLD, auction.getHighestBidderId(), "Setup: BIDDER_OLD phải là highest bidder");

        CountDownLatch workerPausedLatch = new CountDownLatch(1); // Báo worker đã tới điểm chặn
        CountDownLatch cancelDoneLatch = new CountDownLatch(1); // Báo cancel đã xong
        CountDownLatch workerResumeLatch = new CountDownLatch(1); // Cho phép worker tiếp tục

        when(userDAO.lockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);

        // Chặn luồng worker NGAY LÚC nó khóa tiền BIDDER_NEW
        // (tức là trước khi nó kịp cập nhật highestBidder mới vào auction)
        doAnswer(invocation -> {
            workerPausedLatch.countDown();
            try {
                // Đợi admin cancel gọi thả luồng
                workerResumeLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Luồng có thể bị interrupt vì Admin gọi shutdown()
                Thread.currentThread().interrupt();
            }
            return true;
        }).when(userDAO).lockBidderBalance(eq(BIDDER_NEW), eq(NEW_BID));

        // ── Thực thi (Act) ───────────────────────────────────────────────────
        AtomicReference<Exception> cancelError = new AtomicReference<>();

        // Ném task vào queue để worker của ConcurrentBidManager chạy
        Thread submitThread = new Thread(() -> {
            try {
                ConcurrentBidManager.getInstance().submitBid(
                        auction, BIDDER_NEW, "bidder_new_username", NEW_BID, BidType.MANUAL
                );
            } catch (Exception e) {
                // Bỏ qua lỗi submit nếu có
            }
        });
        submitThread.start();

        // Chờ worker lấy task ra xử lý và chạy tới hàm lockBidderBalance
        boolean paused = workerPausedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(paused, "Worker phải chạy và bị chặn tại lockBidderBalance trong 5 giây");
        
        // Luồng Admin: gọi cancelAuction() trong khi worker đang bị treo
        Thread cancelThread = new Thread(() -> {
            try {
                adminService.cancelAuction(AUCTION_ID, REASON);
            } catch (Exception e) {
                cancelError.set(e);
            } finally {
                cancelDoneLatch.countDown();
            }
        }, "test-cancel-thread");
        cancelThread.start();

        // 1. Cho luồng Admin chạy một chút để kịp set status = CANCELED
        Thread.sleep(100); 

        // 2. THẢ WORKER RA TRƯỚC để worker nhận biết auction đã bị cancel. 
        // Tránh lỗi deadlock khi cancelAuction() gọi hàm shutdown().join() đợi worker tắt.
        workerResumeLatch.countDown(); 

        // 3. Bây giờ mới chờ Admin cancel hoàn tất
        boolean cancelDone = cancelDoneLatch.await(5, TimeUnit.SECONDS);
        assertTrue(cancelDone, "cancelAuction() phải hoàn thành trong 5 giây");
        
        cancelThread.join(3000);
        submitThread.join(3000);

        // ── Kiểm tra (Assert) ────────────────────────────────────────────────
        assertNull(cancelError.get(), "cancelAuction() không được throw exception: " + cancelError.get());

        // Admin gọi cancel khi highestBidder của auction vẫn đang là BIDDER_OLD, 
        // nên admin sẽ hoàn tiền cho BIDDER_OLD
        verify(userDAO, atLeastOnce()).unlockBidderBalance(eq(BIDDER_OLD), eq(OLD_BID));

        // VẤN ĐỀ CỐT LÕI: Tiền của BIDDER_NEW đã bị khóa (hàm lock trả về true) 
        // nhưng bid chưa được ghi nhận. Vì hệ thống bị admin cancel và xóa luồng worker ngay lúc này, 
        // BIDDER_NEW cũng PHẢI ĐƯỢC HOÀN TIỀN (unlock).
        verify(userDAO, atLeastOnce()).unlockBidderBalance(eq(BIDDER_NEW), eq(NEW_BID));

        assertEquals(AuctionStatus.CANCELED, auction.getStatus(), "Auction phải đổi sang trạng thái CANCELED");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testB_cancelAfterWorkerPlacedBid_correctWinnerMustBeUnlocked() throws Exception {
        // Arrange
        auction.placeBid(new BidTransaction(
            AUCTION_ID, BIDDER_OLD, "bidder_old_username", OLD_BID,
            LocalDateTime.now().minusMinutes(5), BidType.MANUAL
        ));
        assertEquals(BIDDER_OLD, auction.getHighestBidderId(), "Setup: BIDDER_OLD phải là highest bidder");

        CountDownLatch workerPlacedBidLatch = new CountDownLatch(1);
        CountDownLatch workerResumeLatch    = new CountDownLatch(1);

        doAnswer(invocation -> {
            workerPlacedBidLatch.countDown();
            boolean resumed = workerResumeLatch.await(5, TimeUnit.SECONDS);
            assertTrue(resumed, "Worker phải được resume trong 5 giây");
            return true;
        }).when(bidTransactionDAO).saveBidTransaction(any(BidTransaction.class));

        when(userDAO.lockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);

        // Act
        AtomicReference<Exception> workerError = new AtomicReference<>();
        AtomicReference<Exception> cancelError = new AtomicReference<>();

        Thread workerThread = new Thread(() -> {
            try {
                ConcurrentBidManager.getInstance().submitBid(
                    auction, BIDDER_NEW, "bidder_new_username", NEW_BID, BidType.MANUAL
                );
                Thread.sleep(300);
            } catch (Exception e) {
                workerError.set(e);
            }
        }, "test-worker-thread-B");
        workerThread.start();

        boolean placed = workerPlacedBidLatch.await(5, TimeUnit.SECONDS);
        assertTrue(placed, "Worker phải đã gọi placeBid trong 5 giây");

        Thread cancelThread = new Thread(() -> {
            try {
                adminService.cancelAuction(AUCTION_ID, REASON);
            } catch (Exception e) {
                cancelError.set(e);
            }
        }, "test-cancel-thread-B");
        cancelThread.start();

        Thread.sleep(100);

        // Resume worker để tránh deadlock với shutdown().join()
        workerResumeLatch.countDown();

        cancelThread.join(6000);
        workerThread.join(3000);

        // Assert
        assertNull(cancelError.get(), "cancelAuction() không được throw: " + cancelError.get());

        verify(userDAO, atLeastOnce()).unlockBidderBalance(eq(BIDDER_NEW), eq(NEW_BID));
        verify(userDAO, times(1)).unlockBidderBalance(eq(BIDDER_OLD), eq(OLD_BID));

        assertEquals(AuctionStatus.CANCELED, auction.getStatus(),
            "Auction phải là CANCELED sau cancelAuction()");

        assertThrows(Exception.class, () ->
            ConcurrentBidManager.getInstance().submitBid(
                auction, "any-bidder", "any_username", NEW_BID + 10_000L, BidType.MANUAL
            ),
            "Sau shutdown, submitBid phải throw"
        );
    }

    // =========================================================================
    // Test C — Stress: nhiều bid + cancel đồng thời, verify balance invariant
    // =========================================================================

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testC_stressTest_cancelDuringActiveBidding_totalLockedBalanceMustBeZero() throws Exception {
        int NUM_BIDDERS = 5;

        java.util.concurrent.atomic.AtomicLong totalLocked   = new java.util.concurrent.atomic.AtomicLong(0);
        java.util.concurrent.atomic.AtomicLong totalUnlocked = new java.util.concurrent.atomic.AtomicLong(0);

        doAnswer(inv -> {
            totalLocked.addAndGet((long) inv.getArgument(1));
            return true;
        }).when(userDAO).lockBidderBalance(anyString(), anyLong());

        doAnswer(inv -> {
            totalUnlocked.addAndGet((long) inv.getArgument(1));
            return true;
        }).when(userDAO).unlockBidderBalance(anyString(), anyLong());

        CountDownLatch allBidsDone = new CountDownLatch(NUM_BIDDERS);
        long baseAmount = START_PRICE + 5_000L;

        for (int i = 0; i < NUM_BIDDERS; i++) {
            final String bidderId = "stress-bidder-" + i;
            final long   bidAmt   = baseAmount + (i * 2_000L);
            new Thread(() -> {
                try {
                    ConcurrentBidManager.getInstance().submitBid(
                        auction, bidderId, bidderId + "_name", bidAmt, BidType.MANUAL
                    );
                } catch (Exception ignored) {
                } finally {
                    allBidsDone.countDown();
                }
            }, "stress-" + bidderId).start();
        }

        Thread.sleep(50);
        try {
            adminService.cancelAuction(AUCTION_ID, REASON);
        } catch (Exception ignored) {}

        allBidsDone.await(5, TimeUnit.SECONDS);
        Thread.sleep(500);

        assertEquals(totalLocked.get(), totalUnlocked.get(),
            String.format(
                "BALANCE LEAK! locked=%d unlocked=%d chênh lệch=%d",
                totalLocked.get(), totalUnlocked.get(),
                totalLocked.get() - totalUnlocked.get()
            )
        );
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }
}
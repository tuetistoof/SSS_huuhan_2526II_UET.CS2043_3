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
 * HAI KỊCH BẢN CẦN TEST:
 *
 * Test A — Worker đang GIỮ CHỪNG giữa lockBidderBalance → placeBid,
 *           admin gọi cancelAuction() ngay lúc đó.
 *   Mục đích: đảm bảo bidder MỚI được unlockBidderBalance đúng sau cancel.
 *   Bug nếu sai: worker lock tiền bidder mới, cancel chỉ thấy bidder cũ → tiền bị kẹt.
 *
 * Test B — Worker vừa xong placeBid (auction.getHighestBidderId() đã đổi),
 *           admin gọi cancelAuction() ngay lúc đó.
 *   Mục đích: đảm bảo winner là bidder MỚI NHẤT, được unlock đúng số tiền.
 *   Bug nếu sai: cancel đọc snapshot cũ → unlock sai người hoặc sai số tiền.
 *
 * CHIẾN LƯỢC: dùng CountDownLatch để control timing deterministically.
 *   - workerPausedLatch: báo hiệu "worker đã dừng tại điểm kiểm soát"
 *   - cancelDoneLatch:   báo hiệu "cancelAuction() đã xong"
 *   - workerResumeLatch: cho phép worker tiếp tục sau khi cancel xong
 */
public class CancelAuctionRaceConditionTest {

    private static final String AUCTION_ID   = "race-test-auction-001";
    private static final String SELLER_ID    = "seller-001";
    private static final String ITEM_ID      = "item-001";
    private static final String BIDDER_OLD   = "bidder-old-001";
    private static final String BIDDER_NEW   = "bidder-new-002";
    private static final String REASON       = "Test cancel reason";

    private static final long START_PRICE    = 30_000L;
    private static final long OLD_BID        = 40_000L;  // bidder cũ đã bid
    private static final long NEW_BID        = 55_000L;  // bidder mới sắp bid

    private UserDAO              userDAO;
    private AuctionDAO           auctionDAO;
    private BidTransactionDAO    bidTransactionDAO;
    private AutoBidService       autoBidService;
    private NotificationController notificationController;
    private AdminDAO             adminDAO;

    private AdminService         adminService;
    private Auction              auction;

    // ── Setup / Teardown ─────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        userDAO              = mock(UserDAO.class);
        auctionDAO           = mock(AuctionDAO.class);
        bidTransactionDAO    = mock(BidTransactionDAO.class);
        autoBidService       = mock(AutoBidService.class);
        notificationController = mock(NotificationController.class);
        adminDAO             = mock(AdminDAO.class);

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
     *   T=0  Worker bid NEW_BID: unlock bidder cũ (OLD_BID), sau đó GỌI lockBidderBalance(BIDDER_NEW, NEW_BID)
     *   T=1  [WORKER DỪNG] → workerPausedLatch.countDown()
     *   T=2  Thread chính nhận tín hiệu, gọi cancelAuction()
     *         → auction.cancel() → refundWinner() → đọc highestBidderId = BIDDER_OLD (chưa đổi)
     *         → unlockBidderBalance(BIDDER_OLD, OLD_BID) ← cần verify
     *   T=3  cancelDoneLatch.countDown() → worker tiếp tục: placeBid(BIDDER_NEW)
     *         → cancel đã xong, nhưng shutdown đã interrupt worker → bid có thể bị bỏ
     *
     * ĐIỀU CẦN VERIFY:
     *   - unlockBidderBalance(BIDDER_OLD, OLD_BID) được gọi đúng 1 lần (refund winner tại thời điểm cancel)
     *   - unlockBidderBalance(BIDDER_NEW, NEW_BID) CŨNG được gọi — vì tiền bidder mới đã bị lock
     *     nhưng bid chưa vào auction. Nếu không unlock → tiền BIDDER_NEW bị kẹt.
     *
     * WHY TEST NÀY QUAN TRỌNG:
     *   Unit test hiện tại chỉ mock ConcurrentBidManager, không chạy worker thật.
     *   Race condition này chỉ xảy ra khi worker đang ở giữa lock→place, và cancel
     *   đọc state auction trước khi worker commit. Unit test không bắt được điều này.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testA_cancelWhileWorkerBetweenLockAndPlaceBid_bidderNewMoneyMustBeUnlocked() throws Exception {
        // ── Sắp xếp (Arrange) ────────────────────────────────────────────────

        // Đặt auction đang có BIDDER_OLD là highest bidder với OLD_BID
        auction.placeBid(new com.ssscloud.auction.common.model.BidTransaction(
            AUCTION_ID, BIDDER_OLD, "bidder_old_username", OLD_BID,
            LocalDateTime.now().minusMinutes(5), BidType.MANUAL
        ));
        assertEquals(BIDDER_OLD, auction.getHighestBidderId(), "Setup: BIDDER_OLD phải là highest bidder");

        // 3 latch để control timing
        CountDownLatch workerPausedLatch = new CountDownLatch(1); // worker báo hiệu đã dừng
        CountDownLatch cancelDoneLatch   = new CountDownLatch(1); // cancel báo hiệu đã xong
        CountDownLatch workerResumeLatch = new CountDownLatch(1); // cho phép worker tiếp tục

        // Mock lockBidderBalance: khi được gọi với BIDDER_NEW → dừng, chờ cancel xong
        doAnswer(invocation -> {
            String bidderId = invocation.getArgument(0);
            if (BIDDER_NEW.equals(bidderId)) {
                workerPausedLatch.countDown();     // báo hiệu "tôi đã vào giữa lock"
                // Chờ cancel xong trước khi tiếp tục (simulate slow DB or context switch)
                boolean resumed = workerResumeLatch.await(5, TimeUnit.SECONDS);
                assertTrue(resumed, "Worker phải được resume trong 5 giây");
            }
            return true;
        }).when(userDAO).lockBidderBalance(anyString(), anyLong());

        // unlockBidderBalance cần hoạt động bình thường (không throw)
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);

        // ── Thực thi (Act) ───────────────────────────────────────────────────

        AtomicReference<Exception> workerError  = new AtomicReference<>();
        AtomicReference<Exception> cancelError  = new AtomicReference<>();

        // Thread 1: Worker submit bid mới (BIDDER_NEW, NEW_BID)
        Thread workerThread = new Thread(() -> {
            try {
                ConcurrentBidManager.getInstance().submitBid(
                    auction, BIDDER_NEW, "bidder_new_username", NEW_BID, BidType.MANUAL
                );
                // Chờ đủ để worker bắt đầu xử lý
                Thread.sleep(200);
            } catch (Exception e) {
                workerError.set(e);
            }
        }, "test-worker-thread");
        workerThread.start();

        // Chờ worker dừng giữa lockBidderBalance
        boolean paused = workerPausedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(paused, "Worker phải dừng tại lockBidderBalance trong 5 giây");

        // Thread chính: gọi cancelAuction() NGAY LÚC worker đang dừng giữa lock→place
        Thread cancelThread = new Thread(() -> {
            try {
                adminService.cancelAuction(AUCTION_ID, REASON);
            } catch (Exception e) {
                cancelError.set(e);
            } finally {
                cancelDoneLatch.countDown(); // báo cancel xong
            }
        }, "test-cancel-thread");
        cancelThread.start();

        // Chờ cancel xong, sau đó resume worker
        boolean cancelDone = cancelDoneLatch.await(5, TimeUnit.SECONDS);
        assertTrue(cancelDone, "cancelAuction() phải hoàn thành trong 5 giây");
        workerResumeLatch.countDown(); // cho worker tiếp tục

        // Chờ cả 2 thread hoàn thành
        workerThread.join(3000);
        cancelThread.join(3000);

        // ── Kiểm tra (Assert) ────────────────────────────────────────────────

        assertNull(cancelError.get(), "cancelAuction() không được throw exception: " + cancelError.get());

        // BIDDER_OLD phải được unlock (là winner tại thời điểm cancel)
        verify(userDAO, atLeastOnce()).unlockBidderBalance(eq(BIDDER_OLD), eq(OLD_BID));

        // BIDDER_NEW ĐÃ bị lock tiền nhưng bid chưa vào auction →
        // Sau khi cancel, worker tiếp tục nhưng auction đã CANCELED → bid bị reject,
        // nhưng tiền đã bị lock bởi worker → cần verify unlock xảy ra.
        //
        // NOTE: Hành vi đúng là worker phải kiểm tra auction.getStatus().isActive()
        // TRƯỚC khi commit placeBid. Nếu code đúng, worker sẽ thấy CANCELED và rollback.
        // Nếu code sai (không kiểm tra), BIDDER_NEW mất tiền.
        //
        // Test này document behavior mong muốn: cả BIDDER_OLD và BIDDER_NEW đều phải được unlock.
        verify(userDAO, atLeastOnce()).unlockBidderBalance(eq(BIDDER_NEW), eq(NEW_BID));

        // Sau cancel, auction phải là CANCELED
        assertEquals(AuctionStatus.CANCELED, auction.getStatus(),
            "Auction phải là CANCELED sau cancelAuction()");

        // AuctionRegistry phải đã remove auction
        assertNull(AuctionRegistry.getInstance().getLiveAuction(AUCTION_ID),
            "Auction phải được remove khỏi AuctionRegistry sau cancel");
    }

    // =========================================================================
    // Test B — cancelAuction khi worker vừa xong placeBid
    // =========================================================================

    /**
     * KỊCH BẢN B:
     *   T=0  Worker bid NEW_BID: unlock BIDDER_OLD, lock BIDDER_NEW, gọi placeBid()
     *         → auction.getHighestBidderId() = BIDDER_NEW, getCurrentPrice() = NEW_BID
     *   T=1  [WORKER DỪNG sau placeBid, trước khi save DB] → workerPausedLatch.countDown()
     *   T=2  Thread chính gọi cancelAuction()
     *         → refundWinner() đọc highestBidderId = BIDDER_NEW, winningBid = NEW_BID
     *         → unlockBidderBalance(BIDDER_NEW, NEW_BID)
     *   T=3  cancelDoneLatch.countDown() → worker tiếp tục (save DB, nhưng auction đã cancel)
     *
     * ĐIỀU CẦN VERIFY:
     *   - unlockBidderBalance(BIDDER_NEW, NEW_BID) được gọi đúng 1 lần — đúng người, đúng tiền
     *   - unlockBidderBalance(BIDDER_OLD, OLD_BID) KHÔNG được gọi thêm lần nữa bởi cancel
     *     (BIDDER_OLD đã được unlock bởi worker khi BIDDER_NEW outbid)
     *
     * WHY TEST NÀY QUAN TRỌNG:
     *   Sau khi worker gọi auction.placeBid(), highestBidderId đổi sang BIDDER_NEW.
     *   cancelAuction() đọc auction.getHighestBidderId() → phải thấy BIDDER_NEW,
     *   không phải BIDDER_OLD (stale read). Nếu có race condition trong getHighestBidderId()
     *   hoặc lock không đúng thứ tự → cancel unlock sai người.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testB_cancelAfterWorkerPlacedBid_correctWinnerMustBeUnlocked() throws Exception {
        // ── Sắp xếp (Arrange) ────────────────────────────────────────────────

        auction.placeBid(new com.ssscloud.auction.common.model.BidTransaction(
            AUCTION_ID, BIDDER_OLD, "bidder_old_username", OLD_BID,
            LocalDateTime.now().minusMinutes(5), BidType.MANUAL
        ));
        assertEquals(BIDDER_OLD, auction.getHighestBidderId(), "Setup: BIDDER_OLD phải là highest bidder");

        CountDownLatch workerPlacedBidLatch = new CountDownLatch(1); // worker báo đã gọi placeBid
        CountDownLatch cancelDoneLatch      = new CountDownLatch(1); // cancel báo hiệu đã xong
        CountDownLatch workerResumeLatch    = new CountDownLatch(1); // cho phép worker tiếp tục

        // Intercept tại bidTransactionDAO.saveBidTransaction (sau placeBid, trước khi save DB)
        // → đây là điểm dừng sau khi auction state đã được cập nhật
        doAnswer(invocation -> {
            // Worker đã gọi auction.placeBid() và đang chuẩn bị save
            workerPlacedBidLatch.countDown();
            // Chờ cancel xong
            boolean resumed = workerResumeLatch.await(5, TimeUnit.SECONDS);
            assertTrue(resumed, "Worker phải được resume trong 5 giây");
            return true;
        }).when(bidTransactionDAO).saveBidTransaction(
            any(com.ssscloud.auction.common.model.BidTransaction.class)
        );

        when(userDAO.lockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);

        // ── Thực thi (Act) ───────────────────────────────────────────────────

        AtomicReference<Exception> workerError = new AtomicReference<>();
        AtomicReference<Exception> cancelError = new AtomicReference<>();

        // Thread 1: Worker submit bid mới
        Thread workerThread = new Thread(() -> {
            try {
                ConcurrentBidManager.getInstance().submitBid(
                    auction, BIDDER_NEW, "bidder_new_username", NEW_BID, BidType.MANUAL
                );
                Thread.sleep(300); // đủ để worker bắt đầu xử lý
            } catch (Exception e) {
                workerError.set(e);
            }
        }, "test-worker-thread-B");
        workerThread.start();

        // Chờ worker đã gọi placeBid (auction state đã đổi sang BIDDER_NEW)
        boolean placed = workerPlacedBidLatch.await(5, TimeUnit.SECONDS);
        assertTrue(placed, "Worker phải đã gọi placeBid trong 5 giây");

        // Tại đây: auction.getHighestBidderId() = BIDDER_NEW, getCurrentPrice() = NEW_BID

        // Thread chính: gọi cancelAuction()
        Thread cancelThread = new Thread(() -> {
            try {
                adminService.cancelAuction(AUCTION_ID, REASON);
            } catch (Exception e) {
                cancelError.set(e);
            } finally {
                cancelDoneLatch.countDown();
            }
        }, "test-cancel-thread-B");
        cancelThread.start();

        boolean cancelDone = cancelDoneLatch.await(5, TimeUnit.SECONDS);
        assertTrue(cancelDone, "cancelAuction() phải hoàn thành trong 5 giây");
        workerResumeLatch.countDown(); // cho worker tiếp tục (save DB, nhưng đã canceled)

        workerThread.join(3000);
        cancelThread.join(3000);

        // ── Kiểm tra (Assert) ────────────────────────────────────────────────

        assertNull(cancelError.get(), "cancelAuction() không được throw exception: " + cancelError.get());

        // BIDDER_NEW phải là winner tại thời điểm cancel → phải được unlock đúng số tiền
        verify(userDAO, atLeastOnce()).unlockBidderBalance(eq(BIDDER_NEW), eq(NEW_BID));

        // BIDDER_OLD đã được unlock bởi worker (khi BIDDER_NEW outbid) → 
        // cancel KHÔNG được unlock BIDDER_OLD thêm lần nữa (double-unlock)
        // Worker đã gọi: unlockBidderBalance(BIDDER_OLD, OLD_BID) 1 lần
        // Cancel KHÔNG được gọi thêm: unlockBidderBalance(BIDDER_OLD, OLD_BID)
        verify(userDAO, times(1)).unlockBidderBalance(eq(BIDDER_OLD), eq(OLD_BID));
        // (1 lần duy nhất = từ worker, KHÔNG phải từ cancel)

        // Auction phải CANCELED
        assertEquals(AuctionStatus.CANCELED, auction.getStatus(),
            "Auction phải là CANCELED sau cancelAuction()");

        // Sau cancel, ConcurrentBidManager không còn chấp nhận bid mới cho auction này
        assertThrows(Exception.class, () ->
            ConcurrentBidManager.getInstance().submitBid(
                auction, "any-bidder", "any_username", NEW_BID + 10_000L, BidType.MANUAL
            ),
            "Sau shutdown, submitBid phải throw — closedAuctions guard phải active"
        );
    }

    // =========================================================================
    // Test C — Stress: nhiều bid liên tục + cancel đồng thời
    // =========================================================================

    /**
     * KỊCH BẢN C (Stress Test bổ sung):
     *   N bid từ nhiều bidder được submit đồng thời.
     *   Sau random delay nhỏ, cancel được gọi.
     *   Verify: tổng locked_balance sau cancel = 0 (mọi lock đều được giải phóng).
     *
     * WHY: bổ sung cho Test A/B (deterministic). Stress test này bắt các race condition
     *   không ngờ tới mà timing window quá hẹp để test deterministially.
     *
     * NOTE: test này KHÔNG thể verify "chính xác ai được unlock bao nhiêu" vì
     *   outcome phụ thuộc vào timing. Chỉ verify tổng balance invariant.
     */
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testC_stressTest_cancelDuringActiveBidding_totalLockedBalanceMustBeZero() throws Exception {
        // ── Sắp xếp ──────────────────────────────────────────────────────────

        int NUM_BIDDERS = 5;
        // Track tổng tiền đã lock/unlock bằng AtomicLong
        java.util.concurrent.atomic.AtomicLong totalLocked   = new java.util.concurrent.atomic.AtomicLong(0);
        java.util.concurrent.atomic.AtomicLong totalUnlocked = new java.util.concurrent.atomic.AtomicLong(0);

        doAnswer(inv -> {
            long amount = inv.getArgument(1);
            totalLocked.addAndGet(amount);
            return true;
        }).when(userDAO).lockBidderBalance(anyString(), anyLong());

        doAnswer(inv -> {
            long amount = inv.getArgument(1);
            totalUnlocked.addAndGet(amount);
            return true;
        }).when(userDAO).unlockBidderBalance(anyString(), anyLong());

        CountDownLatch allBidsDone = new CountDownLatch(NUM_BIDDERS);

        // ── Thực thi ─────────────────────────────────────────────────────────

        long baseAmount = START_PRICE + 5_000L;
        for (int i = 0; i < NUM_BIDDERS; i++) {
            final String bidderId = "stress-bidder-" + i;
            final long   bidAmt   = baseAmount + (i * 2_000L); // tăng dần để chỉ có 1 bid thắng
            new Thread(() -> {
                try {
                    ConcurrentBidManager.getInstance().submitBid(
                        auction, bidderId, bidderId + "_name", bidAmt, BidType.MANUAL
                    );
                } catch (Exception ignored) {
                    // Có thể throw nếu auction đã closed — OK
                } finally {
                    allBidsDone.countDown();
                }
            }, "stress-bidder-" + bidderId).start();
        }

        // Cancel sau 50ms (đủ để một vài bid được submit)
        Thread.sleep(50);
        try {
            adminService.cancelAuction(AUCTION_ID, REASON);
        } catch (Exception ignored) {
            // Nếu auction đã bị cancel hoặc state sai → test sẽ fail ở assert bên dưới
        }

        // Chờ tất cả bid thread hoàn thành
        allBidsDone.await(5, TimeUnit.SECONDS);

        // Chờ thêm để worker xử lý hết queue
        Thread.sleep(500);

        // ── Kiểm tra ─────────────────────────────────────────────────────────

        // INVARIANT: sau cancel, tổng tiền được unlock phải bằng tổng tiền đã lock.
        // Nếu locked > unlocked → có bidder bị mất tiền (leak).
        assertEquals(totalLocked.get(), totalUnlocked.get(),
            String.format(
                "BALANCE LEAK DETECTED! Tổng locked=%d nhưng tổng unlocked=%d. " +
                "Chênh lệch=%d — có bidder bị mất tiền.",
                totalLocked.get(), totalUnlocked.get(),
                totalLocked.get() - totalUnlocked.get()
            )
        );

        // Auction phải CANCELED
        assertEquals(AuctionStatus.CANCELED, auction.getStatus(),
            "Auction phải là CANCELED");
    }
}
package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.model.auction.BidTransaction;
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
 * ═══════════════════════════════════════════════════════════════════════════════
 * KIẾN TRÚC MỚI CỦA processTask() — ĐỌC TRƯỚC KHI ĐỌC CÁC TEST:
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * processTask() KHÔNG gọi lockBidderBalance() nữa.
 * Lock đã xảy ra ở BidService trước submitBid(). 
 * Tham số mới: submitBid(..., lockAmount) để worker biết phải unlock bao nhiêu.
 *
 * Flow của processTask() khi bid THẮNG:
 *   1. Check isActive()                               ← checkpoint 1
 *   2. unlockBidderBalance(previousBidderId, lockAmt) ← ĐIỂM CHẶN cho Test A
 *   3. previousWinnerBidtask.put(auctionId, task)
 *   4. updatePendingBalance(seller, delta)
 *   5. auctionEntity.placeBid(bidTransaction)
 *   6. bidTransactionDAO.saveBidTransaction()         ← ĐIỂM CHẶN cho Test B
 *   7. ChangeManager.notify / notifyWatchers
 *
 * Flow khi bid THẤT BẠI (amount <= currentPrice):
 *   → unlockBidderBalance(task.bidderId, task.lockAmount) ← unlock ngay, không leak
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * CHIẾN LƯỢC TIMING CHUNG:
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 *   cancelAuction() gọi shutdown() → workerThread.join(5000).
 *   Nếu worker đang block → join deadlock 5 giây → test timeout.
 *
 *   LUÔN resume worker TRƯỚC khi chờ cancel hoàn tất:
 *     1. workerPausedLatch.await()      → chờ worker dừng tại điểm chặn
 *     2. cancelThread.start()           → bắt đầu cancelAuction()
 *     3. sleep(100)                     → đủ để cancel set status = CANCELED
 *     4. workerResumeLatch.countDown()  → RESUME WORKER TRƯỚC
 *     5. cancelDoneLatch.await()        → chờ cancel hoàn tất
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class CancelAuctionRaceConditionTest {

    private static final String AUCTION_ID = "race-test-auction-001";
    private static final String SELLER_ID  = "seller-001";
    private static final String ITEM_ID    = "item-001";
    private static final String BIDDER_OLD = "bidder-old-001";
    private static final String BIDDER_NEW = "bidder-new-002";
    private static final String REASON     = "Test cancel reason";

    private static final long START_PRICE = 30_000L;
    private static final long OLD_BID     = 40_000L;  // lockAmount của BIDDER_OLD
    private static final long NEW_BID     = 55_000L;  // bidAmount & lockAmount của BIDDER_NEW

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
        userDAO                = mock(UserDAO.class);
        auctionDAO             = mock(AuctionDAO.class);
        bidTransactionDAO      = mock(BidTransactionDAO.class);
        autoBidService         = mock(AutoBidService.class);
        notificationController = mock(NotificationController.class);
        adminDAO               = mock(AdminDAO.class);

        AuctionRegistry.initialize(auctionDAO);

        doNothing().when(notificationController).notifyWatchers(any(Auction.class), anyString());
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(anyString(), anyLong())).thenReturn(true);

        adminService = new AdminService(adminDAO, auctionDAO, autoBidService, userDAO);

        AuctionConfig config = new AuctionConfig(
            AUCTION_ID, "Race Condition Test Auction",
            START_PRICE, 1_000L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        auction = new Auction(config, AuctionStatus.RUNNING, SELLER_ID, ITEM_ID);
        AuctionRegistry.getInstance().registerIfAbsent(auction);

        ConcurrentBidManager.initialize(
            userDAO, bidTransactionDAO, autoBidService, auctionDAO, notificationController
        );
    }

    @AfterEach
    void tearDown() {
        try {
            ConcurrentBidManager.getInstance().shutdown(AUCTION_ID);
        } catch (Exception ignored) {}
        AuctionRegistry.getInstance().remove(AUCTION_ID);
        ConcurrentBidManager.resetInstance();
    }

    // =========================================================================
    // Test A — cancel khi worker đang giữa unlockBidderBalance(OLD) → placeBid
    // =========================================================================

    /**
     * KỊCH BẢN A:
     *
     *   BIDDER_OLD đang giữ giá 40_000 (đã được setup bằng previousWinnerBidtask giả lập).
     *   Worker xử lý bid của BIDDER_NEW (55_000):
     *
     *     Step 1: unlockBidderBalance(BIDDER_OLD, OLD_BID)  ← CHẶN tại đây
     *     Step 2: [worker bị dừng] cancel chạy → status = CANCELED
     *     Step 3: [worker resume] → check isActive() FAIL → rollback:
     *             unlockBidderBalance(BIDDER_NEW, NEW_BID)
     *
     * WHY chặn tại unlockBidderBalance(BIDDER_OLD):
     *   processTask() không gọi lockBidderBalance() nữa — điểm dừng đầu tiên
     *   có thể mock là unlock winner cũ. Đây là bước 2 trong processTask().
     *
     * VERIFY:
     *   - unlockBidderBalance(BIDDER_OLD, OLD_BID): worker tự unlock trong processTask
     *   - unlockBidderBalance(BIDDER_NEW, NEW_BID): rollback của worker sau khi detect CANCELED
     *     HOẶC từ refundWinner() nếu BIDDER_NEW kịp trở thành winner trước cancel
     *
     * NOTE: refundWinner() dùng auction.getCurrentPrice() làm winningBid.
     *   Nếu worker chưa kịp placeBid() thì BIDDER_OLD vẫn là winner → cancel refund OLD.
     *   Test này verify rằng dù timing thế nào, BIDDER_NEW PHẢI được unlock.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testA_cancelWhileWorkerBetweenLockAndPlaceBid_bidderNewMoneyMustBeUnlocked() throws Exception {
        // ── Arrange ───────────────────────────────────────────────────────────
        System.out.println("[TEST A] Starting test: cancel while worker processing bid");
        auction = resetAuctionToBidderOld();
        System.out.println("[TEST A] Setup: auction reset, highest bidder = " + auction.getHighestBidderId() + ", price = " + auction.getCurrentPrice());
        
        CountDownLatch workerPausedLatch = new CountDownLatch(1);
        CountDownLatch workerResumeLatch = new CountDownLatch(1);
        CountDownLatch cancelDoneLatch   = new CountDownLatch(1);

        // Set up basic mocks
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(anyString(), anyLong())).thenReturn(true);
        when(bidTransactionDAO.saveBidTransaction(any())).thenReturn(true);

        // Override updatePendingBalance dengan doAnswer để thêm pause logic
        doAnswer(inv -> {
            String seller = inv.getArgument(0);
            long delta = inv.getArgument(1);
            String threadName = Thread.currentThread().getName();
            
            System.out.println("[WORKER] updatePendingBalance called: seller=" + seller + ", delta=" + delta + ", thread=" + threadName);
            
            // Chỉ chặn khi đúng luồng worker đang xử lý NEW_BID (với khoảng chênh lệch giá là NEW_BID - OLD_BID)
            if (SELLER_ID.equals(seller) && delta == (NEW_BID - OLD_BID) && 
                threadName.startsWith("bid-worker")) {
                
                System.out.println("[WORKER] PAUSING at updatePendingBalance");
                workerPausedLatch.countDown(); // Báo cho main thread biết đã tóm được worker
                try {
                    boolean resumed = workerResumeLatch.await(5, TimeUnit.SECONDS);
                    System.out.println("[WORKER] RESUMED: " + resumed);
                } catch (InterruptedException e) {
                    System.out.println("[WORKER] INTERRUPTED during pause");
                    Thread.currentThread().interrupt();
                }
            }
            return true;
        }).when(userDAO).updatePendingBalance(anyString(), anyLong());

        AtomicReference<Exception> cancelError = new AtomicReference<>();

        // ── Act ───────────────────────────────────────────────────────────────
        Thread submitThread = new Thread(() -> {
            try {
                System.out.println("[SUBMIT] Submitting NEW_BID = " + NEW_BID);
                // Submit NEW_BID vào hàng đợi với tham số lockAmount = NEW_BID
                ConcurrentBidManager.getInstance().submitBid(
                        auction, BIDDER_NEW, "bidder_new_username", NEW_BID, NEW_BID, BidType.MANUAL
                );
                System.out.println("[SUBMIT] Submit completed");
            } catch (Exception ignored) {
                System.out.println("[SUBMIT] Submit failed: " + ignored.getMessage());
            }
        });
        submitThread.start();

        // Chờ worker lấy task ra và bị mắc bẫy
        boolean paused = workerPausedLatch.await(5, TimeUnit.SECONDS);
        System.out.println("[MAIN] Worker paused: " + paused);
        assertTrue(paused, "Worker phải chạy và bị chặn tại updatePendingBalance trong 5 giây");
        
        // Luồng Admin: gọi cancelAuction() trong lúc Worker đang kẹt
        Thread cancelThread = new Thread(() -> {
            try {
                System.out.println("[ADMIN] Starting cancel...");
                adminService.cancelAuction(AUCTION_ID, REASON);
                System.out.println("[ADMIN] Cancel completed successfully");
            } catch (Exception e) {
                System.out.println("[ADMIN] Cancel failed: " + e.getMessage());
                cancelError.set(e);
            } finally {
                cancelDoneLatch.countDown();
            }
        }, "test-cancel-thread-A");
        cancelThread.start();

        // 1. Cho luồng Admin chạy một chút để kịp gọi refundWinner() và kẹt chờ ở shutdown().join()
        System.out.println("[MAIN] Waiting 100ms for admin to start...");
        Thread.sleep(100); 

        // 2. Thả Worker ra để nó hoàn tất (hoặc tự exit do bị interrupt)
        System.out.println("[MAIN] Resuming worker...");
        workerResumeLatch.countDown(); 

        // 3. Chờ Admin cancel hoàn tất
        boolean cancelDone = cancelDoneLatch.await(7, TimeUnit.SECONDS);
        System.out.println("[MAIN] Cancel done: " + cancelDone);
        assertTrue(cancelDone, "cancelAuction() phải hoàn thành trong 7 giây");
        
        System.out.println("[MAIN] Waiting for threads to finish...");
        cancelThread.join(3000);
        submitThread.join(3000);

        // ── Assert ────────────────────────────────────────────────────────────
        System.out.println("[ASSERT] Final status: " + auction.getStatus());
        assertNull(cancelError.get(), "cancelAuction() không được throw exception: " + cancelError.get());
        assertEquals(AuctionStatus.CANCELED, auction.getStatus(), "Auction phải đổi sang trạng thái CANCELED");

        // Admin sẽ hoàn tiền cho người thắng cũ là BIDDER_OLD (vì lúc Admin chạy, worker chưa kịp đổi highestBidder)
        verify(userDAO, atLeastOnce()).unlockBidderBalance(eq(BIDDER_OLD), eq(OLD_BID));

        // NẾU BÀI TEST BÁO ĐỎ (FAIL) Ở DÒNG DƯỚI ĐÂY -> CHÚC MỪNG BẠN ĐÃ BẮT ĐÚNG LỖI!
        // Worker bị ngắt giữa chừng, task của NEW_BID bị rớt khỏi hàng đợi nhưng lockAmount đã bị khóa trước đó.
        // Bắt buộc hệ thống của bạn (bên trong hàm shutdown hoặc processTask) phải có đoạn code "nhặt" 
        // các task bị kẹt lại để trả tiền cho BIDDER_NEW.
        System.out.println("[ASSERT] Verifying BIDDER_NEW is unlocked...");
        verify(userDAO, atLeastOnce()).unlockBidderBalance(eq(BIDDER_NEW), eq(NEW_BID));
        System.out.println("[TEST A] PASSED!");
    }

    // =========================================================================
    // Test B — cancel SAU khi worker đã placeBid() nhưng chưa xong saveBidTransaction
    // =========================================================================

    /**
     * KỊCH BẢN B:
     *
     *   Worker xử lý bid BIDDER_NEW (55_000 > 40_000):
     *     - unlockBidderBalance(BIDDER_OLD, OLD_BID) ✓
     *     - auctionEntity.placeBid() → BIDDER_NEW trở thành highestBidder ✓
     *     - bidTransactionDAO.saveBidTransaction()  ← CHẶN tại đây
     *
     *   Trong khi worker bị dừng tại save:
     *     - cancelAuction() chạy → status = CANCELED
     *     - refundWinner() thấy highestBidder = BIDDER_NEW → unlock BIDDER_NEW
     *
     * VERIFY:
     *   - BIDDER_NEW được unlock (refundWinner từ cancel, vì đã là highestBidder)
     *   - BIDDER_OLD được unlock đúng 1 lần (bởi processTask, không bị double-unlock)
     *   - Sau cancel, submitBid mới phải throw (closedAuctions guard)
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testB_cancelAfterWorkerPlacedBid_correctWinnerMustBeUnlocked() throws Exception {
        // ── Arrange ───────────────────────────────────────────────────────────
        System.out.println("[TEST B] Starting test: cancel after worker placed bid");
        auction = resetAuctionToBidderOld();
        System.out.println("[TEST B] Setup: auction reset, highest bidder = " + auction.getHighestBidderId());

        CountDownLatch workerPlacedBidLatch = new CountDownLatch(1);
        CountDownLatch workerResumeLatch    = new CountDownLatch(1);
        CountDownLatch cancelDoneLatch      = new CountDownLatch(1);

        // Setup basic mocks after resetAuctionToBidderOld
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(anyString(), anyLong())).thenReturn(true);

        // Chặn tại saveBidTransaction — tại thời điểm này placeBid() đã xong,
        // BIDDER_NEW đã là highestBidder trong auction object
        doAnswer(inv -> {
            System.out.println("[WORKER] saveBidTransaction called");
            workerPlacedBidLatch.countDown();
            boolean resumed = workerResumeLatch.await(5, TimeUnit.SECONDS);
            System.out.println("[WORKER] saveBidTransaction resumed: " + resumed);
            return null;
        }).when(bidTransactionDAO).saveBidTransaction(any(BidTransaction.class));

        AtomicReference<Exception> cancelError = new AtomicReference<>();

        // ── Act ───────────────────────────────────────────────────────────────

        Thread submitThread = new Thread(() -> {
            try {
                System.out.println("[SUBMIT] Submitting NEW_BID = " + NEW_BID);
                ConcurrentBidManager.getInstance().submitBid(
                    auction, BIDDER_NEW, "bidder_new_username",
                    NEW_BID, NEW_BID, BidType.MANUAL
                );
                Thread.sleep(300);
                System.out.println("[SUBMIT] Submit completed");
            } catch (Exception ignored) {
                System.out.println("[SUBMIT] Submit failed: " + ignored.getMessage());
            }
        }, "test-worker-thread-B");
        submitThread.start();

        // Chờ worker đã gọi placeBid và đang dừng trong saveBidTransaction
        assertTrue(workerPlacedBidLatch.await(5, TimeUnit.SECONDS),
            "Worker phải đã gọi saveBidTransaction trong 5 giây");
        System.out.println("[MAIN] Worker placed bid");

        // Lúc này BIDDER_NEW đã là highestBidder
        assertEquals(BIDDER_NEW, auction.getHighestBidderId(),
            "BIDDER_NEW phải là highestBidder sau placeBid()");
        System.out.println("[MAIN] Verified: BIDDER_NEW is highest bidder");

        Thread cancelThread = new Thread(() -> {
            try {
                System.out.println("[ADMIN] Starting cancel...");
                adminService.cancelAuction(AUCTION_ID, REASON);
                System.out.println("[ADMIN] Cancel completed");
            } catch (Exception e) {
                System.out.println("[ADMIN] Cancel failed: " + e.getMessage());
                cancelError.set(e);
            } finally {
                cancelDoneLatch.countDown();
            }
        }, "test-cancel-thread-B");
        cancelThread.start();

        Thread.sleep(100);

        // Resume worker trước khi chờ cancel (tránh deadlock shutdown().join())
        System.out.println("[MAIN] Resuming worker...");
        workerResumeLatch.countDown();

        assertTrue(cancelDoneLatch.await(7, TimeUnit.SECONDS),
            "cancelAuction() phải hoàn thành trong 7 giây");
        System.out.println("[MAIN] Cancel completed");

        cancelThread.join(3_000);
        submitThread.join(3_000);

        // ── Assert ────────────────────────────────────────────────────────────

        assertNull(cancelError.get(),
            "cancelAuction() không được throw: " + cancelError.get());

        assertEquals(AuctionStatus.CANCELED, auction.getStatus(),
            "Auction phải là CANCELED");
        System.out.println("[ASSERT] Status is CANCELED");

        // refundWinner() thấy highestBidder = BIDDER_NEW → unlock BIDDER_NEW với getCurrentPrice() = NEW_BID
        verify(userDAO, atLeastOnce()).unlockBidderBalance(eq(BIDDER_NEW), eq(NEW_BID));
        System.out.println("[ASSERT] BIDDER_NEW unlocked");

        // BIDDER_OLD đã được unlock bởi processTask (trước khi dừng) — đúng 1 lần
        verify(userDAO, times(1)).unlockBidderBalance(eq(BIDDER_OLD), eq(OLD_BID));
        System.out.println("[ASSERT] BIDDER_OLD unlocked once");

        // closedAuctions guard: submitBid mới phải throw
        assertThrows(Exception.class, () ->
            ConcurrentBidManager.getInstance().submitBid(
                auction, "any-bidder", "any_username",
                NEW_BID + 10_000L, NEW_BID + 10_000L, BidType.MANUAL
            ),
            "Sau shutdown, submitBid phải throw"
        );
        System.out.println("[ASSERT] submitBid after cancel throws exception");
        System.out.println("[TEST B] PASSED!");
    }

    // =========================================================================
    // Test C — Stress: nhiều bid + cancel đồng thời, verify balance invariant
    // =========================================================================

    /**
     * KỊCH BẢN C:
     *
     *   5 bidder submit đồng thời (giá tăng dần: 35000, 37000, 39000, 41000, 43000).
     *   Sau 50ms, admin cancel auction.
     *
     * INVARIANT CẦN ĐẢM BẢO:
     *   Mỗi bidder đã lock tiền TRƯỚC khi submitBid() (ở BidService thực tế).
     *   Trong test này ta track lock/unlock qua mock để đảm bảo không leak.
     *
     *   processTask() KHÔNG lock — nên ta simulate lock từ bên ngoài giống BidService:
     *   mỗi thread gọi userDAO.lockBidderBalance() trước submitBid().
     *
     *   Sau khi tất cả xong (cancel + drain queue):
     *     totalLocked == totalUnlocked
     *   (Winner cuối cũng được unlock bởi refundWinner() của cancelAuction)
     *
     * WHY invariant này quan trọng:
     *   Nếu có bất kỳ bidder nào không được unlock → tiền bị giam vĩnh viễn (balance leak).
     */
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testC_stressTest_cancelDuringActiveBidding_noBalanceLeak() throws Exception {
        System.out.println("[TEST C] Starting stress test: cancel during active bidding");
        int NUM_BIDDERS = 5;

        AtomicLong totalLocked   = new AtomicLong(0);
        AtomicLong totalUnlocked = new AtomicLong(0);

        // Basic setup first
        when(userDAO.lockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(anyString(), anyLong())).thenReturn(true);
        when(bidTransactionDAO.saveBidTransaction(any())).thenReturn(true);

        // Override with doAnswer to track totals
        doAnswer(inv -> {
            long amount = (long) inv.getArgument(1);
            totalLocked.addAndGet(amount);
            System.out.println("[LOCK] Locked " + amount + ", total = " + totalLocked.get());
            return true;
        }).when(userDAO).lockBidderBalance(anyString(), anyLong());

        doAnswer(inv -> {
            long amount = (long) inv.getArgument(1);
            totalUnlocked.addAndGet(amount);
            System.out.println("[UNLOCK] Unlocked " + amount + ", total = " + totalUnlocked.get());
            return true;
        }).when(userDAO).unlockBidderBalance(anyString(), anyLong());

        CountDownLatch allSubmitsDone = new CountDownLatch(NUM_BIDDERS);
        long baseAmount = START_PRICE + 5_000L; // 35_000

        for (int i = 0; i < NUM_BIDDERS; i++) {
            final String bidderId = "stress-bidder-" + i;
            final long   bidAmt   = baseAmount + (i * 2_000L); // 35000, 37000, 39000, 41000, 43000
            new Thread(() -> {
                try {
                    // Simulate BidService: lock trước khi submitBid
                    userDAO.lockBidderBalance(bidderId, bidAmt);
                    ConcurrentBidManager.getInstance().submitBid(
                        auction, bidderId, bidderId + "_name",
                        bidAmt, bidAmt, BidType.MANUAL
                    );
                } catch (Exception ignored) {
                    // Nếu submitBid throw (auction đã closed), tiền đã lock phải được unlock
                    // Trong BidService thực tế sẽ có rollback — ở đây ta unlock thủ công
                    // để invariant vẫn đúng khi submitBid bị chặn bởi closedAuctions guard.
                    try {
                        userDAO.unlockBidderBalance(bidderId, bidAmt);
                    } catch (Exception e2) {
                        // ignored
                    }
                } finally {
                    allSubmitsDone.countDown();
                }
            }, "stress-" + bidderId).start();
        }

        // Cancel sau 50ms để một số bid đã vào queue, một số chưa
        Thread.sleep(50);
        System.out.println("[MAIN] Total locked so far: " + totalLocked.get());
        try {
            System.out.println("[ADMIN] Canceling auction...");
            adminService.cancelAuction(AUCTION_ID, REASON);
            System.out.println("[ADMIN] Cancel completed");
        } catch (Exception ignored) {
            System.out.println("[ADMIN] Cancel error: " + ignored.getMessage());
        }

        allSubmitsDone.await(5, TimeUnit.SECONDS);
        System.out.println("[MAIN] All submits done");

        // Đợi worker drain hết queue (tối đa 1s)
        Thread.sleep(1_000);

        // INVARIANT: tổng tiền lock == tổng tiền unlock (không có balance leak)
        System.out.println("[ASSERT] Total locked: " + totalLocked.get() + ", Total unlocked: " + totalUnlocked.get());
        assertEquals(totalLocked.get(), totalUnlocked.get(),
            String.format(
                "BALANCE LEAK! locked=%d, unlocked=%d, chênh lệch=%d",
                totalLocked.get(), totalUnlocked.get(),
                totalLocked.get() - totalUnlocked.get()
            )
        );

        assertEquals(AuctionStatus.CANCELED, auction.getStatus(),
            "Auction phải là CANCELED sau cancelAuction()");
        System.out.println("[TEST C] PASSED!");
    }

    // =========================================================================
    // HELPER — reset auction về trạng thái BIDDER_OLD đã win qua ConcurrentBidManager
    // =========================================================================

    /**
     * Tạo lại auction từ đầu và submit bid của BIDDER_OLD qua ConcurrentBidManager.
     * Mục đích: populate previousWinnerBidtask[auctionId] với task của BIDDER_OLD,
     * để khi BIDDER_NEW được xử lý, processTask có thể đọc lockAmount của winner cũ
     * mà không NPE.
     *
     * WHY cần helper này:
     *   auction.placeBid() trực tiếp không populate previousWinnerBidtask.
     *   processTask() đọc map này để biết unlock bao nhiêu cho winner cũ.
     *   Nếu map rỗng khi BIDDER_NEW vào → NullPointerException tại dòng:
     *     BidTask previousWinner = previousWinnerBidtask.get(auctionId);
     *     long unlockAmount = previousWinner.lockAmount;  // NPE
     */
    private Auction resetAuctionToBidderOld() throws Exception {
        // Dọn registry và manager state
        System.out.println("[HELPER] Resetting auction to BIDDER_OLD state...");
        AuctionRegistry.getInstance().remove(AUCTION_ID);
        ConcurrentBidManager.resetInstance();
        ConcurrentBidManager.initialize(
            userDAO, bidTransactionDAO, autoBidService, auctionDAO, notificationController
        );

        AuctionConfig config = new AuctionConfig(
            AUCTION_ID, "Race Condition Test Auction",
            START_PRICE, 1_000L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        Auction freshAuction = new Auction(config, AuctionStatus.RUNNING, SELLER_ID, ITEM_ID);
        AuctionRegistry.getInstance().registerIfAbsent(freshAuction);
        System.out.println("[HELPER] Fresh auction created with status: " + freshAuction.getStatus());

        // Submit bid BIDDER_OLD và đợi worker xử lý xong
        // → previousWinnerBidtask[AUCTION_ID] = BidTask(BIDDER_OLD, OLD_BID, lockAmount=OLD_BID)
        CountDownLatch oldBidProcessed = new CountDownLatch(1);
        doAnswer(inv -> {
            String seller = inv.getArgument(0);
            long delta = inv.getArgument(1);
            System.out.println("[HELPER] updatePendingBalance called for OLD_BID setup: seller=" + seller + ", delta=" + delta);
            oldBidProcessed.countDown();
            return true;
        }).when(userDAO).updatePendingBalance(eq(SELLER_ID), anyLong());

        System.out.println("[HELPER] Submitting OLD_BID = " + OLD_BID);
        ConcurrentBidManager.getInstance().submitBid(
            freshAuction, BIDDER_OLD, "bidder_old_username",
            OLD_BID, OLD_BID, BidType.MANUAL
        );

        boolean processed = oldBidProcessed.await(5, TimeUnit.SECONDS);
        System.out.println("[HELPER] Setup bid processed: " + processed);
        assertTrue(processed,
            "Setup: bid BIDDER_OLD phải được xử lý trong 5 giây");

        // Reset mock sau khi setup xong để không ảnh hưởng verify trong test thực
        reset(userDAO);
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(anyString(), anyLong())).thenReturn(true);

        assertEquals(BIDDER_OLD, freshAuction.getHighestBidderId(),
            "Setup: BIDDER_OLD phải là highestBidder sau helper");
        assertEquals(OLD_BID, freshAuction.getCurrentPrice(),
            "Setup: currentPrice phải là OLD_BID sau helper");

        return freshAuction;
    }
}
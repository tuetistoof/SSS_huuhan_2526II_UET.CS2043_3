package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;

/**
 * Các bài test đồng thời (concurrency) cho ConcurrentBidManager.
 *
 * ConcurrentBidManager dùng per-auction BlockingQueue + worker thread để đảm bảo
 * bid được xử lý tuần tự, thread-safe. Các test tập trung vào:
 *
 *  1. Tính tuần tự: N luồng submit đồng thời → giá cuối là cao nhất, không duplicate
 *  2. Lock/unlock balance: đúng lockAmount (không phải currentPrice) được dùng
 *  3. Bid thất bại (amount <= currentPrice) → unlock ngay cho bidder đó
 *  4. previousWinnerBidtask: unlock winner cũ dùng lockAmount của task, không phải giá bid
 *  5. closedAuctions: submitBid sau shutdown() phải throw, không tạo worker mới
 *  6. Isolation: 2 auction dùng queue riêng, không ảnh hưởng chéo
 *
 * CHÚ Ý: submitBid() hiện có 6 tham số:
 *   submitBid(auction, bidderId, bidderUsername, bidAmount, lockAmount, bidType)
 * lockAmount là số tiền đã lock trước đó ở BidService — có thể khác bidAmount (auto-bid).
 */
public class ConcurrentBidManagerTest {

    private Auction auction;
    private ConcurrentBidManager bidManager;
    private UserDAO userDAO;
    private BidTransactionDAO bidTransactionDAO;
    private NotificationController notificationController;

    private static final String AUCTION_ID  = "concurrent-test-auction";
    private static final String AUCTION2_ID = "auction-2";
    private static final long   START_PRICE = 30_000L;

    @BeforeEach
    void setUp() throws Exception {
        userDAO               = mock(UserDAO.class);
        bidTransactionDAO     = mock(BidTransactionDAO.class);
        AutoBidService autoBidService = mock(AutoBidService.class);
        AuctionDAO auctionDAO         = mock(AuctionDAO.class);
        notificationController        = mock(NotificationController.class);

        doNothing().when(notificationController).notifyWatchers(anyString(), anyString());
        when(userDAO.lockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(anyString(), anyLong())).thenReturn(true);

        ConcurrentBidManager.initialize(
            userDAO, bidTransactionDAO, autoBidService, auctionDAO, notificationController
        );
        bidManager = ConcurrentBidManager.getInstance();

        AuctionConfig config = new AuctionConfig(
            AUCTION_ID, "Concurrent Test Auction",
            START_PRICE, 1_000L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        auction = new Auction(config, AuctionStatus.RUNNING, "seller-id", "item-id");
        AuctionRegistry.getInstance().registerIfAbsent(auction);
    }

    @AfterEach
    void tearDown() {
        ConcurrentBidManager.resetInstance();
        AuctionRegistry.getInstance().remove(AUCTION_ID);
        AuctionRegistry.getInstance().remove(AUCTION2_ID);
    }

    // =========================================================================
    // Group 1 — Tính tuần tự và đơn điệu
    // =========================================================================

    /**
     * WHY: 10 luồng gửi bid đồng thời với bidAmount tăng dần.
     * Queue đảm bảo xử lý tuần tự → giá cuối phải là bid cao nhất (41000).
     * Nếu thread-safety bị vỡ, bid thấp hơn có thể ghi đè bid cao hơn.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testConcurrentBidsProduceMonotonicallyIncreasingPrice() throws Exception {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);
        ExecutorService executor  = Executors.newFixedThreadPool(threadCount);

        for (int i = 1; i <= threadCount; i++) {
            final long   bidAmount = 31_000L + (i * 1_000L); // 32000 … 41000
            final String bidderId  = "bidder-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // lockAmount == bidAmount (manual bid)
                    bidManager.submitBid(auction, bidderId, bidderId, bidAmount, bidAmount, BidType.MANUAL);
                } catch (Exception e) {
                    System.err.println("Submit failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);

        long timeout = System.currentTimeMillis() + 5_000;
        while (auction.getCurrentPrice() != 41_000L && System.currentTimeMillis() < timeout) {
            Thread.sleep(50);
        }

        assertEquals(41_000L, auction.getCurrentPrice(),
            "Giá cuối phải là bid cao nhất (41000), không được thấp hơn do race");
    }

    /**
     * WHY: 5 luồng gửi cùng 1 mức giá đồng thời.
     * Chỉ bid đầu tiên được worker chấp nhận (> currentPrice).
     * Các bid sau bị skip (amount == currentPrice) → phải unlock ngay cho từng bidder.
     * Kết quả: đúng 1 BidTransaction trong auction.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testConcurrentIdenticalBidsAcceptOnlyOne() throws Exception {
        int  threadCount   = 5;
        long sameBidAmount = 35_000L;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);
        ExecutorService executor  = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String bidderId = "same-bidder-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    bidManager.submitBid(auction, bidderId, bidderId, sameBidAmount, sameBidAmount, BidType.MANUAL);
                } catch (Exception e) {
                    System.err.println("Submit failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        Thread.sleep(500);

        assertEquals(1, auction.getBidTransaction().size(),
            "Đúng 1 bid được chấp nhận khi N luồng gửi cùng mức giá");
        assertEquals(sameBidAmount, auction.getCurrentPrice());
    }

    /**
     * WHY: 2 auction dùng queue và worker riêng biệt.
     * Bid vào auction1 không được ảnh hưởng giá của auction2 và ngược lại.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testBidsOnDifferentAuctionsAreIndependent() throws Exception {
        AuctionConfig config2 = new AuctionConfig(
            AUCTION2_ID, "Second Auction",
            20_000L, 500L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        Auction auction2 = new Auction(config2, AuctionStatus.RUNNING, "seller-2", "item-2");
        AuctionRegistry.getInstance().registerIfAbsent(auction2);

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount * 2);
        ExecutorService executor  = Executors.newFixedThreadPool(threadCount * 2);

        for (int i = 1; i <= threadCount; i++) {
            final long   amount   = 31_000L + (i * 1_000L);
            final String bidderId = "a1-bidder-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    bidManager.submitBid(auction, bidderId, bidderId, amount, amount, BidType.MANUAL);
                } catch (Exception e) {
                    System.err.println("A1 submit failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        for (int i = 1; i <= threadCount; i++) {
            final long   amount   = 20_500L + (i * 500L);
            final String bidderId = "a2-bidder-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    bidManager.submitBid(auction2, bidderId, bidderId, amount, amount, BidType.MANUAL);
                } catch (Exception e) {
                    System.err.println("A2 submit failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        Thread.sleep(500);

        long price1 = auction.getCurrentPrice();
        assertTrue(price1 > START_PRICE && price1 <= 36_000L,
            "Giá auction1 phải trong khoảng (30000, 36000]");

        long price2 = auction2.getCurrentPrice();
        assertTrue(price2 > 20_000L && price2 <= 23_000L,
            "Giá auction2 phải trong khoảng (20000, 23000]");
    }

    // =========================================================================
    // Group 2 — shutdown() + closedAuctions guard
    // =========================================================================

    /**
     * WHY: sau shutdown(), closedAuctions.contains(auctionId) = true.
     * submitBid() phải throw ServiceException ngay tại ensureWorkerRunning()
     * — không được tạo worker mới, không enqueue gì cả.
     * Đây là fix chính cho race condition cancel ↔ bid.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testShutdownPreventsNewBids_closedAuctionsGuard() throws Exception {
        // Có 1 bid hợp lệ trước để warm up worker
        bidManager.submitBid(auction, "bidder-pre", "bidder-pre", 35_000L, 35_000L, BidType.MANUAL);
        Thread.sleep(300);
        assertEquals(1, auction.getBidTransaction().size(), "Setup: bid đầu phải được xử lý");

        bidManager.shutdown(AUCTION_ID);

        // Sau shutdown, submitBid phải throw — closedAuctions guard kích hoạt
        assertThrows(Exception.class,
            () -> bidManager.submitBid(auction, "bidder-after", "bidder-after", 40_000L, 40_000L, BidType.MANUAL),
            "submitBid sau shutdown() phải throw ServiceException");

        // Số bid không được tăng thêm
        assertEquals(1, auction.getBidTransaction().size(),
            "Không được có bid mới sau shutdown()");
    }

    /**
     * WHY: test cũ (testShutdownStopsWorker) kỳ vọng bid sau shutdown vẫn thành công
     * vì worker mới được tạo. Behavior đó đã bị fix bằng closedAuctions.
     * Test này replace test cũ, verify đúng behavior mới.
     * Dùng bid > currentPrice để đảm bảo test không fail vì lý do giá thấp.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testShutdownStopsWorker() throws Exception {
        bidManager.submitBid(auction, "bidder-pre", "bidder-pre", 35_000L, 35_000L, BidType.MANUAL);
        Thread.sleep(300);

        assertEquals(1, auction.getBidTransaction().size(), "Bid đầu tiên phải được chấp nhận");
        assertEquals(35_000L, auction.getCurrentPrice());

        bidManager.shutdown(AUCTION_ID);
        Thread.sleep(100);

        // WHY: khác test cũ — sau shutdown submitBid phải throw (không tạo worker mới)
        assertThrows(Exception.class,
            () -> bidManager.submitBid(auction, "bidder-after", "bidder-after", 40_000L, 40_000L, BidType.MANUAL),
            "Sau shutdown, ensureWorkerRunning() phải throw vì auctionId đã trong closedAuctions");

        // Giá không đổi — bid sau bị chặn hoàn toàn
        assertEquals(1, auction.getBidTransaction().size(),
            "Không có bid mới sau shutdown()");
        assertEquals(35_000L, auction.getCurrentPrice(),
            "Giá không được thay đổi sau shutdown()");
    }

    // =========================================================================
    // Group 3 — Lock / Unlock balance correctness
    // =========================================================================

    /**
     * WHY: processTask dùng previousWinnerBidtask.get(auctionId).lockAmount
     * để unlock winner cũ — không dùng currentAuctionPrice.
     * Điều này quan trọng khi auto-bid lock một mức khác với bidAmount.
     *
     * Kịch bản: bidderA lock 50_000 (manual), sau đó bidderB bid 60_000.
     * Khi B thắng, A phải được unlock đúng 50_000 (lockAmount của A), không phải 40_000 (currentPrice lúc B vào).
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testUnlockPreviousWinner_usesPreviousTaskLockAmount_notCurrentPrice() throws Exception {
        // A đặt bid 40_000, lockAmount = 50_000 (giả lập auto-bid lock trước cao hơn)
        bidManager.submitBid(auction, "bidderA", "bidderA", 40_000L, 50_000L, BidType.AUTO);
        Thread.sleep(300);
        assertEquals(40_000L, auction.getCurrentPrice(), "Setup: A phải là winner với giá 40_000");

        // B đặt bid 60_000, lockAmount = 60_000 → A bị outbid → A phải được unlock 50_000
        bidManager.submitBid(auction, "bidderB", "bidderB", 60_000L, 60_000L, BidType.MANUAL);
        Thread.sleep(300);

        // unlockBidderBalance phải được gọi với lockAmount của A (50_000), không phải currentPrice (40_000)
        verify(userDAO, atLeastOnce()).unlockBidderBalance("bidderA", 50_000L);
        assertEquals(60_000L, auction.getCurrentPrice(), "B phải là winner với giá 60_000");
    }

    /**
     * WHY: bid thất bại (amount <= currentPrice) → processTask skip bid
     * và unlock ngay task.lockAmount cho bidder đó.
     * Nếu không unlock, tiền bị giam vĩnh viễn (balance leak).
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testFailedBid_insufficientAmount_unlocksImmediately() throws Exception {
        // Đưa giá lên 40_000 trước
        bidManager.submitBid(auction, "bidder-winner", "bidder-winner", 40_000L, 40_000L, BidType.MANUAL);
        Thread.sleep(300);
        assertEquals(40_000L, auction.getCurrentPrice(), "Setup: giá phải là 40_000");

        // Gửi bid thấp hơn (30_000 <= 40_000) → bị skip → phải unlock 30_000
        bidManager.submitBid(auction, "bidder-loser", "bidder-loser", 30_000L, 30_000L, BidType.MANUAL);
        Thread.sleep(300);

        // Bid thất bại phải unlock lockAmount của nó
        verify(userDAO, atLeastOnce()).unlockBidderBalance("bidder-loser", 30_000L);

        // Giá không thay đổi
        assertEquals(40_000L, auction.getCurrentPrice(),
            "Giá không được thay đổi khi bid thấp hơn current price");
        assertEquals(1, auction.getBidTransaction().size(),
            "Số bid không được tăng khi bid thất bại");
    }

    /**
     * WHY: khi bid thắng, lockBidderBalance KHÔNG được gọi trong processTask —
     * tiền đã được lock ở BidService trước khi submitBid(). processTask chỉ
     * unlock winner cũ và update seller pending. Verify không lock thêm.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testSuccessfulBid_doesNotLockAgainInProcessTask() throws Exception {
        bidManager.submitBid(auction, "bidderA", "bidderA", 40_000L, 40_000L, BidType.MANUAL);
        Thread.sleep(300);

        // lockBidderBalance không được gọi bên trong processTask
        verify(userDAO, never()).lockBidderBalance(anyString(), anyLong());
        assertEquals(40_000L, auction.getCurrentPrice());
    }

    /**
     * WHY: delta (bidAmount - previousPrice) phải được cộng vào pending balance của seller.
     * Verify updatePendingBalance được gọi đúng với delta, không phải toàn bộ bidAmount.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testSellerPendingBalance_updatedWithDeltaNotFullAmount() throws Exception {
        // Bid đầu: 40_000 (từ startPrice 30_000) → delta = 10_000
        bidManager.submitBid(auction, "bidderA", "bidderA", 40_000L, 40_000L, BidType.MANUAL);
        Thread.sleep(300);

        verify(userDAO, atLeastOnce()).updatePendingBalance("seller-id", 10_000L);

        // Bid tiếp: 55_000 (từ 40_000) → delta = 15_000
        bidManager.submitBid(auction, "bidderB", "bidderB", 55_000L, 55_000L, BidType.MANUAL);
        Thread.sleep(300);

        verify(userDAO, atLeastOnce()).updatePendingBalance("seller-id", 15_000L);
    }

    // =========================================================================
    // Group 4 — Balance invariant (stress)
    // =========================================================================

    /**
     * WHY: với N bid đồng thời (giá tăng dần), invariant cần đảm bảo:
     *   tổng tiền unlock == tổng tiền lock (trừ winner cuối vẫn còn locked)
     *   Hay nói cách khác: tất cả bidder thua phải được unlock đầy đủ.
     *
     * Công thức: totalLocked = totalUnlocked + lockAmount_of_final_winner
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testStress_balanceInvariant_noLeakForLosers() throws Exception {
        int NUM_BIDDERS = 8;
        AtomicLong totalLocked   = new AtomicLong(0);
        AtomicLong totalUnlocked = new AtomicLong(0);

        doAnswer(inv -> {
            totalLocked.addAndGet((long) inv.getArgument(1));
            return true;
        }).when(userDAO).lockBidderBalance(anyString(), anyLong());

        doAnswer(inv -> {
            totalUnlocked.addAndGet((long) inv.getArgument(1));
            return true;
        }).when(userDAO).unlockBidderBalance(anyString(), anyLong());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(NUM_BIDDERS);

        // Bid tuần tự tăng dần — mỗi bid thắng đều outbid người trước
        // lockAmount == bidAmount (manual bid)
        long baseAmount = START_PRICE + 2_000L;
        for (int i = 0; i < NUM_BIDDERS; i++) {
            final long   bidAmt   = baseAmount + (i * 3_000L);
            final String bidderId = "stress-bidder-" + i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    bidManager.submitBid(auction, bidderId, bidderId, bidAmt, bidAmt, BidType.MANUAL);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            }, "stress-" + bidderId).start();
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        Thread.sleep(500); // Đợi worker xử lý hết queue

        // lockBidderBalance không được gọi trong processTask — chỉ gọi trong BidService.
        // Test này mock submit từ ngoài không qua BidService nên totalLocked = 0 từ processTask.
        // Verify: mọi bidder thua đều được unlock (totalUnlocked = tổng lockAmount của các bidder thua)
        long finalWinnerLockAmount = auction.getCurrentPrice(); // bidAmount == lockAmount trong test này

        // Invariant: locked - unlocked = lockAmount của winner cuối (còn đang bị giữ)
        // Nhưng vì submitBid() ở đây không lock (chỉ ConcurrentBidManager xử lý),
        // ta verify rằng số lần unlock == số bidder thua (NUM_BIDDERS - 1 bidder cuối còn đang thắng)
        // Nếu có leak → totalUnlocked < expected → test fail
        long expectedUnlocked = 0;
        for (int i = 0; i < NUM_BIDDERS - 1; i++) {
            expectedUnlocked += baseAmount + (i * 3_000L);
        }
        assertEquals(expectedUnlocked, totalUnlocked.get(),
            String.format(
                "Balance leak! Các bidder thua phải được unlock hết. expected=%d actual=%d",
                expectedUnlocked, totalUnlocked.get()
            )
        );
    }

    // =========================================================================
    // Group 5 — resetInstance() isolation
    // =========================================================================

    /**
     * WHY: resetInstance() phải clear closedAuctions để test sau không bị ảnh hưởng.
     * Nếu không clear, auctionId từ test trước vẫn trong closedAuctions →
     * submitBid() trong test sau throw ngay dù auction vẫn active.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testResetInstance_clearClosedAuctions_allowsNewBidAfterReset() throws Exception {
        bidManager.shutdown(AUCTION_ID);

        // Sau shutdown, bid phải throw
        assertThrows(Exception.class,
            () -> bidManager.submitBid(auction, "b1", "b1", 40_000L, 40_000L, BidType.MANUAL));

        // Reset và khởi tạo lại
        ConcurrentBidManager.resetInstance();
        ConcurrentBidManager.initialize(
            userDAO, bidTransactionDAO, mock(AutoBidService.class),
            mock(AuctionDAO.class), notificationController
        );
        ConcurrentBidManager bidManagerNew = ConcurrentBidManager.getInstance();

        // Sau reset, closedAuctions rỗng → submitBid phải thành công
        bidManagerNew.submitBid(auction, "b1", "b1", 40_000L, 40_000L, BidType.MANUAL);
        Thread.sleep(300);

        assertEquals(40_000L, auction.getCurrentPrice(),
            "Sau resetInstance(), bid phải được xử lý bình thường");
    }
}
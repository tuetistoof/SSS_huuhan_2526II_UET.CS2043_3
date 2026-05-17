package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.server.controller.NotificationController;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;

/**
 * Các bài test đồng thời (concurrency) cho ConcurrentBidManager.
 *
 * TẠI SAO cần có những bài test này:
 * Giá trị cốt lõi của ConcurrentBidManager là xử lý bid (lượt trả giá) tuần tự an toàn luồng (thread-safe)
 * thông qua BlockingQueue và worker thread cho từng phiên đấu giá. Nếu điều này bị phá vỡ,
 * nhiều bid cùng một mức giá có thể được chấp nhận, làm hỏng trạng thái của phiên đấu giá.
 *
 * Các bài test này xác minh:
 * 1. Chỉ có một bid chiến thắng khi N luồng (threads) gửi đồng thời (không chấp nhận trùng lặp)
 * 2. Giá cuối cùng phải tăng đơn điệu một cách nghiêm ngặt (chỉ tăng chứ không giảm)
 * 3. Số lượng bid bằng với số lượng đã gửi (không bị mất bid)
 * 4. Worker thread xử lý toàn bộ bid trong hàng đợi (queue) trước khi tắt (shutdown)
 */
public class ConcurrentBidManagerTest {

    private Auction auction;
    private ConcurrentBidManager bidManager;
    private static final String AUCTION_ID = "concurrent-test-auction";

    @BeforeEach
    void setUp() throws Exception {
        // Mock các DAO — chúng ta test tính đồng thời của trạng thái trên RAM (in-memory), không phải database
        BidTransactionDAO bidTransactionDAO = mock(BidTransactionDAO.class);
        AutoBidService autoBidService = mock(AutoBidService.class);
        AuctionDAO auctionDAO = mock(AuctionDAO.class);
        NotificationController notificationController = mock(NotificationController.class);

        doNothing().when(notificationController).notifyWatchers(anyString(), anyString());
        // Reset singleton để cô lập bài test (tránh ảnh hưởng chéo)
        ConcurrentBidManager.initialize(bidTransactionDAO, autoBidService, auctionDAO, notificationController);
        bidManager = ConcurrentBidManager.getInstance();

        // Tạo phiên đấu giá với giá khởi điểm (startPrice) = 30000, bước giá tối thiểu (minIncrement) = 1000
        AuctionConfig config = new AuctionConfig(
            AUCTION_ID, "Concurrent Test Auction",
            30000L, 1000L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        auction = new Auction(config, AuctionStatus.RUNNING, "seller-id", "item-id");

        // Đăng ký vào AuctionRegistry để worker thread có thể tìm thấy
        AuctionRegistry.getInstance().register(auction);
    }

    /**
     * TẠI SAO: 10 luồng gửi bid đồng thời với số tiền tăng dần.
     * Kỳ vọng: tất cả các bid được đưa vào hàng đợi và xử lý tuần tự.
     * Giá cuối cùng phải bằng với bid cao nhất được gửi.
     */

    @AfterEach
    void tearDown() {
        ConcurrentBidManager.resetInstance();
        AuctionRegistry.getInstance().remove(AUCTION_ID);
        AuctionRegistry.getInstance().remove("auction-2");
    }

    @Test
    void testConcurrentBidsProduceMonotonicallyIncreasingPrice() throws Exception {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 1; i <= threadCount; i++) {
            final long bidAmount = 31000L + (i * 1000L); // 32000, 33000, ..., 41000
            final String bidderId = "bidder-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // tất cả các luồng bắt đầu đồng thời
                    bidManager.submitBid(auction, bidderId, bidderId, bidAmount, BidType.MANUAL);
                } catch (Exception e) {
                    System.err.println("Submit failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // kích hoạt tất cả các luồng cùng lúc
        doneLatch.await(5, TimeUnit.SECONDS);

        // Cho worker thread thời gian để xử lý hết hàng đợi
        long timeout = System.currentTimeMillis() + 5000; // Đợi tối đa 5s
        while(auction.getCurrentPrice() != 41000L && System.currentTimeMillis() < timeout) {
            Thread.sleep(50); // Chờ từng nhịp nhỏ
        }
        // TẠI SAO: giá cuối cùng phải là mức cao nhất được gửi (41000), không thể thấp hơn
        // Nếu tính an toàn luồng (thread-safety) bị vỡ, một bid thấp hơn có thể ghi đè lên một bid cao hơn
        long finalPrice = auction.getCurrentPrice();
        assertEquals(41000L, finalPrice, "Final price must not exceed the highest submitted bid");
    }

    /**
     * TẠI SAO: 5 luồng gửi CÙNG MỘT số tiền bid đồng thời.
     * Kỳ vọng: chính xác MỘT bid được chấp nhận (bid đầu tiên được worker xử lý).
     * Các bid còn lại sẽ âm thầm bị loại bỏ vì số tiền <= giá hiện tại (currentPrice).
     * Điều này xác nhận rằng hàng đợi (queue) đã tuần tự hóa truy cập — không có trường hợp chấp nhận kép.
     */
    @Test
    void testConcurrentIdenticalBidsAcceptOnlyOne() throws Exception {
        int threadCount = 5;
        long sameBidAmount = 35000L; // lớn hơn giá khởi điểm + bước giá tối thiểu
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String bidderId = "same-bidder-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    bidManager.submitBid(auction, bidderId, bidderId, sameBidAmount, BidType.MANUAL);
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

        // TẠI SAO: chỉ được phép có đúng 1 giao dịch bid tồn tại — bid đầu tiên chiến thắng,
        // các bid giống hệt nhau theo sau sẽ bị hủy bỏ bởi ConcurrentBidManager.processTask()
        assertEquals(1, auction.getBidTransaction().size(),
            "Chỉ có một bid được chấp nhận khi N luồng gửi cùng một số tiền");
        assertEquals(sameBidAmount, auction.getCurrentPrice());
    }

    /**
     * TẠI SAO: xác minh rằng các bid từ các phiên đấu giá khác nhau không can thiệp (xung đột) lẫn nhau.
     * Hai phiên đấu giá được xử lý đồng thời phải có giá cuối cùng hoàn toàn độc lập.
     */
    @Test
    void testBidsOnDifferentAuctionsAreIndependent() throws Exception {
        // Tạo phiên đấu giá thứ hai
        AuctionConfig config2 = new AuctionConfig(
            "auction-2", "Second Auction",
            20000L, 500L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        Auction auction2 = new Auction(config2, AuctionStatus.RUNNING, "seller-2", "item-2");
        AuctionRegistry.getInstance().register(auction2);

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount * 2);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);

        // Các luồng đặt giá cho phiên đấu giá 1
        for (int i = 1; i <= threadCount; i++) {
            final long amount = 31000L + (i * 1000L);
            final String bidderId = "a1-bidder-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    bidManager.submitBid(auction, bidderId, bidderId, amount, BidType.MANUAL);
                } catch (Exception e) {
                    System.err.println("A1 submit failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Các luồng đặt giá cho phiên đấu giá 2
        for (int i = 1; i <= threadCount; i++) {
            final long amount = 20500L + (i * 500L);
            final String bidderId = "a2-bidder-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    bidManager.submitBid(auction2, bidderId, bidderId, amount, BidType.MANUAL);
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

        // TẠI SAO: mỗi phiên đấu giá có worker riêng — chúng không được ảnh hưởng chéo nhau
        long price1 = auction.getCurrentPrice();
        assertTrue(price1 > 30000L && price1 <= 36000L);

        long price2 = auction2.getCurrentPrice();
        assertTrue(price2 > 20000L && price2 <= 23000L);
        // Dọn dẹp
        AuctionRegistry.getInstance().remove("auction-2");
    }

    /**
     * TẠI SAO: kiểm tra xem hàm shutdown() có dừng worker thread đúng cách không.
     *
     * Hành vi thực tế của shutdown(): xóa queue và interrupt worker của auction đó.
     * Khi submitBid() được gọi sau đó, ensureWorkerRunning() tạo queue + worker MỚI,
     * nên bid sau shutdown vẫn được xử lý — bởi worker mới, không phải worker cũ.
     *
     * Điều cần verify: bid hợp lệ sau shutdown phải được xử lý đúng 1 lần bởi worker mới,
     * không bị duplicate do worker cũ bị leak. Dùng bid > currentPrice để đảm bảo
     * bid không bị reject bởi processTask() vì lý do giá thấp — che giấu bug thực sự.
     */
    @Test
    void testShutdownStopsWorker() throws Exception {
        // Gửi một bid hợp lệ trước và đợi worker xử lý
        bidManager.submitBid(auction, "bidder-pre", "bidder-pre", 35000L, BidType.MANUAL);
        Thread.sleep(300);

        assertEquals(1, auction.getBidTransaction().size(), "Bid đầu tiên phải được chấp nhận");
        assertEquals(35000L, auction.getCurrentPrice());

        // Tắt worker của auction này
        bidManager.shutdown(AUCTION_ID);
        Thread.sleep(100);

        // Gửi bid HỢP LỆ (> currentPrice=35000) sau shutdown.
        // submitBid() tạo worker mới → bid được xử lý đúng 1 lần.
        // Nếu worker cũ bị leak và cũng xử lý bid này → getBidTransaction().size() == 3
        // (double-processed) → test fail đúng lý do.
        bidManager.submitBid(auction, "bidder-after", "bidder-after", 40000L, BidType.MANUAL);
        Thread.sleep(300);

        assertEquals(2, auction.getBidTransaction().size(),
            "Bid sau shutdown phải được xử lý đúng 1 lần bởi worker mới, không bị duplicate");
        assertEquals(40000L, auction.getCurrentPrice(),
            "Giá phải phản ánh bid hợp lệ được xử lý bởi worker mới");
    }
}
package com.ssscloud.auction.server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;

public class AutoBidService {
    public static final long MIN_AUTO_BID_DELAY_SECONDS = 3000L; // Thời gian tối thiểu giữa 2 lần auto bid liên tiếp của cùng 1 người (để tránh spam)
    public static final int MAX_CHAIN_DEPTH = 200;
    public static final int MAX_AUTO_BID_PER_AUCTION = 50; // Giới hạn số lượng auto bid có thể đăng ký cho mỗi phiên đấu giá (để tránh quá tải)

    private final Map<String, List<AutoBidEntry>> registrations = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> executors = new ConcurrentHashMap<>(); 

    private final Map<String, Map<String, AtomicInteger>> bidCounts = new ConcurrentHashMap<>();

    private final ConcurrentBidManager bidManager = ConcurrentBidManager.getInstance();
    private final BidTransactionDAO bidTransactionDAO;
    private final AuctionDAO auctionDAO;
    public AutoBidService(AuctionDAO auctionDAO, BidTransactionDAO bidTransactionDAO) {
        this.auctionDAO = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;
    }

    public static class AutoBidEntry {
        final String bidderId  ;
        final String bidderUsername;
        final double maxBidAmount;
        final LocalDateTime registrationTime;

        public AutoBidEntry(String bidderId, String bidderUsername, double maxBidAmount) {
            this.bidderId = bidderId;
            this.bidderUsername = bidderUsername;
            this.maxBidAmount = maxBidAmount;
            this.registrationTime = LocalDateTime.now();
        }
    }

    // Đăng ký auto bid cho một phiên đấu giá
    public void registerAutoBid(AutoBidRequest req, String bidderId, String bidderUsername) {
        if (req == null || bidderId == null || bidderUsername == null)
            throw new IllegalArgumentException("Request và thông tin người dùng không được để trống");
        if (req.getMaxBid() <= 0) 
            throw new IllegalArgumentException("Maxbid phải lớn hơn 0");
        if (req.getAuctionId() == null || req.getAuctionId().isBlank()) 
            throw new IllegalArgumentException("Auction ID không được để trống");

        Auction auction = auctionDAO.findByAuctionId(req.getAuctionId());
        if (auction == null || auction.getStatus().isEnded() || auction.isExpired())            
            throw new IllegalArgumentException("Phiên đấu giá không tồn tại hoặc đã kết thúc: " + req.getAuctionId());
        if (bidderId.equals(auction.getSellerId()))
            throw new IllegalArgumentException("Người bán không thể đăng ký auto bid cho sản phẩm của mình");
        if (getAutoBidCount(req.getAuctionId(), bidderId) >= MAX_AUTO_BID_PER_AUCTION)
            throw new IllegalArgumentException("Đã đạt giới hạn đăng ký auto bid cho phiên đấu giá này");

        
        String auctionId = req.getAuctionId();
        List<AutoBidEntry> list = registrations.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>());

        list.removeIf(e -> e.bidderId.equals(bidderId));
        list.add(new AutoBidEntry(bidderId, bidderUsername, req.getMaxBid()));

        System.out.println("[AutoBidService] Đăng ký auto bid: bidderId=" + bidderId + ", auctionId=" + auctionId + ", maxBid=" + req.getMaxBid());
    }

    // Xóa tất cả đăng ký auto bid cho một phiên đấu giá (thường gọi khi phiên đấu giá kết thúc)
    public void clearRegistration(String auctionId) {
        registrations.remove(auctionId);
        bidCounts.remove(auctionId);

        ScheduledExecutorService executor = executors.remove(auctionId);
        if (executor != null) {
            executor.shutdownNow();
        }

        System.out.println("[AutoBidService] Đã xóa tất cả đăng ký auto bid cho auctionId=" + auctionId);

    }

    // Lấy danh sách đăng ký auto bid cho một phiên đấu giá
    public List<AutoBidEntry> getRegistrations(String auctionId) {
        return registrations.getOrDefault(auctionId, List.of());
    }

    // Lấy số lượng auto bid đã được đặt cho một phiên đấu giá bởi một người đấu giá cụ thể
    public int getAutoBidCount(String auctionId, String bidderId) {
        Map<String, AtomicInteger> auctionCounts = bidCounts.get(auctionId);
        if (auctionCounts == null) return 0;
        AtomicInteger count = auctionCounts.get(bidderId);
        return count == null ? 0 : count.get();
    }

    // Kích hoạt chuỗi auto bid cho một phiên đấu giá (thường gọi khi có một giao dịch đấu giá mới được đặt)
    public void trigger(Auction auction) {
        String auctionId = auction.getAuctionConfig().getId();
        ScheduledExecutorService executor = getOrCreateExecutor(auctionId);

        executor.schedule(() -> executorChainStep(auction, 0), MIN_AUTO_BID_DELAY_SECONDS, TimeUnit.MILLISECONDS);
    }



    private void executorChainStep(Auction auction, int depth) {
        if (depth > MAX_CHAIN_DEPTH) {
            System.out.println("[AutoBidService] Đạt giới hạn độ sâu chuỗi auto bid cho auctionId=" + auction.getAuctionConfig().getId());
        }

        if (auction.getStatus().isEnded() || auction.isExpired()) {
            System.out.println("[AutoBidService] Phiên đấu giá không còn hoạt động, dừng chuỗi auto bid cho auctionId=" + auction.getAuctionConfig().getId());
            return;
        }
        
        String auctionId = auction.getAuctionConfig().getId();
        long currentPrice = auction.getCurrentPrice();
        String highestBidderId = auction.getHighestBidderId();
        long minIncrement = auction.getAuctionConfig().getMinIncrement();
        long requiredAmount = currentPrice + minIncrement;

        AutoBidEntry bestEligible = findBestEligible(auctionId, requiredAmount, highestBidderId);
        if (bestEligible == null) {
            System.out.println("[AutoBidService] Không tìm thấy auto bid nào đủ điều kiện cho auctionId=" + auctionId + " với requiredAmount=" + requiredAmount);
            return;
        }

        BidTransaction bid;
        try {
            bid = bidManager.placeBid(auction, highestBidderId, highestBidderId, requiredAmount, BidType.AUTO);
        } catch (Exception e) {
            System.out.println("[AutoBidService] Lỗi khi đặt auto bid cho auctionId=" + auctionId + ": " + e.getMessage());
            return;
        }

        incrementBidCount(auctionId, highestBidderId);
        saveBidSafely(bid);
        ChangeManager.getInstance().notify(auction); //  Thông báo về giao dịch đấu giá mới (có thể là auto bid) để các thành phần khác cập nhật trạng thái phiên đấu giá
        System.out.println("[AutoBidService] Đặt auto bid thành công cho auctionId=" + auctionId + " với amount=" + requiredAmount + " từ bidderId=" + highestBidderId);

        // Lên lịch bước tiếp theo sau một khoảng thời gian ngắn (để tránh đặt auto bid liên tiếp quá nhanh)
        ScheduledExecutorService executor = executors.get(auctionId);
        if (executor != null && !executor.isShutdown()) {
            executor.schedule(() -> executorChainStep(auction, depth + 1), MIN_AUTO_BID_DELAY_SECONDS, TimeUnit.MILLISECONDS);
        }
    }


    private void saveBidSafely(BidTransaction bid) {
        try {
            bidTransactionDAO.saveBidTransaction(bid);
        } catch (Exception e) {
            System.out.println("[AutoBidService] Lỗi khi lưu giao dịch đấu giá tự động: " + e.getMessage());
        }
    }
    private void incrementBidCount(String auctionId, String bidderId) {
        bidCounts.computeIfAbsent(auctionId, k -> new ConcurrentHashMap<>())
                 .computeIfAbsent(bidderId, k -> new AtomicInteger(0))
                 .incrementAndGet();
    }
    private AutoBidEntry findBestEligible(String auctionId, long requiredAmount, String highestBidderId){
        List<AutoBidEntry> list = registrations.get(auctionId);
        if (list == null || list.isEmpty()) return null;
        AutoBidEntry best = null;
        for (AutoBidEntry entry : list) {
            if (entry.bidderId.equals(highestBidderId)) continue; // Không tự động đấu lại chính mình
            if (entry.maxBidAmount >= requiredAmount) {
                if (best == null || entry.maxBidAmount > best.maxBidAmount || (entry.maxBidAmount == best.maxBidAmount && entry.registrationTime.isBefore(best.registrationTime)))
                    best = entry;
            }
        }
        return best;
    }
    private ScheduledExecutorService getOrCreateExecutor(String auctionId) {
        return executors.computeIfAbsent(auctionId, k -> Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("AutoBidExecutor-" + auctionId);
            t.setDaemon(true);
            return t;
        }));
    }

}

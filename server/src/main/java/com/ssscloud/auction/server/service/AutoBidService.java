package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.AuctionDAO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoBidService {

    public static final int MAX_AUTO_BID_PER_AUCTION = 50;

    private final Map<String, List<AutoBidEntry>> registrationsByAuction = new ConcurrentHashMap<>();

    private final Map<String, Map<String, AtomicInteger>> bidCountByAuction = new ConcurrentHashMap<>();

    private final ConcurrentBidManager bidManager = ConcurrentBidManager.getInstance();;
    private final AuctionDAO auctionDAO;

    public AutoBidService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    public void register(AutoBidRequest req, String bidderId, String bidderUsername) {
        if (req == null || bidderId == null || bidderUsername == null)
            throw new IllegalArgumentException("Request và thông tin người dùng không được để trống");
        if (req.getAuctionId() == null || String.valueOf(req.getAuctionId()).isBlank())
            throw new IllegalArgumentException("Auction ID không được để trống");
        if (!BidValidator.isPositiveBid(req.getMaxBid()))
            throw new IllegalArgumentException("MaxBid phải lớn hơn 0");
        if (!BidValidator.isPositiveBid(req.getIncrement()))
            throw new IllegalArgumentException("Increment phải lớn hơn 0");
        if (req.getIncrement() > req.getMaxBid())
            throw new IllegalArgumentException("Increment không được lớn hơn MaxBid");

        Auction auction = AuctionRegistry.getInstance().get(req.getAuctionId());
        if (auction == null)
        {
            auction = auctionDAO.findByAuctionId(req.getAuctionId());
            if (auction == null)
                throw new InvalidBidException("Phiên đấu giá không tồn tại: " + req.getAuctionId());
            if (!auction.getStatus().isEnded() && !auction.isExpired())
                AuctionRegistry.getInstance().register(auction);
        }
        if (bidderId.equals(auction.getSellerId()))
            throw new IllegalArgumentException(
                    "Người bán không thể đăng ký auto bid cho sản phẩm của mình");
        if (getAutoBidCount(req.getAuctionId(), bidderId) >= MAX_AUTO_BID_PER_AUCTION)
            throw new IllegalArgumentException(
                    "Đã đạt giới hạn " + MAX_AUTO_BID_PER_AUCTION + " lần auto bid cho phiên này");

        List<AutoBidEntry> entries = registrationsByAuction.computeIfAbsent(
                req.getAuctionId(), k -> new CopyOnWriteArrayList<>());
        entries.removeIf(e -> e.bidderId.equals(bidderId));
        entries.add(new AutoBidEntry(bidderId, bidderUsername,
                (long) req.getMaxBid(), (long) req.getIncrement()));
    }

    public void trigger(Auction auction) {
        if (auction.getStatus().isEnded() || auction.isExpired()) {
            System.out.println("[AutoBidService] Phiên đã kết thúc, dừng chuỗi auctionId="
                    + auction.getAuctionConfig().getId());
            return;
        }

        String auctionId = auction.getAuctionConfig().getId();
        long currentPrice = auction.getCurrentPrice();
        String highestBidderId = auction.getHighestBidderId();
        removeExhaustedEntries(auctionId, currentPrice, highestBidderId);

        List<AutoBidEntry> entries = registrationsByAuction.get(auctionId);
        if (entries == null || entries.isEmpty())
            return;

        List<AutoBidEntry> eligibleEntries = new ArrayList<>();
        for (AutoBidEntry entry : entries) {
            if (!entry.bidderId.equals(highestBidderId))
                eligibleEntries.add(entry);
        }
        if (eligibleEntries.isEmpty())
            return;

        AutoBidEntry winner = eligibleEntries.get(0);
        for (AutoBidEntry entry : eligibleEntries) {
            if (entry.maxBid > winner.maxBid
                    || (entry.maxBid == winner.maxBid
                            && entry.registeredAt.isBefore(winner.registeredAt))) {
                winner = entry;
            }
        }

        long secondHighestMaxBid = 0;
        for (AutoBidEntry entry : eligibleEntries) {
            if (!entry.bidderId.equals(winner.bidderId)
                    && entry.maxBid > secondHighestMaxBid) {
                secondHighestMaxBid = entry.maxBid;
            }
        }

        long base = Math.max (secondHighestMaxBid, currentPrice);
        long bidAmount = Math.min(base + winner.increment, winner.maxBid);

        if (bidAmount <= currentPrice) {
            System.out.println("[AutoBidService] bidAmount=" + bidAmount
                    + " không vượt currentPrice=" + currentPrice
                    + ", bỏ qua auctionId=" + auctionId);
            return;
        }

        bidManager.submitBid(auction, winner.bidderId, winner.bidderUsername, bidAmount, BidType.AUTO);

        incrementBidCount(auctionId, winner.bidderId);
        System.out.println("[AutoBidService] Submit auto bid auctionId=" + auctionId
                + " winner=" + winner.bidderId
                + " bidAmount=" + bidAmount);
    }

    public void onBidSuccess(Auction auction, BidTransaction bid) {
        ChangeManager.getInstance().notify(auction);
    }

    public List<AutoBidEntry> getRegistrations(String auctionId) {
        return registrationsByAuction.getOrDefault(auctionId, List.of());
    }

    public int getAutoBidCount(String auctionId, String bidderId) {
        Map<String, AtomicInteger> auctionCounts = bidCountByAuction.get(auctionId);
        if (auctionCounts == null)
            return 0;
        AtomicInteger count = auctionCounts.get(bidderId);
        return count == null ? 0 : count.get();
    }

    public void clearRegistrations(String auctionId) {
        registrationsByAuction.remove(auctionId);
        bidCountByAuction.remove(auctionId);
        System.out.println("[AutoBidService] Đã xóa đăng ký auto bid cho auctionId=" + auctionId);
    }

    public List<AutoBidEntry> removeExhaustedEntries(String auctionId,
            long currentPrice,
            String highestBidderId) {
        List<AutoBidEntry> entries = registrationsByAuction.get(auctionId);
        if (entries == null || entries.isEmpty())
            return List.of();

        List<AutoBidEntry> exhaustedEntries = new ArrayList<>();
        entries.removeIf(entry -> {
            boolean isExhausted = !entry.bidderId.equals(highestBidderId)
                    && entry.maxBid < currentPrice + entry.increment;
            if (isExhausted)
                exhaustedEntries.add(entry);
            return isExhausted;
        });

        if (!exhaustedEntries.isEmpty()) {
            exhaustedEntries.forEach(e -> System.out.println(
                    "[AutoBidService] Xóa entry hết hạn mức: auctionId=" + auctionId
                            + " bidderId=" + e.bidderId
                            + " maxBid=" + e.maxBid
                            + " currentPrice=" + currentPrice));
        }

        return exhaustedEntries;
    }

    private void incrementBidCount(String auctionId, String bidderId) {
        bidCountByAuction.computeIfAbsent(auctionId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(bidderId, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public BidDTO toDTO(BidTransaction bid, long currentPrice) {
        BidDTO dto = new BidDTO();
        dto.setAuctionId(bid.getAuctionId());
        dto.setBidderUsername(bid.getBidderUsername());
        dto.setBidAmount(bid.getBidAmount());
        dto.setCurrentPrice(currentPrice);
        dto.setBidTime(bid.getBidTime());
        dto.setBidType(bid.getType().name());
        return dto;
    }

    public static class AutoBidEntry {
        public final String bidderId;
        public final String bidderUsername;
        public final long maxBid;
        public final long increment;
        public final LocalDateTime registeredAt;

        public AutoBidEntry(String bidderId, String bidderUsername, long maxBid, long increment) {
            this.bidderId = bidderId;
            this.bidderUsername = bidderUsername;
            this.maxBid = maxBid;
            this.increment = increment;
            this.registeredAt = LocalDateTime.now();
        }
    }
}

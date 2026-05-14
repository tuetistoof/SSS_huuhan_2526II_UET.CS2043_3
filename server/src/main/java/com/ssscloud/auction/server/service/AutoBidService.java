package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;
import com.ssscloud.auction.server.util.SessionRegistry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoBidService {

    private final Map<String, List<AutoBidEntry>> registrationsByAuction = new ConcurrentHashMap<>();

    private final Map<String, Map<String, AtomicInteger>> bidCountByAuction = new ConcurrentHashMap<>();

    private ConcurrentBidManager bidManager;
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final SessionRegistry sessionRegistry = SessionRegistry.getInstance();

    public AutoBidService(AuctionDAO auctionDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
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
        if (auction == null) {
            auction = auctionDAO.findByAuctionId(req.getAuctionId());
            if (auction == null)
                throw new InvalidBidException("Phiên đấu giá không tồn tại: " + req.getAuctionId());
            if (auction.getStatus().isEnded() || auction.isExpired())
                throw new InvalidBidException("Phiên đấu giá đã kết thúc");
            AuctionRegistry.getInstance().registerIfAbsent(auction);
            auction = AuctionRegistry.getInstance().get(req.getAuctionId());
        }
        long minIncrement = auction.getAuctionConfig().getMinIncrement();
        if (req.getIncrement() < minIncrement)
            throw new IllegalArgumentException ("increment phai lon hon minIncrement cua auction");
        if (bidderId.equals(auction.getSellerId()))
            throw new IllegalArgumentException(
                    "Người bán không thể đăng ký auto bid cho sản phẩm của mình");

        User bidder = userDAO.findById(bidderId);
        if (!(bidder instanceof Bidder b))
            throw new IllegalArgumentException("Người dùng không phải bidder");
        if (b.getAccountBalance() < req.getMaxBid())
            throw new InvalidBidException("Số dư tài khoản không đủ để đặt giá");
        List<AutoBidEntry> entries = registrationsByAuction.computeIfAbsent(
                req.getAuctionId(), k -> new CopyOnWriteArrayList<>());
        entries.removeIf(e -> e.bidderId.equals(bidderId));
        entries.add(new AutoBidEntry(bidderId, bidderUsername, (long) req.getMaxBid(), (long) req.getIncrement()));
        trigger(auction);
    }
    public void trigger(Auction auction) {
        if (auction.getStatus().isEnded() || auction.isExpired()) return;

        String auctionId = auction.getAuctionConfig().getId();
        long currentPrice = auction.getCurrentPrice();

        List<AutoBidEntry> allEntries = registrationsByAuction.get(auctionId);
        if (allEntries == null || allEntries.isEmpty()) return;
        System.out.println (allEntries.size());
        List<AutoBidEntry> snapshot = new ArrayList<>(allEntries);
        if (snapshot.isEmpty()) return;
        System.out.print(snapshot.size());
        List<AutoBidEntry> eligible = new ArrayList<>();
        for (AutoBidEntry e : snapshot) {
            eligible.add(e);
        }
        if (eligible.size() <= 1) return;

        AutoBidEntry winner = eligible.get(0);
        for (AutoBidEntry e : eligible) {
            if (e.maxBid > winner.maxBid
                    || (e.maxBid == winner.maxBid
                            && e.registeredAt.isBefore(winner.registeredAt))) {
                winner = e;
            }
        }
        
        long secondHighest = 0;
        for (AutoBidEntry e : eligible) {
            if (!e.bidderId.equals(winner.bidderId) && e.maxBid > secondHighest)
                secondHighest = e.maxBid;
        }
        System.out.println (secondHighest);
        long base = Math.max(secondHighest, currentPrice);
        long bidAmount = Math.min(base + winner.increment, winner.maxBid);
        System.out.println (secondHighest);
        if (bidAmount <= currentPrice) return;

        bidManager = ConcurrentBidManager.getInstance();
        bidManager.submitBid(auction, winner.bidderId, winner.bidderUsername, bidAmount, BidType.AUTO);
        incrementBidCount(auctionId, winner.bidderId);

        List<AutoBidEntry> toRemove = new ArrayList<>();
        for (AutoBidEntry entry : snapshot) {
            if (entry.bidderId.equals(winner.bidderId)) continue;
            toRemove.add(entry);
        }

        allEntries.removeAll(toRemove);
        toRemove.forEach(entry -> notifyAutoBidStopped(entry.bidderId));
    }

    private void notifyAutoBidStopped(String bidderId) {
        java.io.PrintWriter writer = sessionRegistry.getWriter(bidderId);
        if (writer != null) {
            synchronized (writer) {
                writer.println(JsonUtils.toJson(
                    ClientMessage.push("AUTO_BID_STOPPED",
                        java.util.Map.of("message",
                            "Auto Bid đã dừng — giá hiện tại vượt ngưỡng tối đa của bạn"))));
            }
        }
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

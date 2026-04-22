package com.ssscloud.auction.server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.BidTransactionDAO;

public class AutoBidService {
    public static final int MAX_CHAIN_DEPTH = 200;
    private final Map<String, List<AutoBidEntry>> registrations = new ConcurrentHashMap<>();
    private final ConcurrentBidManager bidManager = ConcurrentBidManager.getInstance();

    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();

    // public AutoBidService(BidTransactionDAO bidTransactionDAO) {
    //     this.bidTransactionDAO = bidTransactionDAO;
    // }

    public static class AutoBidEntry {
        final String bidderId;
        final String bidderUsername;
        final long maxBid;
        final LocalDateTime registeredAt;

        AutoBidEntry(String bidderId, String bidderUsername, long maxBid) {
            this.bidderId       = bidderId;
            this.bidderUsername = bidderUsername;
            this.maxBid         = maxBid;
            this.registeredAt = LocalDateTime.now();
        }
    }

    public void register(AutoBidRequest req, String bidderId, String bidderUsername){
        if (req.getAuctionId() == null)
            throw new IllegalArgumentException("Thiếu auctionId trong AutoBidRequest");
        if (BidValidator.isPositiveBid(req.getMaxBid()))
            throw new IllegalArgumentException("Maxbid bắt buộc phải lớn hơn 0");

        String auctionId = String.valueOf(req.getAuctionId());

        List<AutoBidEntry> list = registrations.computeIfAbsent(
            auctionId, k -> new CopyOnWriteArrayList<>()
        );

        list.removeIf(e -> e.bidderId.equals(bidderId));

        list.add(new AutoBidEntry(bidderId, bidderUsername, (long) req.getMaxBid()));

        System.out.println("[AutoBidService] Đăng ký: " + bidderUsername + " | phiên: "  + auctionId + " | max bid: " + (long) req.getMaxBid());
    }

    public void trigger(Auction auction){
        triggerChain(auction, 0);
    }

    private void triggerChain(Auction auction, int depth){
        if (depth >= MAX_CHAIN_DEPTH){
            System.out.println("[AutoBidService] Đã đến giới hạn " + MAX_CHAIN_DEPTH + " vòng, dừng.");
            return;
        }

        String auctionId = auction.getAuctionConfig().getId();
        List<AutoBidEntry> list = registrations.get(auctionId);

        if (list == null || list.isEmpty()) return;

        long currentPrice = auction.getCurrentPrice();
        long minIncrement = auction.getAuctionConfig().getMinIncrement();
        long requireAmount = currentPrice + minIncrement;
        String highestBidderId = auction.getHighestBidderId();

        AutoBidEntry best = null;
        for (AutoBidEntry entry : list){
            if (entry.bidderId.equals(highestBidderId) || entry.maxBid < requireAmount) continue;
            if (best == null)
                best = entry;
            else if (entry.maxBid > best.maxBid)
                best = entry;
            else if (entry.maxBid == best.maxBid && entry.registeredAt.isBefore(best.registeredAt))
                best = entry;
        }

        if (best == null) return;

        try {
            BidTransaction bid = bidManager.placeBid(auction, best.bidderId, best.bidderUsername, requireAmount, BidType.AUTO);
            bidTransactionDAO.saveBidTransaction(bid);
            System.out.println("[AutoBidService] Chain [" + depth + "] " + best.bidderUsername + " đặt " + requireAmount + " | phiên: " + auctionId);
            triggerChain(auction, depth + 1);   
        } catch (InvalidBidException e) {
            System.out.println("[AutoBidService] Chuỗi dừng: " + e.getMessage());
        }
    }

    public void clearRegistrantions(String auctionId){
        registrations.remove(auctionId);
        System.out.println("[AutoBidService] Đã xóa đăng ký phiên: " + auctionId);
    }

}

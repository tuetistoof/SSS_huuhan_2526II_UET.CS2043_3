package com.ssscloud.auction.server.service;
 
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
 
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.util.BidValidator;
 

public class ConcurrentBidManager {
    private static volatile ConcurrentBidManager instance = null;
 
    private ConcurrentBidManager() {}
 
    public static ConcurrentBidManager getInstance() {
        if (instance == null) {
            synchronized (ConcurrentBidManager.class) {
                if (instance == null) {
                    instance = new ConcurrentBidManager();
                }
            }
        }
        return instance;
    }
 
    // ── Lock map ──────────────────────────────────────────────────────────────
 
    private final Map<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();
 
    private ReentrantLock getLock(String auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    }
 
    public void removeLock(String auctionId) {
        ReentrantLock lock = auctionLocks.get(auctionId);
        if (lock == null) return;
 
        lock.lock();
        try {
            auctionLocks.remove(auctionId);
        } finally {
            lock.unlock();
        }
    }
 

    public BidTransaction placeBid(Auction auction, String bidderId,
                                   String bidderUsername, long amount, BidType type) {
        return placeBid(auction, bidderId, bidderUsername, amount, type, null);
    }
 
    public BidTransaction placeBid(Auction auction, String bidderId,
                                   String bidderUsername, long amount, BidType type,
                                   AntiSnipingService antiSnipingService) {
        String auctionId = auction.getAuctionConfig().getId();
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            // Validate status
            if (auction.getStatus().isEnded()) {
                throw new InvalidBidException("Phiên đấu giá đã kết thúc");
            }
 
            // Validate thời gian: auction có thể RUNNING nhưng đã quá endTime
            if (auction.isExpired()) {
                throw new InvalidBidException("Phiên đấu giá đã hết thời gian");
            }
 
            // Validate số tiền
            long minIncrement = auction.getAuctionConfig().getMinIncrement();
            if (!BidValidator.isValidBid(amount, auction.getCurrentPrice(), minIncrement)) {
                throw new InvalidBidException(
                    "Giá đặt phải ít nhất " + (auction.getCurrentPrice() + minIncrement) + " VND"
                );
            }
 
            // Tạo bid và cập nhật state auction
            BidTransaction bid = new BidTransaction(
                auctionId, bidderId, bidderUsername, amount, LocalDateTime.now(), type
            );
            auction.placeBid(bid);
 
            if (antiSnipingService != null) {
                antiSnipingService.processAntiSniping(auction.getAuctionConfig());
            }
 
            return bid;
 
        } finally {
            lock.unlock();
        }
    }
}
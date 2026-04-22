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

/**
 * class xử lý đa luồng
 * method quan trọng trong đây là placeBId
 * để xử lý đa luồng thay vì dùng synchronized cho toàn bộ placeBId
 * (nếu làm vậy các auction khác nhau phải chờ nhau) -> dùng Lock per Auction
 */
public class ConcurrentBidManager {
    //Singleton
    private static ConcurrentBidManager instance = null;
    private ConcurrentBidManager() {}
    public static ConcurrentBidManager getInstance(){
        if (instance == null){
            synchronized (ConcurrentBidManager.class) {
                if (instance == null){
                    instance = new ConcurrentBidManager();
                }
            }
        }
        return instance;
    }
    
    //tạo hashmap lưu mỗi auction có 1 ReentrantLock
    private final Map<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    private ReentrantLock getLock(String auctionID){
        return auctionLocks.computeIfAbsent(auctionID, k -> new ReentrantLock());
    }

    public void removeLock(String auctionID){
        auctionLocks.remove(auctionID);
    }

    public BidTransaction placeBid(Auction auction, String bidderId, String bidderUsername, long amount, BidType type){
        String auctionId = auction.getAuctionConfig().getId();
        ReentrantLock lock = getLock(auctionId);  //lấy lock của phiên đấy
        lock.lock();
        try {
            if (auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.CANCELED
                    || auction.getStatus() == AuctionStatus.PAID) {
                throw new InvalidBidException("Phiên đấu giá đã kết thúc");
            }
            if (auction.isExpired()) {
                throw new InvalidBidException("Phiên đấu giá đã hết thời gian");
            }
            long minIncrement = auction.getAuctionConfig().getMinIncrement();
            if (!BidValidator.isValidBid(amount, auction.getCurrentPrice(), minIncrement)) {
                throw new InvalidBidException(
                    "Giá đặt phải ít nhất " + (auction.getCurrentPrice() + minIncrement) + " VND"
                );
            }
            if (bidderId.equals(auction.getSellerId())) {
                throw new InvalidBidException("Người bán không thể tự đấu giá sản phẩm của mình");
            }


            BidTransaction bid = new BidTransaction(auctionId, bidderId, bidderUsername, amount, LocalDateTime.now(), type);
            auction.placeBid(bid);
            return bid;
                
        } finally {
            lock.unlock();
        }

    }
}

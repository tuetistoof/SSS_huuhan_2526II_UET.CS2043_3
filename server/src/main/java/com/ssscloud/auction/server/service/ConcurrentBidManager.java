package com.ssscloud.auction.server.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;

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
            instance = new ConcurrentBidManager();
        }
        return instance;
    }
    
    //tạo hashmap lưu mỗi auction có 1 ReentrantLock
    private final Map<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    //lấy lock của mỗi phiên, tạo mới nếu chưa có
    private ReentrantLock getLock(String auctionID){
        return auctionLocks.computeIfAbsent(auctionID, k -> new ReentrantLock());
    }

    //xóa lock khi phiên kết thúc cho gọn
    public void removeLock(String auctionID){
        auctionLocks.remove(auctionID);
    }

    public BidTransaction placeBid(Auction auction, String bidderId, String bidderUsername, long amount, BidType type){
        ReentrantLock lock = getLock(auction.getId());  //lấy lock của phiên đấy
        lock.lock();
        try {
            //validate: viết kiểm tra cơ bản ở đây

            BidTransaction bid = new BidTransaction(auction.getId(), bidderId, bidderUsername, amount, type);
            // viết thêm cả update thông tin cho auction đấy nữa
            return bid;
                
        } finally {
            lock.unlock();
        }

    }
}

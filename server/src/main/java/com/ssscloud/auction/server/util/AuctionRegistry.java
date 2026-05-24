package com.ssscloud.auction.server.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.server.dao.AuctionDAO;

public class AuctionRegistry {
    private static final Logger logger = Logger.getLogger(AuctionRegistry.class.getName()); 
    private static volatile AuctionRegistry instance;
    
    // Biến instance dùng cho các hàm non-static
    private final AuctionDAO auctionDAO;
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    private AuctionRegistry(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    public static AuctionRegistry getInstance(AuctionDAO auctionDAO) {
        if (instance == null) {
            synchronized (AuctionRegistry.class) {
                if (instance == null) {
                    instance = new AuctionRegistry(auctionDAO);
                }
            }
        }
        return instance;
    }

    public static AuctionRegistry initialize(AuctionDAO auctionDAO) throws Exception {
        try {
            synchronized (AuctionRegistry.class) {
                instance = new AuctionRegistry(auctionDAO);
                return instance;
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during AuctionRegistry initialization", exception);
            throw exception;
        }
    }

    // Nên kiểm tra kĩ NullPointerException khi dùng hàm này ở nơi khác
    public static AuctionRegistry getInstance() { 
        return instance; 
    }

    public void registerIfAbsent(Auction auction) {  
        if (auction != null && auction.getAuctionConfig() != null) {
            activeAuctions.putIfAbsent(auction.getAuctionConfig().getId(), auction);
        }
    }

    public Auction get(String auctionId) {
        return activeAuctions.get(auctionId);
    }

    // Alias dùng ở MessageHandler (SUBSCRIBE_AUCTION)
    public Auction getLiveAuction(String auctionId) {
        return activeAuctions.get(auctionId);
    }

    public void remove(String auctionId) {
        activeAuctions.remove(auctionId);
    }

    public boolean removeIfNoObservers(String auctionId, int currentObserverCount) {
        boolean[] removed = {false};
        activeAuctions.computeIfPresent(auctionId, (id, auction) -> {
            if (currentObserverCount == 0) {
                removed[0] = true;
                return null;    // returning null removes the entry from the map
            }
            return auction;     // keep entry
        });
        return removed[0];
    }

    // Dùng ở recoverLiveAuctions + startAuctionCloser
    public Collection<Auction> getAllLive() {
        return activeAuctions.values();
    }

    /**
     * ĐÃ SỬA: Chuyển thành hàm non-static để truy cập được auctionDAO và activeAuctions hợp lệ.
     */
    public Auction retrieveAndValidateAuction(String auctionId) throws Exception {
        try {
            // Bước 1: Kiểm tra trong bộ nhớ tạm (Registry / Cache) trước
            Auction auction = this.get(auctionId);
            
            if (auction == null || auction.getStatus().isEnded() || auction.isExpired()
                    || !auction.getStatus().isActive() || auction.getAuctionConfig().getEndTime().isBefore(LocalDateTime.now())) {
                
                // Bước 2: Nếu không thấy hoặc không hợp lệ, truy vấn từ cơ sở dữ liệu (DAO)
                // (Hết lỗi nhờ chuyển sang hàm non-static)
                auction = auctionDAO.findByAuctionId(auctionId);
                if (auction == null) {
                    throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "Auction not found");
                }
                
                if (auction.getStatus().isEnded() || auction.isExpired() || !auction.getStatus().isActive() 
                    || auction.getAuctionConfig().getEndTime().isBefore(LocalDateTime.now())) {
                    throw new ServiceException(ErrorCode.AUCTION_CLOSED, "Auction is closed");
                }
                this.registerIfAbsent(auction);
                auction = this.get(auctionId);
            }
            return auction;
        } catch (Exception exception) {
            logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] Unexpected system error in AuctionRegistry.retrieveAndValidateAuction for auctionId: "
                            + auctionId,
                    exception);
            throw exception;
        }
    }
}
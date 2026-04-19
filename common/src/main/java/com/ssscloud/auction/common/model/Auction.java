package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.observer.Subject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho một phiên đấu giá
 * subject cho observer pattern
 */
public class Auction implements Subject {
    AuctionConfig auctionConfig;
    private AuctionStatus status;
    private String sellerId;
    private String itemId;
    private long currentPrice;
    private String highestBidderId;
    private String highestBidderName;
    private int bidCount;
    private LocalDateTime bidTime;
    private BidType bidType;
    private List <BidTransaction> bidHistory;
    public Auction() {
    }
    public Auction (AuctionConfig auctionConfig, String sellerId, String itemId, long currentPrice, AuctionStatus status)
    {
        this.auctionConfig = auctionConfig;
        this.status = status;
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.currentPrice = currentPrice;
        this.highestBidderId = null;
        this.highestBidderName = null;
        this.bidCount = 0;
        this.bidTime = null;
        this.winnerId = null;
        this.bidHistory = new ArrayList<>();
    }
    // set state methods
    public void start() {
        if (this.status == AuctionStatus.OPEN) {
            this.status = AuctionStatus.RUNNING;
        }
    }

    public void finish() {
        if (this.status == AuctionStatus.OPEN || this.status == AuctionStatus.RUNNING) {
            this.status = AuctionStatus.FINISHED;
        }
    }

    public void markPaid() {
        if (this.status == AuctionStatus.FINISHED) {
            this.status = AuctionStatus.PAID;
        }
    }

    public void cancel() {
        if (this.status.isActive()) {
            this.status = AuctionStatus.CANCELED;
        }
    }

    public void placeBid(BidTransaction bid) {
        // setState
        this.currentPrice = bid.getBidAmount();
        this.highestBidderId = bid.getBidderId();
        this.bidHistory.add(bid);
        if (this.status == AuctionStatus.OPEN) {
            this.status = AuctionStatus.RUNNING;
        }

        // notify
        notifyObservers();
    }

    @Override
    public void notifyObservers() {
        // Truyền chính this vào — ChangeManager tìm HashMap[this] → list observer
        ChangeManager.getInstance().notify(this);
    }

    // helpers
    public boolean isActive() {
        return status.isActive();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(auctionConfig.getEndTime());
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id=" + auctionConfig.getId() +
                ", name='" + auctionConfig.getName() + '\'' +
                ", currentPrice=" + currentPrice +
                ", status=" + status +
                '}';
    }
    // getter setter
    public AuctionConfig getAuctionConfig() {
        return auctionConfig;
    }
    public String getSellerId() {
        return sellerId;
    }
    public String getItemId() {
        return itemId;
    }
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    public long getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(long currentPrice) {
        this.currentPrice = currentPrice;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }
    public String getHighestBidderName() {
        return highestBidderName;
    }
    public void setHighestBidderName(String highestBidderName) {
        this.highestBidderName = highestBidderName;
    }
    public int getBidCount() {
        return bidCount;
    }
    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }
    public LocalDateTime getBidTime() {
        return bidTime;
    }
    public BidType getBidType() {
        return bidType;
    }
    public void setBidType(BidType bidType) {
        this.bidType = bidType;
    }
    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
    public String getWinnerId() {
        return winnerId;
    }
    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }
    public void setBidHistory(List<BidTransaction> bidHistory) {
        this.bidHistory = bidHistory;
    }


}
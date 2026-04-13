package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Entity;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.observer.Subject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho một phiên đấu giá
 * subject cho observer pattern
 */
public class Auction extends Entity implements Subject {

    private String sellerId;
    private String itemId;
    private long startPrice;
    private long currentPrice;
    private long minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String highestBidderId;
    private LocalDateTime bidTime;
    private String winnerId;
    private final int extendTime = 36;
    private String description;
    private List <BidTransaction> bidHistory;
    public Auction(String sellerId, String itemId, long startPrice, long currentPrice, long minIncrement, LocalDateTime starTime, LocalDateTime endTime, AuctionStatus status, String description) {
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.minIncrement = minIncrement;
        this.startTime = starTime;
        this.endTime = endTime;
        this.status = status;
        this.highestBidderId = null;
        this.bidTime = null;
        this.winnerId = null;
        this.description = description;
        this.bidHistory = new ArrayList<>();
    }

    // Constructor mặc định
    public Auction() {
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
        return LocalDateTime.now().isAfter(endTime);
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id=" + super.getId() +
                ", name='" + super.getName() + '\'' +
                ", currentPrice=" + currentPrice +
                ", status=" + status +
                '}';
    }
    // getter setter
    // seller Id quyet dinh qua viec ai la nguoi truy cap
    public String getSellerId() {
        return sellerId;
    }

    // khi tao san pham va dang ban thi khong the sua chi co the xoa
    public String getItemId() {
        return itemId;
    }

    public long getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(long startPrice) {
        this.startPrice = startPrice;
    }

    public long getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(long currentPrice) {
        this.currentPrice = currentPrice;
    }

    public long getMinIncrement() {
        return minIncrement;
    }

    public void setMinIncrement(long minIncrement) {
        this.minIncrement = minIncrement;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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

    public LocalDateTime getBidTime() {
        return bidTime;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
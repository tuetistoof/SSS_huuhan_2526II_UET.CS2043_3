package com.ssscloud.auction.common.model;

import java.time.LocalDateTime;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.model.base.Entity;

public class Auction extends Entity {
    private String sellerId;
    private String itemId;
    private long startPrice;
    private long currentPrice;
    private long minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String highestBidderId;
    private String winnerId;
    private final int extendTime = 36;
    private String description;
    public Auction (String sellerId, String itemId, long startPrice, long currentPrice, long minIncrement, LocalDateTime starTime, LocalDateTime endTime, AuctionStatus status, String description){
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.minIncrement = minIncrement;
        this.startTime = starTime;
        this.endTime = endTime;
        this.status = status;
        this.highestBidderId = null;
        this.winnerId = null;
        this.description = description;
    }

    //getter setter
    // seller Id quyet dinh qua viec ai la nguoi truy cap
    public String getSellerId() {
        return sellerId;
    }

    //khi tao san pham va dang ban thi khong the sua chi co the xoa
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
    public String getWinnerId() {
        return winnerId;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}
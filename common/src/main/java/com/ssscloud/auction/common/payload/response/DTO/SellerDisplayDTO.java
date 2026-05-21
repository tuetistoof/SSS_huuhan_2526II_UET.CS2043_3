package com.ssscloud.auction.common.payload.response.DTO;

import java.time.LocalDateTime;

import com.ssscloud.auction.common.enums.AuctionStatus;

public class SellerDisplayDTO {
    private String id;
    private String auctionName;
    private String itemName;
    private Long startPrice;
    private Long currentPrice;
    private int bidCount;
    private LocalDateTime endTime;
    private AuctionStatus status;

    public SellerDisplayDTO(String id, String auctionName, String itemName, Long startPrice, Long currentPrice, int bidCount, LocalDateTime endTime, AuctionStatus status) {
        this.id = id;
        this.auctionName = auctionName;
        this.itemName = itemName;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.bidCount = bidCount;
        this.endTime = endTime;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuctionName() {
        return auctionName;
    }

    public void setAuctionName(String auctionName) {
        this.auctionName = auctionName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(Long startPrice) {
        this.startPrice = startPrice;
    }

    public Long getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Long currentPrice) {
        this.currentPrice = currentPrice;
    }

    public int getBidCount() {
        return bidCount;
    }

    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
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
}

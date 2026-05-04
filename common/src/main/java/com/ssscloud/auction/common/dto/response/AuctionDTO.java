package com.ssscloud.auction.common.dto.response;

import com.ssscloud.auction.common.enums.AuctionStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private long minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String description;
    
    private long currentPrice;
    private AuctionStatus status;
    private String sellerName;
    private String highestBidderName;
    private int bidCount;

    private ItemDTO item;

    public AuctionDTO() {
    }

    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

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

    
    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }
    public int getBidCount() {
        return bidCount;
    }

    
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public void setHighestBidderName(String highestBidderName) {
        this.highestBidderName = highestBidderName;
    }
    public String getHighestBidderName() {
        return highestBidderName;
    }

    
    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }
    public String getSellerName() {
        return sellerName;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
    public AuctionStatus getStatus() {
        return status;
    }


    public long getMinIncrement(){return minIncrement;}
    public void setMinIncrement(long minIncrement) {this.minIncrement = minIncrement;}


    public ItemDTO getItemData() {
        return item;
    }
    public void setItemData(ItemDTO item) {
        this.item = item;
    }

    
    @Override
    public String toString() {
        return "AuctionDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", status=" + status +
                '}';
    }
}
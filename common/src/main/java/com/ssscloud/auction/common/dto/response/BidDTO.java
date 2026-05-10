package com.ssscloud.auction.common.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String auctionId;
    private String bidderUsername;
    private String highestBidderId;
    private long bidAmount;
    private long currentPrice; 
    private LocalDateTime bidTime;
    private LocalDateTime newEndTime; // Thời gian kết thúc mới sau khi đặt giá (nếu có gia hạn)
    private String bidType; 

    public BidDTO() {}

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
 
    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }
 
    public String getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }
 
    public long getBidAmount() { return bidAmount; }
    public void setBidAmount(long bidAmount) { this.bidAmount = bidAmount; }
 
    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }
 
    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }
 
    public LocalDateTime getNewEndTime() { return newEndTime; }
    public void setNewEndTime(LocalDateTime newEndTime) { this.newEndTime = newEndTime; }
 
    public String getBidType() { return bidType; }
    public void setBidType(String bidType) { this.bidType = bidType; }
 
    @Override
    public String toString() {
        return "BidDTO{auctionId='" + auctionId
                + "', bidder='" + bidderUsername
                + "', highestBidderId='" + highestBidderId + '\''
                + "', amount=" + bidAmount
                + ", currentPrice=" + currentPrice
                + ", newEndTime=" + newEndTime
                + ", type=" + bidType
                + ", time=" + bidTime + '}';
    }
}

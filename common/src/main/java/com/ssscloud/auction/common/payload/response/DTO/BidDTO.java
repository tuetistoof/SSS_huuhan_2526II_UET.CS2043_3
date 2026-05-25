package com.ssscloud.auction.common.payload.response.DTO;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidDTO implements Serializable {
  private static final long serialVersionUID = 1L;

    private String auctionId;
    private String bidderId;
    private String bidderUsername;
    private long bidAmount;
    private long lockedBalance; 
    private LocalDateTime bidTime;
    private LocalDateTime antiSnipingEndTime; // Thời gian kết thúc mới sau khi đặt giá (nếu có gia hạn)
    private String bidType; 
    private long version;

  public BidDTO() {}

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    
    public String getBidderId() {
        return bidderId;
    }
    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }
 
 
    public long getBidAmount() { return bidAmount; }
    public void setBidAmount(long bidAmount) { this.bidAmount = bidAmount; }
 
    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }
 
 
    public String getBidType() { return bidType; }
    public void setBidType(String bidType) { this.bidType = bidType; }
 
    public long getLockedBalance() {
        return lockedBalance;
    }
    public void setLockedBalance(long lockedBalance) {
        this.lockedBalance = lockedBalance;
    }
    public LocalDateTime getAntiSnipingEndTime() {
        return antiSnipingEndTime;
    }
    public void setAntiSnipingEndTime(LocalDateTime antiSnipingEndTime) {
        this.antiSnipingEndTime = antiSnipingEndTime;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
    
    @Override
    public String toString() {
        return "BidDTO{auctionId='" + auctionId
                + "', bidder='" + bidderUsername
                + "', amount=" + bidAmount
                + ", type=" + bidType
                + ", time=" + bidTime + '}';
    }
}

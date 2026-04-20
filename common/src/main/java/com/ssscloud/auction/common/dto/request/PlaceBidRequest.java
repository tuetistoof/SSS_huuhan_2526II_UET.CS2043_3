package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;

public class PlaceBidRequest implements Serializable  {
    private static final long serialVersionUID = 1L;

    // Client gửi lên
    private String auctionId;
    private long bidAmount;
 
    // Server inject — không nhận từ client
    private String bidderId;
    private String bidderUsername;

    public PlaceBidRequest() {}

    public PlaceBidRequest(String auctionId, long bidAmount) {
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
    }
    //getter setter
    public String getAuctionId()  { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
 
    public long getBidAmount()    { return bidAmount; }
    public void setBidAmount(long bidAmount) { this.bidAmount = bidAmount; }
 
    public String getBidderId()   { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }
 
    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }
    @Override
    public String toString() {
        return "PlaceBidRequest{" +
                "auctionId=" + auctionId +
                ", bidAmount=" + bidAmount +
                '}';
    }
}

package com.ssscloud.auction.common.payload.request;

import java.io.Serializable;

public class PlaceBidRequest implements Serializable  {
    private static final long serialVersionUID = 1L;

    // Client gửi lên
    private String auctionId;
    private long bidAmount;
 

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
    @Override
    public String toString() {
        return "PlaceBidRequest{" +
                "auctionId=" + auctionId +
                ", bidAmount=" + bidAmount +
                '}';
    }
}

package com.ssscloud.auction.common.payload.request;

import java.io.Serializable;

public class AutoBidRequest implements Serializable{
    private static final long serialVersionUID = 1L;
    private String auctionId;
    private long maxBid;

    public AutoBidRequest() {}
    public AutoBidRequest(String auctionId, long maxBid){
        this.auctionId = auctionId;
        this.maxBid = maxBid;
    }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public long getMaxBid() { return maxBid; }
    public void setMaxBid(long maxBid) { this.maxBid = maxBid; }

    @Override
    public String toString() {
        return "AutoBidRequest{" +
                "auctionId=" + auctionId +
                ", maxBid=" + maxBid +
                '}';
    }
}


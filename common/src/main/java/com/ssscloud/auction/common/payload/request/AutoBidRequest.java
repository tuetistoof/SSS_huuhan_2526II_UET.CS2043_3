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

  public AutoBidRequest(String auctionId, long maxBid, long increment) {
    this.auctionId = auctionId;
    this.maxBid = maxBid;
    this.increment = increment;
  }

  public String getAuctionId() {
    return auctionId;
  }

    @Override
    public String toString() {
        return "AutoBidRequest{" +
                "auctionId=" + auctionId +
                ", maxBid=" + maxBid +
                '}';
    }
}

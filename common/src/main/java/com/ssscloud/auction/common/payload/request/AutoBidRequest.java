package com.ssscloud.auction.common.payload.request;

import java.io.Serializable;

public class AutoBidRequest implements Serializable {
  private static final long serialVersionUID = 1L;
  private String auctionId;
  private long maxBid;
  private long increment;

  public AutoBidRequest() {}

  public AutoBidRequest(String auctionId, long maxBid, long increment) {
    this.auctionId = auctionId;
    this.maxBid = maxBid;
    this.increment = increment;
  }

  public String getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(String auctionId) {
    this.auctionId = auctionId;
  }

  public long getMaxBid() {
    return maxBid;
  }

  public void setMaxBid(long maxBid) {
    this.maxBid = maxBid;
  }

  public long getIncrement() {
    return increment;
  }

  public void setIncrement(long increment) {
    this.increment = increment;
  }

  @Override
  public String toString() {
    return "AutoBidRequest{"
        + "auctionId="
        + auctionId
        + ", maxBid="
        + maxBid
        + ", increment="
        + increment
        + '}';
  }
}

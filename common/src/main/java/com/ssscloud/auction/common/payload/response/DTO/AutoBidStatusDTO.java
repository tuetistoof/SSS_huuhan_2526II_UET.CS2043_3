package com.ssscloud.auction.common.payload.response.DTO;

public class AutoBidStatusDTO {

  private boolean active;
  private long maxBid;
  private long increment;

  public AutoBidStatusDTO() {}

  public AutoBidStatusDTO(boolean active, long maxBid, long increment) {
    this.active = active;
    this.maxBid = maxBid;
    this.increment = increment;
  }

  public boolean isActive() {
    return active;
  }

  public long getMaxBid() {
    return maxBid;
  }

  public long getIncrement() {
    return increment;
  }
}

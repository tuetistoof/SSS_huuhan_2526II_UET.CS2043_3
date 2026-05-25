package com.ssscloud.auction.common.payload.response.DTO;

import com.ssscloud.auction.common.enums.AuctionStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public class AdminDisplayDTO implements Serializable {
  private String auctionId;
  private String auctionName;
  private String sellerName;
  private long currentPrice;
  private AuctionStatus status;
  private LocalDateTime endTime;

  public AdminDisplayDTO(
      String auctionId,
      String auctionName,
      String sellerName,
      long currentPrice,
      AuctionStatus status,
      LocalDateTime endTime) {
    this.auctionId = auctionId;
    this.auctionName = auctionName;
    this.sellerName = sellerName;
    this.currentPrice = currentPrice;
    this.status = status;
    this.endTime = endTime;
  }

  public String getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(String auctionId) {
    this.auctionId = auctionId;
  }

  public String getAuctionName() {
    return auctionName;
  }

  public void setAuctionName(String auctionName) {
    this.auctionName = auctionName;
  }

  public String getSellerName() {
    return sellerName;
  }

  public void setSellerName(String sellerName) {
    this.sellerName = sellerName;
  }

  public long getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(long currentPrice) {
    this.currentPrice = currentPrice;
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

  @Override
  public String toString() {
    return "AdminAuctionView{"
        + "auctionId='"
        + auctionId
        + ", auctionName='"
        + auctionName
        + ", sellerName='"
        + sellerName
        + ", currentPrice="
        + currentPrice
        + ", status="
        + status
        + ", endTime="
        + endTime
        + '}';
  }
}

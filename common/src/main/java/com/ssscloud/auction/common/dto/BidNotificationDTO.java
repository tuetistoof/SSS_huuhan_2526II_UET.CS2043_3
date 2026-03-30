package com.ssscloud.auction.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidNotificationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long auctionId;
    private double newPrice;
    private String highestBidder;
    private String message;
    private LocalDateTime timestamp;

    public BidNotificationDTO() {}

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public double getNewPrice() { return newPrice; }
    public void setNewPrice(double newPrice) { this.newPrice = newPrice; }

    public String getHighestBidder() { return highestBidder; }
    public void setHighestBidder(String highestBidder) { this.highestBidder = highestBidder; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "BidNotificationDTO{" +
                "auctionId=" + auctionId +
                ", newPrice=" + newPrice +
                ", highestBidder='" + highestBidder + '\'' +
                '}';
    }
}
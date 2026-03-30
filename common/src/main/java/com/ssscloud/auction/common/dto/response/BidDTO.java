package com.ssscloud.auction.common.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long auctionId;
    private String bidderUsername;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    @Override
    public String toString() {
        return "BidDTO{" +
                "id=" + id +
                ", bidderUsername='" + bidderUsername + '\'' +
                ", bidAmount=" + bidAmount +
                ", bidTime=" + bidTime +
                '}';
    }
}

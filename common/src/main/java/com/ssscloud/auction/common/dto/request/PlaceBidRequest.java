package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;

public class PlaceBidRequest implements Serializable  {
    private static final long serialVersionUID = 1L;

    private Long auctionId;
    private double bidAmount;

    public PlaceBidRequest() {}

    public PlaceBidRequest(Long auctionId, double bidAmount) {
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
    }
    //getter setter
    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    @Override
    public String toString() {
        return "PlaceBidRequest{" +
                "auctionId=" + auctionId +
                ", bidAmount=" + bidAmount +
                '}';
    }
}

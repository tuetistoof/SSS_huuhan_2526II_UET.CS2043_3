package com.ssscloud.auction.common.payload.response.request;

import java.io.Serializable;
import java.util.List;

import com.ssscloud.auction.common.payload.response.DTO.BidDTO;

public class BidHistoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long auctionId;
    private double currentPrice;
    private List<BidDTO> bids;

    public BidHistoryResponse() {}

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public List<BidDTO> getBids() { return bids; }
    public void setBids(List<BidDTO> bids) { this.bids = bids; }

    @Override
    public String toString() {
        return "BidHistoryResponse{" +
                "auctionId=" + auctionId +
                ", currentPrice=" + currentPrice +
                ", bidCount=" + (bids != null ? bids.size() : 0) +
                '}';
    }
}
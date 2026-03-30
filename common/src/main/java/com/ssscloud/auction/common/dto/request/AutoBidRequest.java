package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;

public class AutoBidRequest implements Serializable{
    //serialVersion dùng để ghi nhớ các phiên bản của Serializable
    private static final long serialVersionUID = 1L;

    private Long auctionId;
    private double maxBid;
    private double increment;

    public AutoBidRequest() {}

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public double getMaxBid() { return maxBid; }
    public void setMaxBid(double maxBid) { this.maxBid = maxBid; }

    public double getIncrement() { return increment; }
    public void setIncrement(double increment) { this.increment = increment; }
    
    @Override
    public String toString() {
        return "AutoBidRequest{" +
                "auctionId=" + auctionId +
                ", maxBid=" + maxBid +
                ", increment=" + increment +
                '}';
    }
}

//Cấu trúc 1 DTO: serialVersion, thuộc tính truyền đi, getter/setter, constructor mặc định và tham số
// toString để giúp debug

package com.ssscloud.auction.common.payload.request;

import java.io.Serializable;

public class GetAuctionDetailsRequest implements Serializable{
    private static final long serialVersionUID = 1L;
    private String auctionId;

    public GetAuctionDetailsRequest() {};

    public GetAuctionDetailsRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    @Override
    public String toString() {
        return "GetAuctionDetailRequest{" +
                ", auctionId=" + auctionId + '}';
    }
}

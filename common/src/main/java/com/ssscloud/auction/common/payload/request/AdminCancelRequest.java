package com.ssscloud.auction.common.payload.request;

import java.io.Serializable;

public class AdminCancelRequest implements Serializable{
    private String auctionId, reason;

    public AdminCancelRequest(String auctionId, String reason){
        this.auctionId = auctionId;
        this.reason = reason;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }
    public String getAuctionId() {
        return auctionId;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "AdminCancelRequest{" +
               "auctionId=" + auctionId +
               ", reason=" + reason +
               '}';
            
     }
}

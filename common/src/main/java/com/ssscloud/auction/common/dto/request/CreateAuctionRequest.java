package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CreateAuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private long startingPrice;
    private long minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public CreateAuctionRequest() {}

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public long getMinIncrement(){return minIncrement;}
    public void setMinIncrement(long minIncrement) {this.minIncrement = minIncrement;}

    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    @Override
    public String toString() {
        return "CreateAuctionRequest{" +
                ", startingPrice=" + startingPrice +
                ", minIncrement=" + minIncrement +
                ", endTime=" + endTime +
                '}';
    }

}

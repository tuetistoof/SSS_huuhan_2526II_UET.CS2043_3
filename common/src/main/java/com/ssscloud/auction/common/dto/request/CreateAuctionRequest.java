package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CreateAuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private long startingPrice;
    private long minIncrement;
    LocalDateTime startTime;
    private LocalDateTime endTime;

    public CreateAuctionRequest() {}


    public String getTitle() { return name; }
    public void setTitle(String title) { this.name = title; }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public long getMinIncrement(){return minIncrement;}
    public void setMinIncrement(long minIncrement) {this.minIncrement = minIncrement;}

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    @Override
    public String toString() {
        return "CreateAuctionRequest{" +
                ", name='" + name + '\'' +
                ", startingPrice=" + startingPrice +
                ", minIncrement=" + minIncrement +
                ", endTime=" + endTime +
                '}';
    }

}

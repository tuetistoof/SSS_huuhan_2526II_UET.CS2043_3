package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CreateAuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sellerId;
    private String title;
    private String description;
    private long startingPrice;
    private long minIncrement;
    private LocalDateTime endTime;

    public CreateAuctionRequest() {}

    public String getSellerId() {return sellerId;}
    public void setSellerId(String sellerId){this.sellerId = sellerId;}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public long getMinIncrement(){return minIncrement;}
    public void setMinIncrement(long minIncrement) {this.minIncrement = minIncrement;}

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    @Override
    public String toString() {
        return "CreateAuctionRequest{" +
                "sellerId='" + sellerId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", startingPrice=" + startingPrice +
                ", minIncrement=" + minIncrement +
                ", endTime=" + endTime +
                '}';
    }

}

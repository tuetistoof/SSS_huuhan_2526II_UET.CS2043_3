package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CreateAuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String description;
    private double startingPrice;
    private LocalDateTime endTime;

    public CreateAuctionRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    @Override
    public String toString() {
        return "CreateAuctionRequest{" +
                "title='" + title + '\'' +
                ", startingPrice=" + startingPrice +
                ", endTime=" + endTime +
                '}';
    }
    
}

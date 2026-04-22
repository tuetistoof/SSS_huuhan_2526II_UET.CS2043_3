package com.ssscloud.auction.common.dto.response;

import com.ssscloud.auction.common.enums.AuctionStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private long startingPrice;
    private long minIncrement;
    private LocalDateTime endTime;
    private String description;
    private long currentPrice;
    
    private AuctionStatus status;
    private String sellerName;
    private String highestBidderName;
    private int bidCount;

    public AuctionDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    public void setDescription(String description) {
        this.description = description;
    }


    public long getMinIncrement(){return minIncrement;}
    public void setMinIncrement(long minIncrement) {this.minIncrement = minIncrement;}

    @Override
    public String toString() {
        return "AuctionDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", status=" + status +
                '}';
    }
}

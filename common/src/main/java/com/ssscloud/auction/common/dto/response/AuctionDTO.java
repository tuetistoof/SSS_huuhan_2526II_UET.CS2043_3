package com.ssscloud.auction.common.dto.response;

import com.ssscloud.auction.common.enums.AuctionStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private long startingPrice;
    private long currentPrice;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String sellerName;
    private String highestBidderName;
<<<<<<< HEAD
    private int bidCount;
=======
>>>>>>> f2782340f8d12c50bfd7c1d1cf145f3045fda1fe
    private long minIncrement;

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

<<<<<<< HEAD
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
=======
    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }
>>>>>>> f2782340f8d12c50bfd7c1d1cf145f3045fda1fe

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

<<<<<<< HEAD
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getHighestBidderName() {
        return highestBidderName;
    }

    public void setHighestBidderName(String highestBidderName) {
        this.highestBidderName = highestBidderName;
    }

    public int getBidCount() {
        return bidCount;
    }

    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }

    public double getMinIncrement() {
        return minIncrement;
    }

    public void setMinIncrement(long minIncrement) {
        this.minIncrement = minIncrement;
    }
=======
    public long getMinIncrement(){return minIncrement;}
    public void setMinIncrement(long minIncrement) {this.minIncrement = minIncrement;}
>>>>>>> f2782340f8d12c50bfd7c1d1cf145f3045fda1fe

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

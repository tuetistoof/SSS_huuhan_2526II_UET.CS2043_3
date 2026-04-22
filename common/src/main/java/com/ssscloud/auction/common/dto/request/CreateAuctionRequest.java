package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CreateAuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

<<<<<<< HEAD
    private String name;
    private LocalDateTime startTime;
=======
    private String sellerId;
    private String title;
    private String description;
    private long startingPrice;
    private long minIncrement;
>>>>>>> 43e159cc1a0ae3c14ceebb8c40d741af668c30c8
    private LocalDateTime endTime;
    private String description;

    private String sellerId;
    private String sellerName;
    private String ItemId;
    public CreateAuctionRequest() {}

<<<<<<< HEAD
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
=======
    public String getSellerId() {return sellerId;}
    public void setSellerId(String sellerId){this.sellerId = sellerId;}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
>>>>>>> 43e159cc1a0ae3c14ceebb8c40d741af668c30c8

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

<<<<<<< HEAD
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
=======
    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }

    public long getMinIncrement(){return minIncrement;}
    public void setMinIncrement(long minIncrement) {this.minIncrement = minIncrement;}
>>>>>>> 43e159cc1a0ae3c14ceebb8c40d741af668c30c8

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getSellerId() {
        return sellerId;
    }
    public void setItemId(String itemId) {
        ItemId = itemId;
    }
    public String getSellerName() {
        return sellerName;
    }
    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }
    public String getItemId() {
        return ItemId;
    }
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    @Override
    public String toString() {
        return "CreateAuctionRequest{" +
<<<<<<< HEAD
                "name='" + name + '\'' +
                ", startTime=" + startTime +
=======
                "sellerId='" + sellerId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", startingPrice=" + startingPrice +
                ", minIncrement=" + minIncrement +
>>>>>>> 43e159cc1a0ae3c14ceebb8c40d741af668c30c8
                ", endTime=" + endTime +
                '}';
    }

}

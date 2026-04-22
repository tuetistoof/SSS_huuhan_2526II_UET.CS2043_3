package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CreateAuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private long startPrice;
    private long minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ItemData itemData;



    public CreateAuctionRequest() {}


    public String getName() { return name; }
    public void setName(String title) { this.name = title; }

    public long getStartPrice() { return startPrice; }
    public void setStartPrice(long startPrice) { this.startPrice = startPrice; }

    public long getMinIncrement(){return minIncrement;}
    public void setMinIncrement(long minIncrement) {this.minIncrement = minIncrement;}

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public ItemData      getItemData()                     { return itemData; }
    public void          setItemData(ItemData d)           { this.itemData = d; }

    @Override
    public String toString() {
        return "CreateAuctionRequest{" +
                ", name='" + name + '\'' +
                ", startPrice=" + startPrice +
                ", minIncrement=" + minIncrement +
                ", startTime=" + startTime +
                ", endTime=" + endTime 
                + ", itemData=" + itemData + '}';
    }

}

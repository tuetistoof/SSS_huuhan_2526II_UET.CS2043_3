package com.ssscloud.auction.common.dto.response;

import com.ssscloud.auction.common.enums.AuctionStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private long startPrice;
    private long minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    private UserDTO userDTO;
    private ItemDTO itemDTO;

    private String sellerName;
    private long currentPrice;
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

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public long getStartPrice() {
        return startPrice;
    }
    public void setStartPrice(long startPrice) {
        this.startPrice = startPrice;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
    public AuctionStatus getStatus() {
        return status;
    }

    public ItemDTO getItemDTO() {
        return itemDTO;
    }
    public void setItemDTO(ItemDTO itemDTO) {
        this.itemDTO = itemDTO;
    }
    public UserDTO getUserDTO() {
        return userDTO;
    }
    public void setUserDTO(UserDTO userDTO) {
        this.userDTO = userDTO;
    }


    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }
    public int getBidCount() {
        return bidCount;
    }   

    public void setHighestBidderName(String highestBidderName) {
        this.highestBidderName = highestBidderName;
    }
    public String getHighestBidderName() {
        return highestBidderName;
    }

    

    public long getMinIncrement(){return minIncrement;}
    public void setMinIncrement(long minIncrement) {this.minIncrement = minIncrement;}

    @Override
    public String toString() {
        return "AuctionDTO{" +
            "id='"                + id                + '\'' +
            ", name='"            + name              + '\'' +
            ", currentPrice="     + currentPrice      +
            ", minIncrement="     + minIncrement      +
            ", status="           + status            +
            ", sellerName='"      + sellerName        + '\'' +
            ", highestBidder='"   + highestBidderName + '\'' +
            ", bidCount="         + bidCount          +
            ", startTime="        + startTime         +
            ", endTime="          + endTime           +
            '}';
    }
}
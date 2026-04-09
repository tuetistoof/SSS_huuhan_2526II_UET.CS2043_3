package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho một phiên đấu giá
 */
public class Auction extends Entity {

    private String title;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private LocalDateTime endTime;
    private AuctionStatus status = AuctionStatus.OPEN;
    private String sellerId;
    private String highestBidderId;
    private Item item;                          // Sản phẩm được đấu giá
    private List<BidTransaction> bidHistory = new ArrayList<>();

    // Constructor mặc định
    public Auction() {
    }

    // Constructor đầy đủ (khuyến khích)
    public Auction(String title, String description, double startingPrice, 
                   LocalDateTime endTime, String sellerId, Item item) {
        this.title = title;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.endTime = endTime;
        this.sellerId = sellerId;
        this.item = item;
    }

    // ==================== GETTER & SETTER ====================

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public void setBidHistory(List<BidTransaction> bidHistory) { this.bidHistory = bidHistory; }

    // Method hỗ trợ
    public boolean isActive() {
        return status.isActive();
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id=" + getId() +
                ", title='" + title + '\'' +
                ", currentPrice=" + currentPrice +
                ", status=" + status +
                '}';
    }
}
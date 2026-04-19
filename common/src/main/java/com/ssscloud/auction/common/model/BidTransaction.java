package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.enums.BidType;
import java.time.LocalDateTime;

public class BidTransaction{
    private String auctionId;
    private long bidAmount;
    private String bidderId;
    private String bidderUsername;
    private LocalDateTime bidTime;
    private BidType type; // MANUAL hoặc AUTO

    // Constructor đầy đủ — dùng khi tạo bid mới
    public BidTransaction(String auctionId, String bidderId, String bidderUsername, long bidAmount, int bidCount, LocalDateTime bidTime, BidType type) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.type = type;
    }

    // No-arg constructor cho Gson
    public BidTransaction() {
    }

    // Getter

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public long getBidAmount() {
        return bidAmount;
    }
    public LocalDateTime getBidTime() {
        return bidTime;
    }

    p
    ublic BidType getType() {
        return type;
    }

    // Setters cần cho DAO khi đọc từ DB
    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public void setBidderUsername(String u) {
        this.bidderUsername = u;
    }

    public void setBidAmount(long bidAmount) {
        this.bidAmount = bidAmount;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public void setType(BidType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "BidTransaction{bidder='" + bidderUsername
                + "', amount=" + bidAmount + ", time=" + bidTime + '}';
    }
}

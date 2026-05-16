package com.ssscloud.auction.common.dto.response;

import java.time.LocalDateTime;

/**
 * DTO chung cho cả push realtime lẫn load pending khi login.
 * Client dùng cùng 1 class để render notification item.
 */
public class NotificationDTO {

    private String        id;
    private String        type;        // OUTBID | ENDED
    private String        auctionId;
    private String        auctionName;
    private long          price;       // currentPrice (OUTBID) / finalPrice (ENDED)
    private String        winner;      // null nếu type = OUTBID
    private boolean       read;
    private String        userId;     // chỉ dùng phía server khi save
    private LocalDateTime createdAt;

    public NotificationDTO() {}

    public NotificationDTO(String id, String type, String auctionId, String auctionName,
                           long price, String winner, boolean read, LocalDateTime createdAt) {
        this.id          = id;
        this.type        = type;
        this.auctionId   = auctionId;
        this.auctionName = auctionName;
        this.price       = price;
        this.winner      = winner;
        this.read        = read;
        this.createdAt   = createdAt;
    }

    public String        getId()          { return id; }
    public String        getType()        { return type; }
    public String        getAuctionId()   { return auctionId; }
    public String        getAuctionName() { return auctionName; }
    public long          getPrice()       { return price; }
    public String        getWinner()      { return winner; }
    public boolean       isRead()         { return read; }
    public LocalDateTime getCreatedAt()   { return createdAt; }

    public String getUserId()               { return userId; }
    public void setUserId(String userId)    { this.userId = userId; }

    public void setId(String id)                   { this.id = id; }
    public void setType(String type)               { this.type = type; }
    public void setAuctionId(String auctionId)     { this.auctionId = auctionId; }
    public void setAuctionName(String name)        { this.auctionName = name; }
    public void setPrice(long price)               { this.price = price; }
    public void setWinner(String winner)           { this.winner = winner; }
    public void setRead(boolean read)              { this.read = read; }
    public void setCreatedAt(LocalDateTime t)      { this.createdAt = t; }
}
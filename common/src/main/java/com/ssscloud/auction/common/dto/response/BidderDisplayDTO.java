package com.ssscloud.auction.common.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class BidderDisplayDTO {
    private String id;
    private String auctionName;
    private String itemName;
    private String itemType;
    private Long currentPrice;
    private LocalDateTime endTime;
    private String sellerUsername;
    private List <String> imageUrl;
    private long myLastBid;
    private boolean leading;

    public BidderDisplayDTO () {};
    
    public BidderDisplayDTO (String id, String auctionName, String itemName, String itemType, long currentPrice, LocalDateTime endTime, String sellerUsername, List <String> imageUrl)
    {
        this.id = id;
        this.auctionName = auctionName;
        this.itemName = itemName;
        this.itemType = itemType;
        this.currentPrice = currentPrice;
        this.endTime = endTime;
        this.sellerUsername = sellerUsername;
        this.imageUrl = imageUrl;
    }

    public BidderDisplayDTO(String id, String auctionName, String itemName, String itemType, long currentPrice, LocalDateTime endTime, String sellerUsername, List<String> imageUrl, long myLastBid, boolean leading) {
        this(id, auctionName, itemName, itemType, currentPrice, endTime, sellerUsername, imageUrl); // gọi lại constructor cũ
        this.myLastBid = myLastBid;
        this.leading = leading;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getAuctionName() {
        return auctionName;
    }
    public void setAuctionName(String auctionName) {
        this.auctionName = auctionName;
    }
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public String getItemType() {
        return itemType;
    }
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
    public Long getCurrentPrice() {
        return currentPrice;
    }
    public void setCurrentPrice(Long currentPrice) {
        this.currentPrice = currentPrice;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public String getSellerUsername() {
        return sellerUsername;
    }
    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }
    public List<String> getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(List<String> imageUrl) {
        this.imageUrl = imageUrl;
    }
    public long getMyLastBid() {
        return myLastBid;
    }
    public void setMyLastBid(long myLastBid) {
        this.myLastBid = myLastBid;
    }

    public boolean isLeading() {
        return leading;
    }
    public void setLeading(boolean leading) {
        this.leading = leading; 
    }

}

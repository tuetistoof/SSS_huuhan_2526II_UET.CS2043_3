package com.ssscloud.auction.common.model;

<<<<<<< HEAD
=======
import com.ssscloud.auction.common.model.base.Entity;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.observer.Subject;

>>>>>>> f7473fc67a0a3f2fa5214d775b8a5758661a0d7a
import java.time.LocalDateTime;

<<<<<<< HEAD
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.model.base.Entity;

public class Auction extends Entity {
=======
/**
 * Entity đại diện cho một phiên đấu giá
 * subject cho observer pattern
 */
public class Auction extends Entity implements Subject{

    private String title;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private LocalDateTime endTime;
    private AuctionStatus status = AuctionStatus.OPEN;
>>>>>>> f7473fc67a0a3f2fa5214d775b8a5758661a0d7a
    private String sellerId;
    private String itemId;
    private long startPrice;
    private long currentPrice;
    private long minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String highestBidderId;
<<<<<<< HEAD
    private String winnerId;
    private final int extendTime = 36;
    private String description;
    public Auction (String sellerId, String itemId, long startPrice, long currentPrice, long minIncrement, LocalDateTime starTime, LocalDateTime endTime, AuctionStatus status, String description){
=======
    private Item item;                          // Sản phẩm được đấu giá
    private List<BidTransaction> bidHistory = new ArrayList<>();

    // Constructor mặc định
    public Auction() {
    }

    // Constructor đầy đủ 
    public Auction(String title, String description, double startingPrice, 
                   LocalDateTime endTime, String sellerId, Item item) {
        this.title = title;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.endTime = endTime;
>>>>>>> f7473fc67a0a3f2fa5214d775b8a5758661a0d7a
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.minIncrement = minIncrement;
        this.startTime = starTime;
        this.endTime = endTime;
        this.status = status;
        this.highestBidderId = null;
        this.winnerId = null;
        this.description = description;
    }

<<<<<<< HEAD
    //getter setter
    // seller Id quyet dinh qua viec ai la nguoi truy cap
    public String getSellerId() {
        return sellerId;
    }

    //khi tao san pham va dang ban thi khong the sua chi co the xoa
    public String getItemId() {
        return itemId;
    }
    
    public long getStartPrice() {
        return startPrice;
    }
    public void setStartPrice(long startPrice) {
        this.startPrice = startPrice;
    }
    
    public long getCurrentPrice() {
        return currentPrice;
    }
    public void setCurrentPrice(long currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public long getMinIncrement() {
        return minIncrement;
    }
    public void setMinIncrement(long minIncrement) {
        this.minIncrement = minIncrement;
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
    public AuctionStatus getStatus() {
        return status;
    }
    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
    public String getHighestBidderId() {
        return highestBidderId;
    }
    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }
    public String getWinnerId() {
        return winnerId;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
=======
    //  GETTER & SETTER

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

    // set state methods
    public void start() {
        if (this.status == AuctionStatus.OPEN) {
            this.status = AuctionStatus.RUNNING;
        }
    }

    public void finish() {
        if (this.status == AuctionStatus.OPEN || this.status == AuctionStatus.RUNNING) {
            this.status = AuctionStatus.FINISHED;
        }
    }
 
    public void markPaid() {
        if (this.status == AuctionStatus.FINISHED) {
            this.status = AuctionStatus.PAID;
        }
    }

    public void cancel() {
        if (this.status.isActive()) {
            this.status = AuctionStatus.CANCELED;
        }
    }

    public void placeBid(BidTransaction bid) {
        //setState
        this.currentPrice          = bid.getBidAmount();
        this.highestBidderId       = bid.getBidderId();
        this.bidHistory.add(bid);
        if (this.status == AuctionStatus.OPEN) {
            this.status = AuctionStatus.RUNNING;
        }
 
        //notify
        notifyObservers();
    }

    @Override
    public void notifyObservers() {
        // Truyền chính this vào — ChangeManager tìm HashMap[this] → list observer
        ChangeManager.getInstance().notify(this);
    }

    

    //helpers
    public boolean isActive()  { return status.isActive(); }
    public boolean isExpired() { return LocalDateTime.now().isAfter(endTime); }

    @Override
    public String toString() {
        return "Auction{" +
                "id=" + getId() +
                ", title='" + title + '\'' +
                ", currentPrice=" + currentPrice +
                ", status=" + status +
                '}';
>>>>>>> f7473fc67a0a3f2fa5214d775b8a5758661a0d7a
    }
}
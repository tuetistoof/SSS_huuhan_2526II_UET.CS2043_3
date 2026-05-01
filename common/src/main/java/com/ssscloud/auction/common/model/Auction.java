package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.observer.Subject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity đại diện cho một phiên đấu giá
 * subject cho observer pattern
 */
public class Auction implements Subject {
    AuctionConfig auctionConfig;
    private AuctionStatus status;
    private String sellerId;
    private String itemId;
    private List <BidTransaction> bidTransaction;
    public Auction() {
    }
    public Auction (AuctionConfig auctionConfig,  AuctionStatus status, String sellerId, String itemId)
    {
        this.auctionConfig = auctionConfig;
        this.status = status;
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.bidTransaction = new ArrayList<>();
    }
    public Auction (AuctionConfig auctionConfig,  AuctionStatus status, String sellerId, String itemId, List <BidTransaction> bidTransaction)
    {
        this.auctionConfig = auctionConfig;
        this.status = status;
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.bidTransaction = bidTransaction;
    }
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
        // setState
        this.bidTransaction.add(bid);
        if (this.status == AuctionStatus.OPEN) {
            this.status = AuctionStatus.RUNNING;
        }
    }

    @Override
    public void notifyObservers() {
        // Truyền chính this vào — ChangeManager tìm HashMap[this] → list observer
        ChangeManager.getInstance().notify(this);
    }

    // helpers
    public boolean isActive() {
        return status.isActive();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(auctionConfig.getEndTime());
    }

    @Override
    public String toString() {
        long currentPrice = bidTransaction.isEmpty() //điều kiện kiểm tra
            ? auctionConfig.getStartPrice()     //nếu đúng
            : bidTransaction.getLast().getBidAmount(); // nếu sai
        return "Auction{" +
                "id=" + auctionConfig.getId() +
                ", name='" + auctionConfig.getName() + '\'' +
                ", currentPrice=" + currentPrice +
                ", status=" + status +
                '}';
}
    // getter setter
    public AuctionConfig getAuctionConfig() {
        return auctionConfig;
    }
    public String getSellerId() {
        return sellerId;
    }
    public String getItemId() {
        return itemId;
    }
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    public long getCurrentPrice() {
        if (!bidTransaction.isEmpty())
        {
            BidTransaction lastBidTransaction = bidTransaction.getLast();
            return lastBidTransaction.getBidAmount();
        }
        else return 0;
    }


    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public String getHighestBidderId() {
        if (!bidTransaction.isEmpty())
        {
            BidTransaction lastBidTransaction = bidTransaction.getLast();
            return lastBidTransaction.getBidderId();
        }
        else return null;
    }

    public String getHighestBidderName() {
        if (!bidTransaction.isEmpty())
        {
            BidTransaction lastBidTransaction = bidTransaction.getLast();
            return lastBidTransaction.getBidderUsername();
        }
        else return null;
    }
    public LocalDateTime getBidTime() {
        if (!bidTransaction.isEmpty())
        {
            BidTransaction lastBidTransaction = bidTransaction.getLast();
            return lastBidTransaction.getBidTime();
        }
        else return null;
    }
    public BidType getBidType() {
        if (!bidTransaction.isEmpty())
        {
            BidTransaction lastBidTransaction = bidTransaction.getLast();
            return lastBidTransaction.getType();
        }
        else return null;
    }
   
    public List<BidTransaction> getBidTransaction() {
        return bidTransaction;
    }
    public void setBidTransaction(List<BidTransaction> bidTransaction) {
        this.bidTransaction = bidTransaction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Auction auction = (Auction) o;
        // So sánh dựa trên ID duy nhất trong AuctionConfig
        return Objects.equals(this.getAuctionConfig().getId(), 
                             auction.getAuctionConfig().getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getAuctionConfig().getId());
    }
}
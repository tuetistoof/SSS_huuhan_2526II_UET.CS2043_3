package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.observer.Subject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Auction implements Subject {

    private final AuctionConfig auctionConfig;

    // volatile để đảm bảo visibility giữa các thread
    private volatile AuctionStatus status;

    private final String sellerId;
    private String itemId;

    private List<BidTransaction> bidTransaction;

    // Lock riêng cho bidTransaction
    private final ReadWriteLock bidLock = new ReentrantReadWriteLock();
    public ReadWriteLock getBidLock() {
        return bidLock;
    }
    public Auction() {
        this.auctionConfig = null;
        this.bidTransaction = new ArrayList<>();
        this.sellerId = null;
    }

    public Auction(
            AuctionConfig auctionConfig,
            AuctionStatus status,
            String sellerId,
            String itemId) {
        this.auctionConfig = auctionConfig;
        this.status = status;
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.bidTransaction = new ArrayList<>();
    }

    public Auction(
            AuctionConfig auctionConfig,
            AuctionStatus status,
            String sellerId,
            String itemId,
            List<BidTransaction> bidTransaction) {
        this.auctionConfig = auctionConfig;
        this.status = status;
        this.sellerId = sellerId;
        this.itemId = itemId;

        // Defensive copy
        this.bidTransaction = new ArrayList<>(bidTransaction);
    }

    public void placeBid(BidTransaction bid) {
        bidLock.writeLock().lock();

        try {
            this.bidTransaction.add(bid);
        } finally {
            bidLock.writeLock().unlock();
        }
    }

    public void setBidTransaction(List<BidTransaction> bidTransaction) {
        bidLock.writeLock().lock();

        try {
            this.bidTransaction = new ArrayList<>(bidTransaction);
        } finally {
            bidLock.writeLock().unlock();
        }
    }

    public BidTransaction getLastBidTransaction (){
        bidLock.readLock().lock();
        try {
            if (!bidTransaction.isEmpty()) {
                return bidTransaction.getLast();
            }
            return null;
        } finally {
            bidLock.readLock().unlock();
        }
    }

    public long getCurrentPrice() {
        bidLock.readLock().lock();

        try {
            if (!bidTransaction.isEmpty()) {
                return bidTransaction.getLast().getBidAmount();
            }

            return auctionConfig.getStartPrice();
        } finally {
            bidLock.readLock().unlock();
        }
    }

    public String getHighestBidderId() {
        bidLock.readLock().lock();

        try {
            if (!bidTransaction.isEmpty()) {
                return bidTransaction.getLast().getBidderId();
            }

            return null;
        } finally {
            bidLock.readLock().unlock();
        }
    }

    public String getHighestBidderName() {
        bidLock.readLock().lock();

        try {
            if (!bidTransaction.isEmpty()) {
                return bidTransaction.getLast().getBidderUsername();
            }

            return null;
        } finally {
            bidLock.readLock().unlock();
        }
    }

    public LocalDateTime getBidTime() {
        bidLock.readLock().lock();

        try {
            if (!bidTransaction.isEmpty()) {
                return bidTransaction.getLast().getBidTime();
            }

            return null;
        } finally {
            bidLock.readLock().unlock();
        }
    }

    public BidType getBidType() {
        bidLock.readLock().lock();

        try {
            if (!bidTransaction.isEmpty()) {
                return bidTransaction.getLast().getType();
            }

            return null;
        } finally {
            bidLock.readLock().unlock();
        }
    }

    public List<BidTransaction> getBidTransaction() {
        bidLock.readLock().lock();

        try {
            return new ArrayList<>(this.bidTransaction);
        } finally {
            bidLock.readLock().unlock();
        }
    }

    public int getBidCount() {
        bidLock.readLock().lock();

        try {
            return bidTransaction.size();
        } finally {
            bidLock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        bidLock.readLock().lock();

        try {
            long currentPrice = bidTransaction.isEmpty()
                    ? auctionConfig.getStartPrice()
                    : bidTransaction.getLast().getBidAmount();

            return "Auction{" +
                    "id=" + auctionConfig.getId() +
                    ", name='" + auctionConfig.getName() + '\'' +
                    ", currentPrice=" + currentPrice +
                    ", status=" + status +
                    '}';
        } finally {
            bidLock.readLock().unlock();
        }
    }

    // =========================
    // Status methods
    // =========================

    public void start() {
        if (this.status == AuctionStatus.OPEN) {
            this.status = AuctionStatus.RUNNING;
        }
    }

    public void finish() {
        if (this.status == AuctionStatus.OPEN
                || this.status == AuctionStatus.RUNNING) {

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

    @Override
    public void notifyObservers() {
        ChangeManager.getInstance().notify(this);
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(auctionConfig.getEndTime());
    }

    // =========================
    // Getters / Setters
    // =========================

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

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    // =========================
    // equals / hashCode
    // =========================

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Auction auction = (Auction) o;

        return Objects.equals(
                this.getAuctionConfig().getId(),
                auction.getAuctionConfig().getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getAuctionConfig().getId());
    }
}
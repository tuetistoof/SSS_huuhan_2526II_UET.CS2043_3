package com.ssscloud.auction.common.model.auction;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.observer.Subject;
import com.ssscloud.auction.common.payload.response.DTO.AuctionDTO;
import com.ssscloud.auction.common.payload.response.DTO.BidDTO;
import com.ssscloud.auction.common.payload.response.DTO.ItemDTO;
import com.ssscloud.auction.common.payload.response.DTO.UserDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Auction implements Subject {

    private final AuctionConfig auctionConfig;
    private volatile AuctionStatus status;
    private final String sellerId;
    private volatile String itemId;
    private List<BidTransaction> bidTransaction;

    private final ReadWriteLock bidLock = new ReentrantReadWriteLock(true);
    private final ReadWriteLock auctionLock = new ReentrantReadWriteLock(true);
    private final AtomicLong snapshotVersion = new AtomicLong(0);
    private final AtomicReference<AuctionSnapshot> snapshot = new AtomicReference<>();

    public Auction() {
        this.auctionConfig = null;
        this.bidTransaction = new ArrayList<>();
        this.sellerId = null;
        publishSnapshotWithoutLock();
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
        publishSnapshotWithoutLock();
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
        this.bidTransaction = copyTransactions(bidTransaction);
        publishSnapshotWithoutLock();
    }

    public ReadWriteLock getAuctionLock() {
        return auctionLock;
    }

    public ReadWriteLock getBidLock() {
        return bidLock;
    }

    public AuctionSnapshot snapshot() {
        return snapshot.get();
    }

    public void refreshSnapshot() {
        bidLock.readLock().lock();
        try {
            publishSnapshotWithoutLock();
        } finally {
            bidLock.readLock().unlock();
        }
    }

    public void placeBid(BidTransaction bid) {
        commitBid(bid, false, null);
    }

    public void commitBid(BidTransaction bid, boolean markRunning, LocalDateTime newEndTime) {
        Objects.requireNonNull(bid, "bid");
        auctionLock.writeLock().lock();
        try {
            bidLock.writeLock().lock();
            try {
                this.bidTransaction.add(new BidTransaction(bid));
                if (markRunning && this.status == AuctionStatus.OPEN) {
                    this.status = AuctionStatus.RUNNING;
                }
                if (newEndTime != null && this.auctionConfig != null) {
                    this.auctionConfig.setEndTime(newEndTime);
                }
                publishSnapshotWithoutLock();
            } finally {
                bidLock.writeLock().unlock();
            }
        } finally {
            auctionLock.writeLock().unlock();
        }
    }

    public void setBidTransaction(List<BidTransaction> bidTransaction) {
        bidLock.writeLock().lock();
        try {
            this.bidTransaction = copyTransactions(bidTransaction);
            publishSnapshotWithoutLock();
        } finally {
            bidLock.writeLock().unlock();
        }
    }

    public BidTransaction getLastBidTransaction() {
        return snapshot.get().getLastBidTransaction();
    }

    public long getCurrentPrice() {
        return snapshot.get().getCurrentPrice();
    }

    public String getHighestBidderId() {
        return snapshot.get().getHighestBidderId();
    }

    public String getHighestBidderName() {
        return snapshot.get().getHighestBidderName();
    }

    public LocalDateTime getBidTime() {
        return snapshot.get().getBidTime();
    }

    public BidType getBidType() {
        return snapshot.get().getBidType();
    }

    public List<BidTransaction> getBidTransaction() {
        return snapshot.get().getBidTransactions();
    }

    public int getBidCount() {
        return snapshot.get().getBidCount();
    }

    public BidDTO toBidDtoForBidUpdate() {
        AuctionSnapshot currentSnapshot = snapshot.get();
        BidTransaction lastBidTransaction = currentSnapshot.getLastBidTransaction();
        if (lastBidTransaction == null) {
            return null;
        }

        BidDTO bidDto = toBidDto(lastBidTransaction);
        bidDto.setAntiSnipingEndTime(currentSnapshot.getEndTime());
        bidDto.setVersion(currentSnapshot.getVersion());
        return bidDto;
    }

    public Map<String, Object> buildAuctionEndedPayload(Auction auction) {
        Auction targetAuction = auction != null ? auction : this;
        AuctionSnapshot currentSnapshot = targetAuction.snapshot();
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId", currentSnapshot.getId());
        payload.put("auctionName", currentSnapshot.getName());
        payload.put("finalPrice", currentSnapshot.getCurrentPrice());
        payload.put("winnerName", currentSnapshot.getHighestBidderName() != null
                ? currentSnapshot.getHighestBidderName() : "No bids placed");
        payload.put("version", currentSnapshot.getVersion());
        return payload;
    }

    public AuctionDTO toAuctionDto(Auction auction, UserDTO sellerDto, ItemDTO itemDto) {
        Auction targetAuction = auction != null ? auction : this;
        AuctionSnapshot currentSnapshot = targetAuction.snapshot();

        AuctionDTO auctionDto = new AuctionDTO();
        auctionDto.setId(currentSnapshot.getId());
        auctionDto.setName(currentSnapshot.getName());
        auctionDto.setStartPrice(currentSnapshot.getStartPrice());
        auctionDto.setMinIncrement(currentSnapshot.getMinIncrement());
        auctionDto.setStartTime(currentSnapshot.getStartTime());
        auctionDto.setEndTime(currentSnapshot.getEndTime());
        auctionDto.setStatus(currentSnapshot.getStatus());
        auctionDto.setVersion(currentSnapshot.getVersion());

        List<BidDTO> bidDtoList = new ArrayList<>();
        for (BidTransaction transaction : currentSnapshot.getBidTransactions()) {
            BidDTO bidDto = toBidDto(transaction);
            bidDto.setAntiSnipingEndTime(currentSnapshot.getEndTime());
            bidDto.setVersion(currentSnapshot.getVersion());
            bidDtoList.add(bidDto);
        }

        auctionDto.setBidDto(bidDtoList);
        auctionDto.setSellerDTO(sellerDto);
        auctionDto.setItemDTO(itemDto);
        return auctionDto;
    }

    @Override
    public String toString() {
        AuctionSnapshot currentSnapshot = snapshot.get();
        return "Auction{"
                + "id=" + currentSnapshot.getId()
                + ", name='" + currentSnapshot.getName() + '\''
                + ", currentPrice=" + currentSnapshot.getCurrentPrice()
                + ", status=" + currentSnapshot.getStatus()
                + ", version=" + currentSnapshot.getVersion()
                + '}';
    }

    public void start() {
        if (this.status == AuctionStatus.OPEN) {
            setStatus(AuctionStatus.RUNNING);
        }
    }

    public void finish() {
        if (this.status == AuctionStatus.OPEN || this.status == AuctionStatus.RUNNING) {
            setStatus(AuctionStatus.FINISHED);
        }
    }

    public void markPaid() {
        if (this.status == AuctionStatus.FINISHED) {
            setStatus(AuctionStatus.PAID);
        }
    }

    public void cancel() {
        if (this.status != null && this.status.isActive()) {
            setStatus(AuctionStatus.CANCELED);
        }
    }

    @Override
    public void notifyObservers() {
        ChangeManager.getInstance().notify(this);
    }

    public boolean isActive() {
        return status != null && status.isActive();
    }

    public boolean isExpired() {
        LocalDateTime endTime = snapshot.get().getEndTime();
        return endTime != null && LocalDateTime.now().isAfter(endTime);
    }

    public AuctionConfig getAuctionConfig() {
        return auctionConfig;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getItemId() {
        return snapshot.get().getItemId();
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
        refreshSnapshot();
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        auctionLock.writeLock().lock();
        try {
            this.status = status;
            refreshSnapshot();
        } finally {
            auctionLock.writeLock().unlock();
        }
    }

    private void publishSnapshotWithoutLock() {
        snapshot.set(AuctionSnapshot.from(
                auctionConfig,
                status,
                sellerId,
                itemId,
                bidTransaction,
                snapshotVersion.incrementAndGet()));
    }

    private static List<BidTransaction> copyTransactions(List<BidTransaction> transactions) {
        List<BidTransaction> copiedTransactions = new ArrayList<>();
        if (transactions == null) {
            return copiedTransactions;
        }
        for (BidTransaction transaction : transactions) {
            copiedTransactions.add(new BidTransaction(transaction));
        }
        return copiedTransactions;
    }

    private static BidDTO toBidDto(BidTransaction bidTransaction) {
        BidDTO bidDto = new BidDTO();
        bidDto.setAuctionId(bidTransaction.getAuctionId());
        bidDto.setBidderId(bidTransaction.getBidderId());
        bidDto.setBidderUsername(bidTransaction.getBidderUsername());
        bidDto.setBidAmount(bidTransaction.getBidAmount());
        bidDto.setLockedBalance(bidTransaction.getLockedBalance());
        bidDto.setBidTime(bidTransaction.getBidTime());
        bidDto.setBidType(bidTransaction.getType() != null ? bidTransaction.getType().name() : null);
        return bidDto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Auction auction = (Auction) o;
        String thisId = this.getAuctionConfig() != null ? this.getAuctionConfig().getId() : null;
        String otherId = auction.getAuctionConfig() != null ? auction.getAuctionConfig().getId() : null;
        return Objects.equals(thisId, otherId);
    }

    @Override
    public int hashCode() {
        String id = this.getAuctionConfig() != null ? this.getAuctionConfig().getId() : null;
        return Objects.hash(id);
    }
}

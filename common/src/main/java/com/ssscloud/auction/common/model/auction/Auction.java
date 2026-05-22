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
    private final ReadWriteLock auctionLock = new ReentrantReadWriteLock() {
        
    }; 
    
    public ReadWriteLock getAuctionLock() {
        return auctionLock;
    }

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

    public BidTransaction getLastBidTransaction() {
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

    public BidDTO toBidDtoForBidUpdate() {
        bidLock.readLock().lock();

        try {
            BidTransaction bidTransaction = this.getLastBidTransaction();
            if (bidTransaction == null) {
                return null;
            }

            BidDTO bidDto = new BidDTO();
            bidDto.setAuctionId(bidTransaction.getAuctionId());
            bidDto.setBidderId(bidTransaction.getBidderId());
            bidDto.setBidderUsername(bidTransaction.getBidderUsername());
            bidDto.setBidAmount(bidTransaction.getBidAmount());
            bidDto.setLockedBalance(bidTransaction.getLockedBalance());
            bidDto.setBidTime(bidTransaction.getBidTime());
            bidDto.setAntiSnipingEndTime(auctionConfig.getEndTime());
            bidDto.setBidType(bidTransaction.getType().name());
            return bidDto;
        } finally {
            bidLock.readLock().unlock();
        }
    }

    public Map<String, Object> buildAuctionEndedPayload(Auction auction) {
        auctionLock.readLock().lock();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("auctionId", auction.getAuctionConfig().getId());
            payload.put("auctionName", auction.getAuctionConfig().getName());
            payload.put("finalPrice", auction.getCurrentPrice());
            payload.put("winnerName", auction.getHighestBidderName() != null
                    ? auction.getHighestBidderName() : "No bids placed");
            return payload;
        } finally {
            auctionLock.readLock().unlock();
        }
    }
    
    public AuctionDTO toAuctionDto(Auction auction, UserDTO sellerDto, ItemDTO itemDto) {
        AuctionDTO auctionDto = new AuctionDTO();

        // 1. Đọc các thông tin cấu hình cơ bản (Sử dụng auctionLock)
        auctionLock.readLock().lock();
        try {
            auctionDto.setId(this.auctionConfig.getId());
            auctionDto.setName(this.auctionConfig.getName());
            auctionDto.setStartPrice(this.auctionConfig.getStartPrice());
            auctionDto.setMinIncrement(this.auctionConfig.getMinIncrement());
            auctionDto.setStartTime(this.auctionConfig.getStartTime());
            auctionDto.setEndTime(this.auctionConfig.getEndTime());
            auctionDto.setStatus(this.status);
        } finally {
            auctionLock.readLock().unlock();
        }

        List<BidTransaction> snapshotBids;
        bidLock.readLock().lock();
        try {
            snapshotBids = new ArrayList<>(this.bidTransaction);
        } finally {
            bidLock.readLock().unlock();
        }

        // 3. Ánh xạ danh sách giao dịch thầu sang danh sách BidDTO (Không giữ lock khi map)
        List<BidDTO> bidDtoList = new ArrayList<>();
        if (snapshotBids != null && !snapshotBids.isEmpty()) {
            for (BidTransaction tx : snapshotBids) {
                BidDTO bidDto = new BidDTO();
                bidDto.setAuctionId(tx.getAuctionId());
                bidDto.setBidderId(tx.getBidderId());
                bidDto.setBidderUsername(tx.getBidderUsername());
                bidDto.setBidAmount(tx.getBidAmount());
                bidDto.setLockedBalance(tx.getLockedBalance());
                bidDto.setBidTime(tx.getBidTime());
                bidDto.setBidType(tx.getType().name());
                
                // Anti-sniping end time tại thời điểm transaction này được tạo ra
                bidDto.setAntiSnipingEndTime(auctionDto.getEndTime());
                
                bidDtoList.add(bidDto);
            }
        }

        auctionDto.setBidDto(bidDtoList);
        auctionDto.setSellerDTO(sellerDto);
        auctionDto.setItemDTO(itemDto);

        return auctionDto;
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
        auctionLock.readLock().lock();

        try {
            return LocalDateTime.now().isAfter(auctionConfig.getEndTime());
        } finally {
            auctionLock.readLock().unlock();
        }
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
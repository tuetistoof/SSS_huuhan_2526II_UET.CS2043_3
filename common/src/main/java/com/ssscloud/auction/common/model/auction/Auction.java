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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Auction implements Subject {

    private final AuctionConfig auctionConfig;
    private volatile AuctionStatus status; 
    private final String sellerId;
    private String itemId;
    private List<BidTransaction> bidTransaction;

    // ĐÃ TỐI ƯU: Thay thế ReadWriteLock bằng ReentrantLock đơn giản, hiệu năng cao hơn
    private final Lock mutationLock = new ReentrantLock(true); 
    
    private final AtomicLong snapshotVersion = new AtomicLong(0);
    private final AtomicReference<AuctionSnapshot> snapshot = new AtomicReference<>();

    // --- Constructors ---

    public Auction() {
        this.auctionConfig  = null;
        this.sellerId       = null;
        this.bidTransaction = new ArrayList<>();
        publishSnapshotWithoutLock();
    }

    public Auction(AuctionConfig auctionConfig, AuctionStatus status, String sellerId, String itemId) {
        this.auctionConfig  = auctionConfig;
        this.status         = status;
        this.sellerId       = sellerId;
        this.itemId         = itemId;
        this.bidTransaction = new ArrayList<>();
        publishSnapshotWithoutLock();
    }

    public Auction(AuctionConfig auctionConfig, AuctionStatus status, String sellerId, String itemId, List<BidTransaction> bidTransaction) {
        this.auctionConfig  = auctionConfig;
        this.status         = status;
        this.sellerId       = sellerId;
        this.itemId         = itemId;
        this.bidTransaction = copyTransactions(bidTransaction);
        publishSnapshotWithoutLock();
    }

    // --- Snapshot access ---
    public AuctionSnapshot snapshot() { return snapshot.get(); }

    // --- Getters (Không cần dùng lock vì đã có snapshot cô lập dữ liệu) ---

    public AuctionConfig getAuctionConfig() { return auctionConfig; }
    public String getSellerId()             { return sellerId; }
    public String getItemId()               { return itemId; }

    public void setItemId(String itemId) {
        mutationLock.lock(); // Đảm bảo ghi tuần tự
        try {
            this.itemId = itemId;
            publishSnapshotWithoutLock();
        } // Thay vì notify luôn ở đây, ta thường để luồng Service quyết định hoặc gọi ở cuối hàm
        finally {
            mutationLock.unlock();
        }
    }
    public AtomicReference<AuctionSnapshot> getSnapshot() {
        return snapshot;
    }
    public AuctionStatus getStatus() {
        return status;
    }

    public LocalDateTime getEndTime() {
        return snapshot.get().getEndTime();
    }

    public BidTransaction getLastBidTransaction() {
        return snapshot.get().getLastBidTransaction(); 
    }

    public long getCurrentPrice() {
        BidTransaction last = snapshot.get().getLastBidTransaction();
        return last != null ? last.getBidAmount()
                            : (auctionConfig != null ? auctionConfig.getStartPrice() : 0L);
    }

    public String getHighestBidderId() {
        BidTransaction last = snapshot.get().getLastBidTransaction();
        return last != null ? last.getBidderId() : null;
    }

    public String getHighestBidderName() {
        BidTransaction last = snapshot.get().getLastBidTransaction();
        return last != null ? last.getBidderUsername() : null;
    }

    public LocalDateTime getBidTime() {
        BidTransaction last = snapshot.get().getLastBidTransaction();
        return last != null ? last.getBidTime() : null;
    }

    public BidType getBidType() {
        BidTransaction last = snapshot.get().getLastBidTransaction();
        return last != null ? last.getType() : null;
    }

    public List<BidTransaction> getBidTransaction() { return snapshot.get().getBidTransactions(); }
    public int getBidCount()                        { return snapshot.get().getBidCount(); }

    // --- Mutation (Chỉ ghi mới cần khóa mutationLock) ---

    public void commitBid(BidTransaction bid, boolean markRunning, LocalDateTime newEndTime) {
        Objects.requireNonNull(bid, "bid");
        mutationLock.lock(); 
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
            mutationLock.unlock();
        }
    }

    public void placeBid(BidTransaction bid) {
        commitBid(bid, false, null);
    }

    public void setBidTransaction(List<BidTransaction> bidTransaction) {
        mutationLock.lock();
        try {
            this.bidTransaction = copyTransactions(bidTransaction);
            publishSnapshotWithoutLock();
        } finally {
            mutationLock.unlock();
        }
    }

    public void setStatus(AuctionStatus status) {
        mutationLock.lock();
        try {
            this.status = status;
            publishSnapshotWithoutLock();
        } finally {
            mutationLock.unlock();
        }
    }

    public void start()    { if (status == AuctionStatus.OPEN)                                setStatus(AuctionStatus.RUNNING);  }
    public void finish()   { if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING) setStatus(AuctionStatus.FINISHED); }
    public void markPaid() { if (status == AuctionStatus.FINISHED)                                setStatus(AuctionStatus.PAID);     }
    public void cancel()   { if (status != null && status.isActive())                             setStatus(AuctionStatus.CANCELED); }

    // --- State queries ---
    public boolean isActive() { return status != null && status.isActive(); }
    public boolean isExpired() {
        LocalDateTime endTime = snapshot.get().getEndTime();
        return endTime != null && LocalDateTime.now().isAfter(endTime);
    }

    // --- DTO builders ---
    public BidDTO toBidDtoForBidUpdate() {
        AuctionSnapshot s   = snapshot.get();
        BidTransaction last = s.getLastBidTransaction();
        if (last == null) return null;
        BidDTO dto = toBidDto(last);
        dto.setAntiSnipingEndTime(s.getEndTime());
        dto.setVersion(s.getVersion());
        return dto;
    }

    public Map<String, Object> buildAuctionEndedPayload() {
        AuctionSnapshot s   = snapshot.get();
        BidTransaction last = s.getLastBidTransaction();
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId",   auctionConfig != null ? auctionConfig.getId()   : null);
        payload.put("auctionName", auctionConfig != null ? auctionConfig.getName() : null);
        payload.put("finalPrice",  last != null ? last.getBidAmount()
                                               : (auctionConfig != null ? auctionConfig.getStartPrice() : 0L));
        payload.put("winnerName",  last != null ? last.getBidderUsername() : "No bids placed");
        payload.put("version",     s.getVersion());
        return payload;
    }

    public AuctionDTO toAuctionDto(UserDTO sellerDto, ItemDTO itemDto) {
        AuctionSnapshot s   = snapshot.get();
        AuctionDTO dto = new AuctionDTO();

        if (auctionConfig != null) {
            dto.setId(auctionConfig.getId());
            dto.setName(auctionConfig.getName());
            dto.setStartPrice(auctionConfig.getStartPrice());
            dto.setMinIncrement(auctionConfig.getMinIncrement());
            dto.setStartTime(auctionConfig.getStartTime());
        }

        dto.setEndTime(s.getEndTime());
        dto.setStatus(s.getStatus());
        dto.setVersion(s.getVersion());

        List<BidDTO> bidDtoList = new ArrayList<>();
        for (BidTransaction t : s.getBidTransactions()) {
            BidDTO bidDto = toBidDto(t);
            bidDto.setAntiSnipingEndTime(s.getEndTime());
            bidDto.setVersion(s.getVersion());
            bidDtoList.add(bidDto);
        }
        dto.setBidDto(bidDtoList);
        dto.setSellerDTO(sellerDto);
        dto.setItemDTO(itemDto);
        return dto;
    }

    @Override
    public String toString() {
        AuctionSnapshot s   = snapshot.get();
        BidTransaction last = s.getLastBidTransaction();
        long price = last != null ? last.getBidAmount()
                                  : (auctionConfig != null ? auctionConfig.getStartPrice() : 0L);
        return "Auction{" + "id=" + (auctionConfig != null ? auctionConfig.getId() : null)
                + ", name='" + (auctionConfig != null ? auctionConfig.getName() : null) + '\''
                + ", currentPrice=" + price + ", status=" + s.getStatus() + ", version=" + s.getVersion() + '}';
    }

    @Override
    public void notifyObservers() { ChangeManager.getInstance().notify(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Auction other = (Auction) o;
        String thisId  = auctionConfig != null ? auctionConfig.getId() : null;
        String otherId = other.auctionConfig != null ? other.auctionConfig.getId() : null;
        return Objects.equals(thisId, otherId);
    }

    @Override
    public int hashCode() { return Objects.hash(auctionConfig != null ? auctionConfig.getId() : null); }

    // --- Private helpers ---
    private void publishSnapshotWithoutLock() {
        snapshot.set(AuctionSnapshot.from(
                auctionConfig != null ? auctionConfig.getEndTime() : null,
                status,
                bidTransaction,
                snapshotVersion.incrementAndGet()));
    }

    private static List<BidTransaction> copyTransactions(List<BidTransaction> transactions) {
        if (transactions == null) return new ArrayList<>();
        List<BidTransaction> copy = new ArrayList<>(transactions.size());
        for (BidTransaction t : transactions) copy.add(new BidTransaction(t));
        return copy;
    }

    private static BidDTO toBidDto(BidTransaction t) {
        BidDTO dto = new BidDTO();
        dto.setAuctionId(t.getAuctionId());
        dto.setBidderId(t.getBidderId());
        dto.setBidderUsername(t.getBidderUsername());
        dto.setBidAmount(t.getBidAmount());
        dto.setLockedBalance(t.getLockedBalance());
        dto.setBidTime(t.getBidTime());
        dto.setBidType(t.getType() != null ? t.getType().name() : null);
        return dto;
    }
}

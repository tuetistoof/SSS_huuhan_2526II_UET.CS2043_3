package com.ssscloud.auction.common.model.auction;

import com.ssscloud.auction.common.enums.AuctionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Snapshot bất biến lưu trữ trạng thái động của một phiên đấu giá tại một thời điểm nhất định.
 *
 * Stored
 *   - endTime         
 *   - status         
 *   - bidTransactions 
 *   - version         
 */
public final class AuctionSnapshot {

    private final LocalDateTime endTime;
    private final AuctionStatus status;
    private final List<BidTransaction> bidTransactions; // unmodifiable
    private final long version;

    private AuctionSnapshot(
            LocalDateTime endTime,
            AuctionStatus status,
            List<BidTransaction> bidTransactions,
            long version) {
        this.endTime         = endTime;
        this.status          = status;
        this.bidTransactions = bidTransactions;
        this.version         = version;
    }

    public static AuctionSnapshot from(
            LocalDateTime endTime,
            AuctionStatus status,
            List<BidTransaction> bidTransactions,
            long version) {

        List<BidTransaction> copied = copyTransactions(bidTransactions);
        return new AuctionSnapshot(
                endTime,
                status,
                Collections.unmodifiableList(copied),
                version);
    }

    // --- Getters ---

    public LocalDateTime getEndTime()  { return endTime; }
    public AuctionStatus getStatus()   { return status; }
    public long getVersion()           { return version; }

    public List<BidTransaction> getBidTransactions() {
        return copyTransactions(bidTransactions);
    }

    public BidTransaction getLastBidTransaction() {
        if (bidTransactions.isEmpty()) return null;
        return new BidTransaction(bidTransactions.getLast());
    }
    
    public int getBidCount() {
        return bidTransactions.size();
    }

    // --- Private helper ---

    private static List<BidTransaction> copyTransactions(List<BidTransaction> src) {
        if (src == null) return new ArrayList<>();
        List<BidTransaction> copy = new ArrayList<>(src.size());
        for (BidTransaction t : src) copy.add(new BidTransaction(t));
        return copy;
    }
}
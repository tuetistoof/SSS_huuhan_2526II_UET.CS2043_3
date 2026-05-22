package com.ssscloud.auction.common.model.auction;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.base.AuctionConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AuctionSnapshot {
    private final String id;
    private final String name;
    private final long startPrice;
    private final long minIncrement;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final AuctionStatus status;
    private final String sellerId;
    private final String itemId;
    private final List<BidTransaction> bidTransactions;
    private final BidTransaction lastBidTransaction;
    private final long currentPrice;
    private final String highestBidderId;
    private final String highestBidderName;
    private final LocalDateTime bidTime;
    private final BidType bidType;
    private final int bidCount;
    private final long version;

    private AuctionSnapshot(
            String id,
            String name,
            long startPrice,
            long minIncrement,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AuctionStatus status,
            String sellerId,
            String itemId,
            List<BidTransaction> bidTransactions,
            BidTransaction lastBidTransaction,
            long currentPrice,
            String highestBidderId,
            String highestBidderName,
            LocalDateTime bidTime,
            BidType bidType,
            int bidCount,
            long version) {
        this.id = id;
        this.name = name;
        this.startPrice = startPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.bidTransactions = bidTransactions;
        this.lastBidTransaction = lastBidTransaction;
        this.currentPrice = currentPrice;
        this.highestBidderId = highestBidderId;
        this.highestBidderName = highestBidderName;
        this.bidTime = bidTime;
        this.bidType = bidType;
        this.bidCount = bidCount;
        this.version = version;
    }

    public static AuctionSnapshot from(
            AuctionConfig auctionConfig,
            AuctionStatus status,
            String sellerId,
            String itemId,
            List<BidTransaction> bidTransactions,
            long version) {
        String id = auctionConfig != null ? auctionConfig.getId() : null;
        String name = auctionConfig != null ? auctionConfig.getName() : null;
        long startPrice = auctionConfig != null ? auctionConfig.getStartPrice() : 0L;
        long minIncrement = auctionConfig != null ? auctionConfig.getMinIncrement() : 0L;
        LocalDateTime startTime = auctionConfig != null ? auctionConfig.getStartTime() : null;
        LocalDateTime endTime = auctionConfig != null ? auctionConfig.getEndTime() : null;

        List<BidTransaction> copiedTransactions = copyTransactions(bidTransactions);
        BidTransaction lastBid = copiedTransactions.isEmpty()
                ? null
                : new BidTransaction(copiedTransactions.getLast());
        long currentPrice = lastBid != null ? lastBid.getBidAmount() : startPrice;
        String highestBidderId = lastBid != null ? lastBid.getBidderId() : null;
        String highestBidderName = lastBid != null ? lastBid.getBidderUsername() : null;
        LocalDateTime bidTime = lastBid != null ? lastBid.getBidTime() : null;
        BidType bidType = lastBid != null ? lastBid.getType() : null;

        return new AuctionSnapshot(
                id,
                name,
                startPrice,
                minIncrement,
                startTime,
                endTime,
                status,
                sellerId,
                itemId,
                Collections.unmodifiableList(copiedTransactions),
                lastBid,
                currentPrice,
                highestBidderId,
                highestBidderName,
                bidTime,
                bidType,
                copiedTransactions.size(),
                version);
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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getStartPrice() {
        return startPrice;
    }

    public long getMinIncrement() {
        return minIncrement;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getItemId() {
        return itemId;
    }

    public List<BidTransaction> getBidTransactions() {
        return copyTransactions(bidTransactions);
    }

    public BidTransaction getLastBidTransaction() {
        return lastBidTransaction == null ? null : new BidTransaction(lastBidTransaction);
    }

    public long getCurrentPrice() {
        return currentPrice;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public String getHighestBidderName() {
        return highestBidderName;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public BidType getBidType() {
        return bidType;
    }

    public int getBidCount() {
        return bidCount;
    }

    public long getVersion() {
        return version;
    }
}

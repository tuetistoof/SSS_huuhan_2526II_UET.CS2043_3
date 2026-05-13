package com.ssscloud.auction.server.util;

import com.ssscloud.auction.common.model.Auction;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionRegistry {
    private static final AuctionRegistry instance = new AuctionRegistry();
    private AuctionRegistry() {}
    public static AuctionRegistry getInstance() { return instance; }

    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    public void register(Auction auction) {
        activeAuctions.put(auction.getAuctionConfig().getId(), auction);
    }

    public Auction get(String auctionId) {
        return activeAuctions.get(auctionId);
    }

    // Alias dùng ở MessageHandler (SUBSCRIBE_AUCTION)
    public Auction getLiveAuction(String auctionId) {
        return activeAuctions.get(auctionId);
    }

    public void remove(String auctionId) {
        activeAuctions.remove(auctionId);
    }

    // Dùng ở recoverLiveAuctions + startAuctionCloser
    public Collection<Auction> getAllLive() {
        return activeAuctions.values();
    }
}
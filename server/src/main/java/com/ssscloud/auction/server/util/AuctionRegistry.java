package com.ssscloud.auction.server.util;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ssscloud.auction.common.model.auction.Auction;

public class AuctionRegistry {
    private static final AuctionRegistry instance = new AuctionRegistry();
    private AuctionRegistry() {}
    public static AuctionRegistry getInstance() { 
        return instance; 
    }

    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    public void registerIfAbsent(Auction auction) {  
        activeAuctions.putIfAbsent(auction.getAuctionConfig().getId(), auction);
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
    public boolean removeIfNoObservers(String auctionId, int currentObserverCount) {
        boolean[] removed = {false};
        activeAuctions.computeIfPresent(auctionId, (id, auction) -> {
            if (currentObserverCount == 0) {
                removed[0] = true;
                return null;    // returning null removes the entry from the map
            }
            return auction;     // keep entry
        });
        return removed[0];
    }

    // Dùng ở recoverLiveAuctions + startAuctionCloser
    public Collection<Auction> getAllLive() {
        return activeAuctions.values();
    }
}
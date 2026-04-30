package com.ssscloud.auction.server.util;

import com.ssscloud.auction.common.model.Auction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionRegistry {
    private static final AuctionRegistry instance = new AuctionRegistry();
    
    private final Map<String, Auction> liveAuctions = new ConcurrentHashMap<>();

    private AuctionRegistry() {}

    public static AuctionRegistry getInstance() { 
        return instance; 
    }

    // 3. Hàm này dùng để nạp Auction vào kho (Gọi lúc CreateAuction)
    public void register(Auction auction) {
        liveAuctions.put(auction.getAuctionConfig().getId(), auction);
    }

    // 4. Hàm này dùng để lôi Auction ra (Gọi lúc Client Subscribe)
    public Auction getLiveAuction(String auctionId) {
        return liveAuctions.get(auctionId);
    }
}

/**
 * NotificationService lo push đến user NGOÀI phòng (Watch List).
 */

package com.ssscloud.auction.server.service;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.WatchlistDAO;

public class NotificationService {
    private static final NotificationService instance = new NotificationService();
    private NotificationService() {}
    public static NotificationService getInstance() { return instance; }

    private WatchlistDAO watchlistDAO;
    public void init(WatchlistDAO dao) {
        this.watchlistDAO = dao;
    }

    public void notifyWatchers(Auction auction, String highestBidderId) {
        if (watchlistDAO == null) return;
 
        String auctionId   = auction.getAuctionConfig().getId();
        String auctionName = auction.getAuctionConfig().getName();
        long   currentPrice = auction.getCurrentPrice();
 
        List<String> watchers = watchlistDAO.findUserIdsByAuction(auctionId);
 
        for (String watcherId : watchers) {
            // Bỏ qua người vừa thắng
            if (watcherId.equals(highestBidderId)) continue;
 
            // Bỏ qua user đang trong phòng — họ đã nhận BID_UPDATE + OUTBID qua RoomObserver
            if (isInRoom(auctionId, watcherId)) continue;
 
            PrintWriter writer = SessionRegistry.getInstance().getWriter(watcherId);
            if (writer == null) continue; // user offline
 
            pushOutbid(writer, auctionId, auctionName, currentPrice);
        }
    }
    public void notifyAuctionEnded(Auction auction) {
        if (watchlistDAO == null) return;
 
        String auctionId   = auction.getAuctionConfig().getId();
        String auctionName = auction.getAuctionConfig().getName();
        long   finalPrice  = auction.getCurrentPrice();
        String winner      = auction.getHighestBidderName() != null
                             ? auction.getHighestBidderName() : "Không có người đặt giá";
 
        List<String> watchers = watchlistDAO.findUserIdsByAuction(auctionId);
 
        for (String watcherId : watchers) {
            if (isInRoom(auctionId, watcherId)) continue; // đã nhận qua RoomObserver
 
            PrintWriter writer = SessionRegistry.getInstance().getWriter(watcherId);
            if (writer == null) continue;
 
            pushAuctionEnded(writer, auctionId, auctionName, finalPrice, winner);
        }
    }
    private void pushOutbid(PrintWriter writer, String auctionId,
                             String auctionName, long currentPrice) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId",    auctionId);
        payload.put("auctionName",  auctionName);
        payload.put("currentPrice", currentPrice);
        payload.put("type",         "OUTBID");
 
        synchronized (writer) {
            writer.println(JsonUtils.toJson(
                    ClientMessage.push("OUTBID_NOTIFICATION", payload)));
        }
    }
 
    private void pushAuctionEnded(PrintWriter writer, String auctionId,
                                   String auctionName, long finalPrice, String winner) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId",   auctionId);
        payload.put("auctionName", auctionName);
        payload.put("finalPrice",  finalPrice);
        payload.put("winner",      winner);
        payload.put("type",        "ENDED");
 
        synchronized (writer) {
            writer.println(JsonUtils.toJson(
                    ClientMessage.push("AUCTION_ENDED_NOTIFICATION", payload)));
        }
    }
    private boolean isInRoom(String auctionId, String watcherId) {
        Auction auction = com.ssscloud.auction.server.util.AuctionRegistry
                .getInstance().getLiveAuction(auctionId);
        if (auction == null) return false;
 
        return ChangeManager.getInstance().hasObserver(auction, watcherId);
    }

}


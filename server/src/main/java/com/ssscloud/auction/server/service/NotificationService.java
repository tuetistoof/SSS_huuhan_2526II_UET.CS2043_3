/**
 * NotificationService handles push notifications to users outside of the bidding room (Watchlist users).
 */
package com.ssscloud.auction.server.service;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.common.exception.ServiceExceptions;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.server.dao.WatchlistDAO;
import com.ssscloud.auction.server.util.SessionRegistry;
import com.ssscloud.auction.server.util.AuctionRegistry;

public class NotificationService {
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName()); // Logging Standards: Declared first

    private static final NotificationService instance = new NotificationService();
    private WatchlistDAO watchlistDAO;

    private NotificationService() {}

    public static NotificationService getInstance() { return instance; }

    // --- PUBLIC METHODS ---

    public void init(WatchlistDAO watchlistDAO) {
        this.watchlistDAO = watchlistDAO;
        logger.log(Level.INFO, "[NotificationService] Successfully initialized with WatchlistDAO.");
    }

    public void notifyWatchers(Auction auction, String highestBidderId) {
        if (watchlistDAO == null) return;
 
        String auctionId   = auction.getAuctionConfig().getId();
        String auctionName = auction.getAuctionConfig().getName();
        long   currentPrice = auction.getCurrentPrice();
 
        List<String> watcherIdList = watchlistDAO.findUserIdsByAuction(auctionId);
 
        for (String watcherId : watcherIdList) {
            // Skip the current highest bidder
            if (watcherId.equals(highestBidderId)) continue;
 
            // Skip users currently in the bidding room as they receive updates via RoomObserver
            if (isInRoom(auctionId, watcherId)) continue;
 
            try {
                PrintWriter writer = SessionRegistry.getInstance().getWriter(watcherId);
                if (writer == null) { 
                    logger.log(Level.FINE, "Notification skipped: WatcherId " + watcherId + " is currently offline.");
                    continue;
                }
                pushOutbid(writer, auctionId, auctionName, currentPrice);
            } catch (Exception exception) { 
                logger.log(Level.WARNING, "[NotificationService] Failed to deliver outbid notification to watcherId " + watcherId + " for auctionId " + auctionId, exception);
            }
        }
    }

    public void notifyAuctionEnded(Auction auction) {
        if (watchlistDAO == null) return;
 
        String auctionId   = auction.getAuctionConfig().getId();
        String auctionName = auction.getAuctionConfig().getName();
        long   finalPrice  = auction.getCurrentPrice();
        String winnerName  = auction.getHighestBidderName() != null
                             ? auction.getHighestBidderName() : "No bids placed";
 
        List<String> watcherIdList = watchlistDAO.findUserIdsByAuction(auctionId);
 
        for (String watcherId : watcherIdList) {
            if (isInRoom(auctionId, watcherId)) continue; 
 
            try {
                PrintWriter writer = SessionRegistry.getInstance().getWriter(watcherId);
                if (writer == null) { 
                    logger.log(Level.FINE, "Notification skipped: WatcherId " + watcherId + " is currently offline.");
                    continue;
                }
                pushAuctionEnded(writer, auctionId, auctionName, finalPrice, winnerName);
            } catch (Exception exception) { 
                logger.log(Level.WARNING, "[NotificationService] Failed to deliver auction ended notification to watcherId " + watcherId + " for auctionId " + auctionId, exception);
            }
        }
    }

    // --- PRIVATE METHODS ---

    private void pushOutbid(PrintWriter writer, String auctionId,
                             String auctionName, long currentPrice) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId",    auctionId);
        payload.put("auctionName",  auctionName);
        payload.put("currentPrice", currentPrice);
        payload.put("type",         "OUTBID");
 
        try {
            synchronized (writer) {
                writer.println(JsonUtils.toJson(ClientMessage.push("OUTBID_NOTIFICATION", payload)));
            }
        } catch (Exception exception) {
            logger.log(Level.WARNING, "[NotificationService] Error transmitting outbid notification to client.", exception);
        }
    }
 
    private void pushAuctionEnded(PrintWriter writer, String auctionId,
                                   String auctionName, long finalPrice, String winnerName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId",   auctionId);
        payload.put("auctionName", auctionName);
        payload.put("finalPrice",  finalPrice);
        payload.put("winner",      winnerName);
        payload.put("type",        "ENDED");
 
        try {
            synchronized (writer) {
                writer.println(JsonUtils.toJson(ClientMessage.push("AUCTION_ENDED_NOTIFICATION", payload)));
            }
        } catch (Exception exception) {
            logger.log(Level.WARNING, "[NotificationService] Error transmitting auction ended notification to client.", exception);
        }
    }

    private boolean isInRoom(String auctionId, String watcherId) {
        Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
        if (auction == null) return false;
 
        return ChangeManager.getInstance().hasObserver(auction, watcherId);
    }

}

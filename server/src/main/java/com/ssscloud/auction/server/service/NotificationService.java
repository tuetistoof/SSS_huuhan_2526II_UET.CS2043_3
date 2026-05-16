/**
 * NotificationService handles push notifications to users outside of the bidding room (Watchlist users).
 */
package com.ssscloud.auction.server.service;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.NotificationDTO;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.server.dao.NotificationDAO;
import com.ssscloud.auction.server.dao.WatchlistDAO;
import com.ssscloud.auction.server.util.SessionRegistry;
import com.ssscloud.auction.server.util.AuctionRegistry;

public class NotificationService {
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName()); // Logging Standards: Declared first

    private static final NotificationService instance = new NotificationService();
    private WatchlistDAO watchlistDAO; // Dependency Injection: Short name
    private NotificationDAO notificationDAO;

    private NotificationService() {}

    public static NotificationService getInstance() { return instance; }

    public void init(WatchlistDAO watchlistDAO, NotificationDAO notificationDAO) {
        this.watchlistDAO    = watchlistDAO;
        this.notificationDAO = notificationDAO; 
        logger.log(Level.INFO, "[NotificationService] Initialized — watchlistDAO={0}, notificationDAO={1}",
                new Object[]{watchlistDAO != null ? "OK" : "NULL", notificationDAO != null ? "OK" : "NULL"});
    }

    public void notifyWatchers(Auction auction, String highestBidderId) throws ServiceException, Exception {
        try {
            if (watchlistDAO == null) {
                throw new ServiceException(ErrorCode.NOTIFICATION_SERVICE_NOT_INITIALIZED, "NotificationService failure: WatchlistDAO dependency is not initialized.");
            }
     
            String auctionId = auction.getAuctionConfig().getId(); 
            String auctionName = auction.getAuctionConfig().getName();
            long currentPrice = auction.getCurrentPrice();
     
            List<String> watcherIdList = watchlistDAO.findUserIdsByAuction(auctionId); 
     
            for (String watcherId : watcherIdList) {
                // Skip the current highest bidder
                if (watcherId.equals(highestBidderId)) continue;
     
                // Skip users currently in the bidding room as they receive updates via RoomObserver
                if (isInRoom(auctionId, watcherId)) continue;
     
                try {
                    PrintWriter writer = SessionRegistry.getInstance().getWriter(watcherId);
                    if (writer == null) { 
                        savePending(watcherId, "OUTBID", auctionId, auctionName, currentPrice, null);
                        logger.log(Level.FINE, "Notification skipped: WatcherId " + watcherId + " is currently offline.");
                        continue;
                    }
                    pushOutbid(writer, auctionId, auctionName, currentPrice);
                } catch (Exception deliveryException) { 
                    logger.log(Level.WARNING, "[NotificationService] Failed to deliver outbid notification to watcherId " + watcherId + " for auctionId " + auctionId, deliveryException);
                }
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            // Final safety net for system-level failures
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in NotificationService.notifyWatchers: " + exception.getMessage(), exception);
            throw new ServiceException(ErrorCode.NOTIFICATION_FAILED, "An unexpected system error occurred while notifying watchers.");
        }
    }

    public void notifyAuctionEnded(Auction auction) throws ServiceException, Exception {
        try {
            if (watchlistDAO == null) {
                throw new ServiceException(ErrorCode.NOTIFICATION_SERVICE_NOT_INITIALIZED, "NotificationService failure: WatchlistDAO dependency is not initialized.");
            }
     
            String auctionId = auction.getAuctionConfig().getId(); // Internal Logic: [Entity]Id
            String auctionName = auction.getAuctionConfig().getName();
            long finalPrice = auction.getCurrentPrice();
            String winnerName = auction.getHighestBidderName() != null
                                 ? auction.getHighestBidderName() : "No bids placed";
     
            List<String> watcherIdList = watchlistDAO.findUserIdsByAuction(auctionId); // DTOs: List suffix
     
            for (String watcherId : watcherIdList) {
                if (isInRoom(auctionId, watcherId)) continue; 
     
                try {
                    PrintWriter writer = SessionRegistry.getInstance().getWriter(watcherId);
                    if (writer == null) { 
                        savePending(watcherId, "ENDED", auctionId, auctionName, finalPrice, winnerName);
                        logger.log(Level.FINE, "Notification skipped: WatcherId " + watcherId + " is currently offline.");
                        continue;
                    }
                    pushAuctionEnded(writer, auctionId, auctionName, finalPrice, winnerName);
                } catch (Exception exception) { 
                    logger.log(Level.WARNING, "[NotificationService] Failed to deliver auction ended notification to watcherId " + watcherId + " for auctionId " + auctionId, exception); // Specific problem logging
                }
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            // Final safety net for system-level failures
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in NotificationService.notifyAuctionEnded: " + exception.getMessage(), exception);
            throw new ServiceException(ErrorCode.NOTIFICATION_FAILED, "An unexpected system error occurred during auction end notification.");
        }
    }

    // --- PRIVATE METHODS ---

    private void pushOutbid(PrintWriter writer, String auctionId,
                            String auctionName, long currentPrice) {
        Map<String, Object> jsonPayload = new HashMap<>(); // Internal Logic: jsonPayload
        jsonPayload.put("auctionId", auctionId);
        jsonPayload.put("auctionName", auctionName);
        jsonPayload.put("currentPrice", currentPrice);
        jsonPayload.put("type", "OUTBID");
 
        try {
            synchronized (writer) {
                writer.println(JsonUtils.toJson(ClientMessage.push("OUTBID_NOTIFICATION", jsonPayload)));
            }
        } catch (Exception exception) {
            logger.log(Level.WARNING, "[NotificationService] Error transmitting outbid notification to client.", exception);
        }
    }
 
    private void pushAuctionEnded(PrintWriter writer, String auctionId,
                                  String auctionName, long finalPrice, String winnerName) {
        Map<String, Object> jsonPayload = new HashMap<>(); // Internal Logic: jsonPayload
        jsonPayload.put("auctionId", auctionId);
        jsonPayload.put("auctionName", auctionName);
        jsonPayload.put("finalPrice", finalPrice);
        jsonPayload.put("winner", winnerName);
        jsonPayload.put("type", "ENDED");
 
        try {
            synchronized (writer) {
                System.out.println("Sending auction ended notification: " + jsonPayload);
                writer.println(JsonUtils.toJson(ClientMessage.push("AUCTION_ENDED_NOTIFICATION", jsonPayload)));
            }
        } catch (Exception exception) {
            logger.log(Level.WARNING, "[NotificationService] Error transmitting auction ended notification to client.", exception);
        }
    }
    private void savePending(String userId, String type, String auctionId,
                             String auctionName, long price, String winner) {
        if (notificationDAO == null) {
            logger.log(Level.WARNING, "[DEBUG] savePending SKIP — notificationDAO is NULL. userId={0} type={1}",
                    new Object[]{userId, type});
            return;
        }
        logger.log(Level.INFO, "[DEBUG] savePending userId={0} type={1} auction={2}",
                new Object[]{userId, type, auctionName});
        NotificationDTO dto = new NotificationDTO();
        dto.setUserId     (userId);
        dto.setType       (type);
        dto.setAuctionId  (auctionId);
        dto.setAuctionName(auctionName);
        dto.setPrice      (price);
        dto.setWinner     (winner);
        dto.setCreatedAt  (LocalDateTime.now());
        boolean saved = notificationDAO.save(dto);
        if (!saved) logger.log(Level.WARNING, "Không lưu được pending notification cho user " + userId);
    }

    private boolean isInRoom(String auctionId, String watcherId) {
        Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
        if (auction == null) return false;
        return ChangeManager.getInstance().hasObserver(auction, watcherId);
    }
}

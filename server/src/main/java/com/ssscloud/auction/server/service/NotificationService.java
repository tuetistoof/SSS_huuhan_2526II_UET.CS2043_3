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

import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.response.DTO.NotificationDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.model.auction.BidTransaction;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.server.dao.NotificationDAO;
import com.ssscloud.auction.server.dao.QueryDAO;
import com.ssscloud.auction.server.util.SessionRegistry;
import com.ssscloud.auction.server.util.AuctionRegistry;

public class NotificationService {
    // Logging Standards: Declared first as a privsate static final attribute
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName()); 

    private final QueryDAO queryDAO; // Dependency Injection: Short name
    private final NotificationDAO notificationDAO;

    // Concurrency: ReadWriteLock for notification operations
    public NotificationService(QueryDAO queryDAO, NotificationDAO notificationDAO) {
        this.queryDAO    = queryDAO;
        this.notificationDAO = notificationDAO; 
        logger.log(Level.INFO, "NotificationService initialized with dependencies: QueryDAO={0}, NotificationDAO={1}",
                new Object[]{queryDAO != null ? "READY" : "MISSING", notificationDAO != null ? "READY" : "MISSING"});
    }

    public void notifyWatchers(Auction auction, String highestBidderId) throws ServiceException, Exception {
        try {
            if (queryDAO == null) {
                throw new ServiceException(ErrorCode.NOTIFICATION_SERVICE_NOT_INITIALIZED, "NotificationService failure: QueryDAO dependency is not initialized.");
            }
     
            String auctionId = auction.getAuctionConfig().getId(); 
            String auctionName = auction.getAuctionConfig().getName();
            long currentPrice = auction.getCurrentPrice();
     
            List<String> watcherIdList = queryDAO.findUserIdsByAuction(auctionId); 
     
            for (String watcherId : watcherIdList) {
                // Skip the current highest bidder
                if (watcherId.equals(highestBidderId)) continue;
     
                // Skip users currently in the bidding room as they receive updates via RoomObserver
                if (isInRoom(auctionId, watcherId)) continue;
     
                try {
                    PrintWriter writer = SessionRegistry.getInstance().getWriter(watcherId);
                    if (writer == null) { 
                        savePendingNotification(watcherId, "OUTBID", auctionId, auctionName, currentPrice, null);
                        logger.log(Level.FINE, "Notification skipped: WatcherId " + watcherId + " is currently offline.");
                        continue;
                    }
                    pushOutbidNotification(writer, auctionId, auctionName, currentPrice);
                } catch (Exception deliveryException) { 
                    logger.log(Level.WARNING, "[NotificationService] Failed to deliver outbid notification to watcherId " + watcherId + " for auctionId " + auctionId, deliveryException);
                }
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in NotificationService.notifyWatchers", exception);
            throw exception;
        }
    }

    public void notifyAuctionEnded(Auction auction) throws ServiceException, Exception {
        try {
            if (queryDAO == null) {
                throw new ServiceException(ErrorCode.NOTIFICATION_SERVICE_NOT_INITIALIZED, "NotificationService failure: QueryDAO dependency is not initialized.");
            }
     
            String auctionId = auction.getAuctionConfig().getId(); // Internal Logic: [Entity]Id
            String auctionName = auction.getAuctionConfig().getName();
            BidTransaction lastBid = auction.getLastBidTransaction();
            long finalPrice = lastBid == null ? auction.getAuctionConfig().getStartPrice() : lastBid.getBidAmount();
            String winnerName = lastBid == null ? null : lastBid.getBidderUsername();
     
            List<String> watcherIdList = queryDAO.findUserIdsByAuction(auctionId); // DTOs: List suffix
     
            for (String watcherId : watcherIdList) {
                if (isInRoom(auctionId, watcherId)) continue; 
     
                try {
                    PrintWriter writer = SessionRegistry.getInstance().getWriter(watcherId);
                    if (writer == null) { 
                        savePendingNotification(watcherId, "ENDED", auctionId, auctionName, finalPrice, winnerName);
                        logger.log(Level.FINE, "Notification skipped: WatcherId " + watcherId + " is currently offline.");
                        continue;
                    }
                    pushAuctionEndedNotification(writer, auctionId, auctionName, finalPrice, winnerName);
                } catch (Exception exception) { 
                    logger.log(Level.WARNING, "Failed to deliver auction end notification to watcherId " + watcherId, exception);
                }
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in NotificationService.notifyAuctionEnded", exception);
            throw exception;
        }
    }

    // --- PRIVATE METHODS ---

    private void pushOutbidNotification(PrintWriter writer, String auctionId,
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
            logger.log(Level.WARNING, "Transmission failure: Unable to deliver outbid notification payload.", exception);
        }
    }
 
    private void pushAuctionEndedNotification(PrintWriter writer, String auctionId,
                                  String auctionName, long finalPrice, String winnerName) {
        Map<String, Object> jsonPayload = new HashMap<>(); // Internal Logic: jsonPayload
        jsonPayload.put("auctionId", auctionId);
        jsonPayload.put("auctionName", auctionName);
        jsonPayload.put("finalPrice", finalPrice);
        jsonPayload.put("winner", winnerName);
        jsonPayload.put("type", "ENDED");
 
        try {
            synchronized (writer) {
                writer.println(JsonUtils.toJson(ClientMessage.push("AUCTION_ENDED_NOTIFICATION", jsonPayload)));
            }
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Transmission failure: Unable to deliver auction end notification payload.", exception);
        }
    }

    private void savePendingNotification(String userId, String type, String auctionId,
                             String auctionName, long price, String winner) {
        try {
            NotificationDTO notificationDto = new NotificationDTO(); // DTO suffix
            notificationDto.setUserId(userId);
            notificationDto.setType(type);
            notificationDto.setAuctionId(auctionId);
            notificationDto.setAuctionName(auctionName);
            notificationDto.setPrice(price);
            notificationDto.setWinner(winner);
            notificationDto.setCreatedAt(LocalDateTime.now());
            notificationDAO.save(notificationDto);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Database failure: Unable to save pending notification for offline user: " + userId, exception);
        }
    }

    private boolean isInRoom(String auctionId, String watcherId) {
        Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
        if (auction == null) return false;
        return ChangeManager.getInstance().hasObserver(auction, watcherId);
    }
}

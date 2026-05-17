package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.NotificationDTO;
import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.NotificationDAO;
import com.ssscloud.auction.server.service.NotificationService;
import com.ssscloud.auction.server.util.AuctionRegistry;

import java.util.logging.Logger;
import java.util.List;
import java.util.logging.Level;

public class NotificationController {
    // Logging Standards: Declared first as a private static final attribute
    private static final Logger logger = Logger.getLogger(NotificationController.class.getName());
 
    private final NotificationService notificationService; // Dependency Injection: Short name
    private final NotificationDAO notificationDAO;         // Dependency Injection: Short name
 
    public NotificationController(NotificationService notificationService, NotificationDAO notificationDAO) {
        this.notificationService = notificationService;
        this.notificationDAO  = notificationDAO;
    }
 
    // --- PUBLIC METHODS ---

    public void notifyWatchers(String auctionId, String highestBidderId) throws ControllerException, Exception {
        try {
            validateAuctionId(auctionId);
            validateUserId(highestBidderId);
            
            Auction auction = resolveAuction(auctionId);
            logger.log(Level.INFO, "Triggering outbid notifications for auctionId: {0}", auctionId);
            
            notificationService.notifyWatchers(auction, highestBidderId);
        } catch (ControllerException | ServiceException e) {
            throw e;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error during watcher notification broadcast.", exception);
            throw exception;
        }
    }

    public void notifyAuctionEnded(String auctionId) throws ControllerException, Exception {
        try {
            validateAuctionId(auctionId);
            
            Auction auction = resolveAuction(auctionId);
            logger.log(Level.INFO, "Triggering auction-ended notifications for auctionId: {0}", auctionId);
            
            notificationService.notifyAuctionEnded(auction);
        } catch (ControllerException | ServiceException e) {
            throw e;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error during auction end notification broadcast.", exception);
            throw exception;
        }
    }

    public String getPendingNotifications(String userId) throws ControllerException, Exception {
        try {
            validateUserId(userId);
            
            List<NotificationDTO> pendingNotificationsList = notificationDAO.findUnreadByUserId(userId);
            logger.log(Level.INFO, "Successfully loaded {0} pending notification(s) for userId: {1}",
                    new Object[]{pendingNotificationsList.size(), userId});
                    if (!pendingNotificationsList.isEmpty()) {
            notificationDAO.markAllRead(userId);
        }
            
            return JsonUtils.toJson(ApiResponse.success(pendingNotificationsList, "Pending notifications retrieved successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (DAOException daoException) {
            throw new ControllerException(ErrorCode.NOTIFICATION_FETCH_FAILED, "Persistence failure while fetching pending notifications.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while retrieving pending notifications for userId: " + userId, exception);
            throw exception;
        }
    }

    // --- PRIVATE METHODS ---

    private Auction resolveAuction(String auctionId) {
        Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
        if (auction == null) {
            logger.log(Level.WARNING,
                "Auction reference not found in live registry for auctionId: {0}. It may have concluded recently.",
                auctionId);
            throw new ControllerException(ErrorCode.INVALID_AUCTION_ID,
                "Auction record not active in registry: " + auctionId);
        }
        return auction;
    }

    private void validateAuctionId(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new ControllerException(ErrorCode.INVALID_AUCTION_ID,
                "Auction identification is mandatory and cannot be null.");
        }
    }
 
    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ControllerException(ErrorCode.INVALID_DATA,
                "User identification is mandatory and cannot be null.");
        }
    }
}

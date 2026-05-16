package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.NotificationDTO;
import com.ssscloud.auction.common.exception.ControllerException;
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
    private static final Logger logger = Logger.getLogger(NotificationController.class.getName());
 
    private final NotificationService notificationService;
    private final NotificationDAO notificationDAO;
 
    public NotificationController(NotificationService notificationService, NotificationDAO notificationDAO) {
        this.notificationService = notificationService;
        this.notificationDAO  = notificationDAO;
    }
 
    public void notifyWatchers(String auctionId, String highestBidderId) throws ServiceException, Exception{
        validateAuctionId(auctionId);
        validateUserId(highestBidderId);
        Auction auction = resolveAuction(auctionId);
        logger.log(Level.INFO, "[NotificationController] Triggering watcher notifications for auctionId: {0}", auctionId);
        notificationService.notifyWatchers(auction, highestBidderId);
    }
    public void notifyAuctionEnded(String auctionId) throws ServiceException, Exception {
        validateAuctionId(auctionId);
 
        Auction auction = resolveAuction(auctionId);
        logger.log(Level.INFO, "[NotificationController] Triggering auction-ended notifications for auctionId: {0}", auctionId);
        notificationService.notifyAuctionEnded(auction);
    }

    public String getPendingNotifications(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ControllerException(ErrorCode.INVALID_DATA, "userId is required.");
        }
        List<NotificationDTO> pending = notificationDAO.findUnreadByUserId(userId);
        logger.log(Level.INFO,"[NotificationController] Loaded {0} pending notifications for userId: {1}",
        new Object[]{pending.size(), userId});
        return JsonUtils.toJson(ApiResponse.success(pending, "Pending notifications loaded."));
}



    // HELPERS
    private Auction resolveAuction(String auctionId) {
        Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
        if (auction == null) {
            // Auction đã bị remove khỏi registry (vừa FINISHED) — log warning, không throw
            // vì notifyAuctionEnded được gọi đúng lúc auction bị remove
            logger.log(Level.WARNING,
                "[NotificationController] Auction not found in registry for auctionId: {0} — may have just finished.",
                auctionId);
            throw new ControllerException(ErrorCode.INVALID_AUCTION_ID,
                "Auction not found in live registry: " + auctionId);
        }
        return auction;
    }
    private void validateAuctionId(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new ControllerException(ErrorCode.INVALID_AUCTION_ID,
                "The auction identifier is mandatory and cannot be null or blank.");
        }
    }
 
    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ControllerException(ErrorCode.INVALID_DATA,
                "The user identifier is mandatory and cannot be null or blank.");
        }
    }
}

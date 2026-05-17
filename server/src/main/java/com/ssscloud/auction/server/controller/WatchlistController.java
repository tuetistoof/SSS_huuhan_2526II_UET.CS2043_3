package com.ssscloud.auction.server.controller;
 
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.WatchlistDAO;
 
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
 
public class WatchlistController {
    private static final Logger logger = Logger.getLogger(WatchlistController.class.getName());
 
    private final WatchlistDAO watchlistDAO;
 
    public WatchlistController(WatchlistDAO watchlistDAO) {
        this.watchlistDAO = watchlistDAO;
    }

    public String follow(Object rawRequest, String userId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing follow auction request for userId: {0}", userId);
            String jsonPayload = JsonUtils.toJson(rawRequest).replace("\"", "").trim();
            String auctionId = jsonPayload;

            validateAuctionId(auctionId);
     
            boolean isAdded = watchlistDAO.add(auctionId, userId); 
            if (!isAdded) {
                throw new ControllerException(ErrorCode.AUCTION_ALREADY_IN_WATCHLIST, "The specified auction is already present in the user's watchlist.");
            }
            return JsonUtils.toJson(ApiResponse.success(null, "Auction successfully added to the watchlist."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while following auction.", exception);
            throw exception;
        }
    }

    public String unfollow(Object rawRequest, String userId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing unfollow auction request for userId: {0}", userId);
            String jsonPayload = JsonUtils.toJson(rawRequest).replace("\"", "").trim();
            String auctionId = jsonPayload;

            validateAuctionId(auctionId);

            boolean isRemoved = watchlistDAO.remove(auctionId, userId); 
            if (!isRemoved) {
                throw new ControllerException(ErrorCode.AUCTION_NOT_IN_WATCHLIST, "The specified auction was not found in the user's watchlist.");
            }
            return JsonUtils.toJson(ApiResponse.success(null, "Auction successfully removed from the watchlist."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while unfollowing auction.", exception);
            throw exception;
        }
    }

    public String checkFollowing(Object rawRequest, String userId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Checking watchlist status for userId: {0}", userId);
            String jsonPayload = JsonUtils.toJson(rawRequest).replace("\"", "").trim();
            String auctionId = jsonPayload;

            validateAuctionId(auctionId);
            boolean followingStatusResult = watchlistDAO.isFollowing(auctionId, userId); 
            return JsonUtils.toJson(ApiResponse.success(followingStatusResult, "Watchlist status check completed successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while checking watchlist status.", exception);
            throw exception;
        }
    }

    public String getWatchlist(String userId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Retrieving full watchlist for userId: {0}", userId);
            List<BidderDisplayDTO> watchlistDetailsList = watchlistDAO.findWatchlistDetailsByUser(userId);
            return JsonUtils.toJson(ApiResponse.success(new ListResponse<>(watchlistDetailsList), "User watchlist retrieved successfully. Total items: " + watchlistDetailsList.size()));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while retrieving user watchlist.", exception);
            throw exception;
        }
    }

    private void validateAuctionId(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new ControllerException(ErrorCode.INVALID_AUCTION_ID, "The auction identifier is mandatory and cannot be null or blank.");
        }
    }
}
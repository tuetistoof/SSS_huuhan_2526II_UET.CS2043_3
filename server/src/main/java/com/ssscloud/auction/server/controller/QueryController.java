package com.ssscloud.auction.server.controller;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.dto.response.SellerDisplayDTO;
import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.QueryDAO;

public class QueryController {
    private static final Logger logger = Logger.getLogger(QueryController.class.getName());

    private final QueryDAO queryDAO;
    public QueryController(QueryDAO queryDAO) {
        this.queryDAO = queryDAO;
    }
    

    // --- PUBLIC METHODS ---

    // Hiển thị bidded auctions
    public String getBiddedAuctionsList(String userId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing request to retrieve bidded auctions for userId: {0}", userId);
            List<BidderDisplayDTO> biddedAuctionsList = queryDAO.findBiddedAuctionsDetailsByUser(userId);
            return JsonUtils.toJson(ApiResponse.success(new ListResponse<>(biddedAuctionsList), "User's bidded auctions retrieved successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (DAOException daoException) {
            throw new ControllerException(ErrorCode.BIDDED_AUCTIONS_FETCH_FAILED, "Persistence error while retrieving user bidded auctions.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while retrieving bidded auctions for userId: " + userId, exception);
            throw exception;
        }
    }

     public String getMyAuctions(String sellerId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing request to retrieve seller auctions for sellerId: {0}", sellerId);
            validateGetMyAuctionsRequest(sellerId);
            
            List<SellerDisplayDTO> sellerAuctionsList = queryDAO.findSellerAuction(sellerId);
            logger.log(Level.INFO, "Successfully retrieved {0} auction(s) for sellerId: {1}", 
                       new Object[]{sellerAuctionsList.size(), sellerId});
            
            return JsonUtils.toJson(ApiResponse.success(new ListResponse<>(sellerAuctionsList), "Seller auctions retrieved successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (DAOException daoException) {
            throw new ControllerException(ErrorCode.SELLER_AUCTION_FETCH_FAILED, "Persistence error while fetching seller-specific auctions.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while retrieving seller auction list for sellerId: " + sellerId, exception);
            throw exception;
        }
    }

    public String getActiveAuctions() throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing request to retrieve active auction list for bidders.");
            
            List<BidderDisplayDTO> activeAuctionsList = queryDAO.findActiveAuctions();
            logger.log(Level.INFO, "Successfully retrieved {0} active auction(s).", activeAuctionsList.size());
            
            return JsonUtils.toJson(ApiResponse.success(new ListResponse<>(activeAuctionsList), "Active auctions retrieved successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (DAOException daoException) {
            throw new ControllerException(ErrorCode.ACTIVE_AUCTION_FETCH_FAILED, "Persistence error while retrieving active auctions.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while retrieving global active auction list.", exception);
            throw exception;
        }
    }

    public String getWatchlist(String userId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Retrieving full watchlist for userId: {0}", userId);
            List<BidderDisplayDTO> watchlistDetailsList = queryDAO.findWatchlistDetailsByUser(userId);
            return JsonUtils.toJson(ApiResponse.success(new ListResponse<>(watchlistDetailsList), "User watchlist retrieved successfully. Total items: " + watchlistDetailsList.size()));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while retrieving user watchlist.", exception);
            throw exception;
        }
    }

    public String follow(Object rawRequest, String userId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing follow auction request for userId: {0}", userId);
            String jsonPayload = JsonUtils.toJson(rawRequest).replace("\"", "").trim();
            String auctionId = jsonPayload;

            validateAuctionId(auctionId);

            boolean isAdded = queryDAO.add(auctionId, userId); 
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

            boolean isRemoved = queryDAO.remove(auctionId, userId); 
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
            boolean followingStatusResult = queryDAO.isFollowing(auctionId, userId); 
            return JsonUtils.toJson(ApiResponse.success(followingStatusResult, "Watchlist status check completed successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while checking watchlist status.", exception);
            throw exception;
        }
    }

    // --- PRIVATE METHODS ---

    private void validateGetMyAuctionsRequest(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new ControllerException(ErrorCode.INVALID_DATA, "The seller identifier is mandatory to retrieve auctions.");
        }
    }

    private void validateAuctionId(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new ControllerException(ErrorCode.INVALID_AUCTION_ID, "The auction identifier is mandatory and cannot be null or blank.");
        }
    }
}

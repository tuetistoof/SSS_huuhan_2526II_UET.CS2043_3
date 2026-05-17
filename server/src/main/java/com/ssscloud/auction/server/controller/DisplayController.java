package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.dto.response.SellerDisplayDTO;
import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.DisplayDAO;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

public class DisplayController {
    // Logging Standards: Declared first as a private static final attribute
    private static final Logger logger = Logger.getLogger(DisplayController.class.getName());

    private final DisplayDAO displayDAO; // Dependency Injection: Short name

    public DisplayController(DisplayDAO displayDAO) {
        this.displayDAO = displayDAO;
    }

    // --- PUBLIC METHODS ---

    // Hiển thị bidded auctions
    public String getBiddedAuctionsList(String userId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing request to retrieve bidded auctions for userId: {0}", userId);
            List<BidderDisplayDTO> biddedAuctionsList = displayDAO.findBiddedAuctionsDetailsByUser(userId);
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
            
            List<SellerDisplayDTO> sellerAuctionsList = displayDAO.findSellerAuction(sellerId);
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
            
            List<BidderDisplayDTO> activeAuctionsList = displayDAO.findActiveAuctions();
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

    // --- PRIVATE METHODS ---

    private void validateGetMyAuctionsRequest(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new ControllerException(ErrorCode.INVALID_DATA, "The seller identifier is mandatory to retrieve auctions.");
        }
    }
}

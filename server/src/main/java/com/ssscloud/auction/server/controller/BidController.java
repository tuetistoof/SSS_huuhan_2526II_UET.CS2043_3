package com.ssscloud.auction.server.controller;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.service.AutoBidService;
import com.ssscloud.auction.server.service.BidService;

public class BidController {
    private static final Logger logger = Logger.getLogger(BidController.class.getName());
    
    private final BidTransactionDAO bidTransactionDAO;
    private final BidService bidService;
    private final AutoBidService autoBidService;
    private final AuctionDAO auctionDAO;
    
    public BidController(BidService bidService, AutoBidService autoBidService, BidTransactionDAO bidTransactionDAO, AuctionDAO auctionDAO) {
        this.bidService = bidService;
        this.autoBidService = autoBidService;
        this.bidTransactionDAO = bidTransactionDAO;
        this.auctionDAO = auctionDAO;
    }

    public String placeBid(Object rawRequest, String bidderId, String bidderUsername) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing manual bid placement for bidderId: {0}, username: {1}", new Object[]{bidderId, bidderUsername});
            String jsonPayload = JsonUtils.toJson(rawRequest);
            PlaceBidRequest placeBidRequest = JsonUtils.fromJson(jsonPayload, PlaceBidRequest.class);

            validatePlaceBidRequest(placeBidRequest);

            bidService.placeBid(placeBidRequest, bidderId, bidderUsername);
            return JsonUtils.toJson(ApiResponse.success(null, "Bid has been placed successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected critical failure during manual bid placement.", exception);
            throw exception;
        }
    }

    public String registerAutoBid(Object rawRequest, String bidderId, String bidderUsername) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing auto-bid registration for bidderId: {0}, username: {1}", new Object[]{bidderId, bidderUsername});
            String jsonPayload = JsonUtils.toJson(rawRequest);
            AutoBidRequest autoBidRequest = JsonUtils.fromJson(jsonPayload, AutoBidRequest.class);

            validateAutoBidRequest(autoBidRequest);

            autoBidService.register(autoBidRequest, bidderId, bidderUsername);
            return JsonUtils.toJson(ApiResponse.success(null, "Auto-bid configuration has been registered successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected critical failure during auto-bid registration.", exception);
            throw exception;
        }
    }

    public String getBidHistory(Object rawRequest) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Retrieving bid history for the specified auction.");
            String jsonPayload = JsonUtils.toJson(rawRequest).replace("\"", "").trim();
            validateBidHistoryRequest(jsonPayload);

            List<BidTransaction> transactionList = bidTransactionDAO.findByAuctionId(jsonPayload);
            List<BidDTO> bidHistoryList = transactionList.stream().map(transaction -> {
                BidDTO bidDto = new BidDTO();
                bidDto.setAuctionId(transaction.getAuctionId());
                bidDto.setBidderUsername(transaction.getBidderUsername());
                bidDto.setBidAmount(transaction.getBidAmount());
                bidDto.setBidTime(transaction.getBidTime());
                bidDto.setBidType(transaction.getType().name());
                return bidDto;
            }).toList();

            return JsonUtils.toJson(ApiResponse.success(bidHistoryList, "Bid history retrieved successfully for auctionId: " + jsonPayload));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected critical failure while retrieving bid history.", exception);
            throw exception;
        }
    }

    public String getAutoBidStatus(Object rawRequest, String bidderId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Retrieving user bid status for the specified auction.");
            String jsonPayload = JsonUtils.toJson(rawRequest).replace("\"", "").trim();
            
            List<AutoBidService.AutoBidEntry> entries = autoBidService.getRegistrations(jsonPayload);
            boolean isActive = entries.stream().anyMatch(e -> e.bidderId.equals(bidderId));

            return JsonUtils.toJson(ApiResponse.success(
                java.util.Map.of("active", isActive),
                "Auto-bid status retrieved successfully."
            ));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected failure while retrieving auto-bid status.", exception);
            throw exception;
        }
    }

    private void validatePlaceBidRequest(PlaceBidRequest placeBidRequest) throws ControllerException {
        try {
            if (placeBidRequest == null) {
                    throw new ControllerException(ErrorCode.INVALID_BID_REQUEST, "The manual bid request payload cannot be null.");
            }
            if (placeBidRequest.getAuctionId() == null || placeBidRequest.getAuctionId().isBlank()) {
                throw new ControllerException(ErrorCode.MISSING_AUCTION_ID, "The auctionId is required.");
            }
            if (placeBidRequest.getBidAmount() <= 0) {
                throw new ControllerException(ErrorCode.INVALID_BID_AMOUNT, "The bid amount must be a positive value greater than zero.");
            }
            if (!auctionDAO.findByAuctionId(placeBidRequest.getAuctionId()).getStatus().isActive()) {
                throw new ControllerException(ErrorCode.INVALID_AUCTION_ID, "The specified auction is not active. Bids can only be placed on active auctions.");
            }
        } catch (Exception e) {
            throw new ControllerException(ErrorCode.INVALID_BID_REQUEST, "Manual bid request validation failed: " + e.getMessage(), e);
        }
    }

    private void validateAutoBidRequest(AutoBidRequest autoBidRequest) throws ControllerException {
        try {
            // FIX #4: Kiểm tra null/blank TRƯỚC khi query DB để tránh NullPointerException
            if (autoBidRequest == null) {
                throw new ControllerException(ErrorCode.INVALID_BID_REQUEST, "The auto-bid request payload cannot be null.");
            }
            if (autoBidRequest.getAuctionId() == null || autoBidRequest.getAuctionId().isBlank()) {
                throw new ControllerException(ErrorCode.MISSING_AUCTION_ID, "The auctionId is required for auto-bid registration.");
            }
            // Chỉ query DB sau khi đã chắc chắn auctionId không null
            if (!auctionDAO.findByAuctionId(autoBidRequest.getAuctionId()).getStatus().isActive()) {
                throw new ControllerException(ErrorCode.INVALID_AUCTION_ID, "The specified auction is not active.");
            }
            if (autoBidRequest.getMaxBid() <= 0) {
                throw new ControllerException(ErrorCode.INVALID_BID_AMOUNT, "The maximum bid threshold must be greater than zero.");
            }
            if (autoBidRequest.getIncrement() <= 0) {
                throw new ControllerException(ErrorCode.INVALID_INCREMENT, "The bid increment value must be greater than zero.");
            }
        } catch (Exception e) {
            throw new ControllerException(ErrorCode.INVALID_BID_REQUEST, "Auto-bid request validation failed: " + e.getMessage(), e);
        }
    }

    private void validateBidHistoryRequest(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new ControllerException(ErrorCode.MISSING_AUCTION_ID, "The auctionId is required to retrieve bid history.");
        }
    }
}
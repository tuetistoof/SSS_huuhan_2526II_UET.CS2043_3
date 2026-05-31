package com.ssscloud.auction.server.controller;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.payload.request.CreateAuctionRequest;
import com.ssscloud.auction.common.payload.request.GetAuctionDetailsRequest;
import com.ssscloud.auction.common.payload.response.DTO.AuctionDTO;
import com.ssscloud.auction.common.payload.response.request.ApiResponse;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.service.AuctionService;
/**
 * AuctionController xử lý các request từ client liên quan đến auction:
 *   - Tạo auction mới (chỉ dành cho seller)
 *  - Lấy thông tin chi tiết của một auction cụ thể (dành cho tất cả user)
 * - Đảm bảo auction đang xem đã được load vào bộ nhớ (đăng ký vào registry) để nhận update real-time (dành cho tất cả user)
 */

public class AuctionController {
    private static final Logger logger = Logger.getLogger(AuctionController.class.getName());

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public String createAuction(Object rawRequest, String sellerId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing auction creation for sellerId: {0}", sellerId);
            
            String jsonPayload = JsonUtils.toJson(rawRequest);
            CreateAuctionRequest createAuctionRequest = JsonUtils.fromJson(jsonPayload, CreateAuctionRequest.class);

            validateCreateAuctionRequest(createAuctionRequest);

            AuctionDTO auctionDto = auctionService.createAuction(createAuctionRequest, sellerId);

            return JsonUtils.toJson(ApiResponse.success(auctionDto, "Auction created successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error during auction creation.", exception);
            throw exception;
        }
    }

    public String getAuctionById(Object rawRequest) throws ControllerException, Exception {
        try {
            logger.info("Retrieving specific auction details by ID.");
            
            String jsonPayload = JsonUtils.toJson(rawRequest);
            GetAuctionDetailsRequest getAuctionDetailsRequest = JsonUtils.fromJson(jsonPayload, GetAuctionDetailsRequest.class);
            String auctionId = (getAuctionDetailsRequest != null) ? getAuctionDetailsRequest.getAuctionId() : null;

            if (auctionId == null || auctionId.isBlank()) {
                throw new ControllerException(ErrorCode.MISSING_AUCTION_ID, "AuctionId identifier is mandatory.");
            }

            AuctionDTO auctionDto = auctionService.getAuctionById(auctionId);
            return JsonUtils.toJson(ApiResponse.success(auctionDto, "Auction detailed information retrieved successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while retrieving auction details.", exception);
            throw exception;
        }
    }
    public void ensureLiveAuctionLoaded(String auctionId) throws ControllerException, Exception { //hàm này để đăng kí vào registry
        try {
            if (auctionId == null || auctionId.isBlank()) {
                throw new ControllerException(ErrorCode.MISSING_AUCTION_ID, "AuctionId identifier is mandatory.");
            }
            auctionService.ensureLiveAuctionLoaded(auctionId);
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error while loading live auction.", exception);
            throw exception;
        }
    }

    private void validateCreateAuctionRequest(CreateAuctionRequest createAuctionRequest) {
        if (createAuctionRequest == null) {
            throw new ControllerException(ErrorCode.INVALID_DATA, "CreateAuctionRequest payload is null.");
        }
        if (createAuctionRequest.getName() == null || createAuctionRequest.getName().isBlank()) {
            throw new ControllerException(ErrorCode.INVALID_DATA, "Auction name is required.");
        }
        if (createAuctionRequest.getStartPrice() <= 0) {
            throw new ControllerException(ErrorCode.INVALID_BID_AMOUNT, "Initial starting price must be greater than zero.");
        }
        if (createAuctionRequest.getItemData() == null) {
            throw new ControllerException(ErrorCode.INVALID_ITEM_DATA, "Associated item data is mandatory.");
        }
        if (createAuctionRequest.getEndTime() == null) {
            throw new ControllerException(ErrorCode.INVALID_DATA, "Auction termination time (endTime) must be specified.");
        }
        if (createAuctionRequest.getStartTime() != null && createAuctionRequest.getEndTime().isBefore(createAuctionRequest.getStartTime())) {
            throw new ControllerException(ErrorCode.INVALID_DATA, "Termination time must occur after the scheduled start time.");
        }
    }
}

    

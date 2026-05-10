package com.ssscloud.auction.server.controller;

import java.util.List;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.service.AuctionService;
/**
 * auction controller xử lí các request liên quan đến auction: getAllAuction, createAuction
 */
public class AuctionController {
    private final AuctionService auctionService;
    public AuctionController (AuctionService auctionService){
        this.auctionService = auctionService;
    }

    // public String createAuction(Object data, String sellerId){
    public String createAuction(String rawDataJson, String sellerId) {
        try {
            // String json = JsonUtils.toJson(rawDataJson);
            CreateAuctionRequest request = JsonUtils.fromJson(rawDataJson, CreateAuctionRequest.class);
            // Validate
            if (request == null) {
                return JsonUtils.toJson(ApiResponse.error("Dữ liệu request không hợp lệ"));
            }
            if (request.getName() == null || request.getName().isBlank()) {
                return JsonUtils.toJson(ApiResponse.error("Tên phiên đấu giá không được để trống"));
            }
            if (request.getStartPrice() <= 0) {
                return JsonUtils.toJson(ApiResponse.error("Giá khởi điểm phải lớn hơn 0"));
            }
            if (request.getItemData() == null) {
                return JsonUtils.toJson(ApiResponse.error("Thông tin sản phẩm (itemData) không được để trống"));
            }
            if (request.getEndTime() == null) {
                return JsonUtils.toJson(ApiResponse.error("Thời gian kết thúc không được để trống"));
            }
            
            AuctionDTO result = auctionService.createAuction(request, sellerId);
            
            if (result == null) {
                return JsonUtils.toJson(ApiResponse.error("Không thể tạo phiên đấu giá. Kiểm tra log server."));
            }
 
            return JsonUtils.toJson(ApiResponse.success(result, "Tạo phiên đấu giá thành công: " + result.getName()));
        } catch (Exception e) {
                e.printStackTrace();
                return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
    
    // /**
    //  * Trả về các auction đang active (OPEN / RUNNING).
    //  */
    
    public String getActiveAuctions() {
        try {
            List<AuctionDisplayInfoDTO> auctions = auctionService.getActiveAuctions();
            ListResponse <AuctionDisplayInfoDTO> response = new ListResponse<>(auctions);
            return JsonUtils.toJson(ApiResponse.success(response,
                    "Lấy danh sách thành công (" + auctions.size() + " phiên đang mở)"));
        } catch (Exception e) {
            e.printStackTrace();
            return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
    public String getMyAuctions(String sellerId) {
        try {
            if (sellerId == null || sellerId.isBlank()) {
                return JsonUtils.toJson(ApiResponse.error("Thiếu sellerId"));
            }
            List<AuctionDisplayInfoDTO> auctions = auctionService.getMyAuctions(sellerId);
            ListResponse <AuctionDisplayInfoDTO> response = new ListResponse<>(auctions);
            return JsonUtils.toJson(ApiResponse.success(response,
                    "Lấy danh sách thành công (" + auctions.size() + " phiên)"));
        } catch (Exception e) {
            e.printStackTrace();
            return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
    
    public String getAuctionById(Object data) {
        try {
            String raw = JsonUtils.toJson(data);
            String auctionId = JsonUtils.fromJson(raw, String.class);
            if (auctionId == null || auctionId.isBlank()) {
                return JsonUtils.toJson(ApiResponse.error("Thiếu auctionId"));
            }
            AuctionDTO response = auctionService.getAuctionById(auctionId);
            return JsonUtils.toJson(ApiResponse.success(response,
                    "Lấy auction thành công "));
        } catch (InvalidBidException e) {
            return JsonUtils.toJson(ApiResponse.error(e.getMessage()));
        }
        catch (Exception e) {
            e.printStackTrace();
            return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}

    

package com.ssscloud.auction.server.controller;

import java.util.List;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionListResponse;
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

//TO_DOS: sau này sẽ bổ sung chức năng lấy danh sách phiên đấu giá

    // public String getAllAuctions() {
    //     try {
    //         List<AuctionDTO> auctions = auctionService.getAllAuctions();
    //         AuctionListResponse response = new AuctionListResponse(auctions);
    //         return JsonUtils.toJson(ApiResponse.success(response,
    //                 "Lấy danh sách thành công (" + auctions.size() + " phiên)"));
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
    //     }
    // }
 
    // /**
    //  * Trả về các auction đang active (OPEN / RUNNING).
    //  */
    // public String getActiveAuctions() {
    //     try {
    //         List<AuctionDTO> auctions = auctionService.getActiveAuctions();
    //         AuctionListResponse response = new AuctionListResponse(auctions);
    //         return JsonUtils.toJson(ApiResponse.success(response,
    //                 "Lấy danh sách thành công (" + auctions.size() + " phiên đang mở)"));
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
    //     }
    // }

}

    

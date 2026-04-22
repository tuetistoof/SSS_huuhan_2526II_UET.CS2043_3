package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.service.AuctionService;
import com.ssscloud.auction.server.service.BidService;

public class AuctionController {
    private final AuctionService auctionService;
    public AuctionController (AuctionService auctionService)
    {
        this.auctionService = auctionService;
    }

    public String createAuction(Object data, String sellerId){
        try {
            String json = JsonUtils.toJson(data);
            CreateAuctionRequest request = JsonUtils.fromJson(json, CreateAuctionRequest.class);
            // Validate tối thiểu
            if (request == null) {
                return JsonUtils.toJson(ApiResponse.error("Dữ liệu request không hợp lệ"));
            }
            if (request.getName() == null || request.getName().isBlank()) {
                return JsonUtils.toJson(ApiResponse.error("Tên phiên đấu giá không được để trống"));
            }
            if (request.getStartPrice() <= 0) {
                return JsonUtils.toJson(ApiResponse.error("Giá khởi điểm phải lớn hơn 0"));
            }

            
            AuctionDTO result = auctionService.createAuction(request, sellerId);
            return JsonUtils.toJson(ApiResponse.success(result,
                    "Tạo phiên đấu giá thành công: " + result.getName()));
        } catch (Exception e) {
            e.printStackTrace();
            return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    
}

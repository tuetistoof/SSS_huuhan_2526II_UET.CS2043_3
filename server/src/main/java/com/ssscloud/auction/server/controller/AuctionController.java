package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.service.AuctionService;
import com.ssscloud.auction.server.service.BidService;

public class AuctionController {
    private final AuctionService auctionService = new AuctionService();

    public String createAuction(Object data){
        try {
            String json = JsonUtils.toJson(data);
            CreateAuctionRequest request = JsonUtils.fromJson(json, CreateAuctionRequest.class);
            // Validate tối thiểu
            if (request == null) {
                return JsonUtils.toJson(ApiResponse.error("Dữ liệu request không hợp lệ"));
            }
            if (request.getTitle() == null || request.getTitle().isBlank()) {
                return JsonUtils.toJson(ApiResponse.error("Tên phiên đấu giá không được để trống"));
            }
            if (request.getStartingPrice() <= 0) {
                return JsonUtils.toJson(ApiResponse.error("Giá khởi điểm phải lớn hơn 0"));
            }
            if (request.getSellerId() == null || request.getSellerId().isBlank()) {
                return JsonUtils.toJson(ApiResponse.error("Thiếu sellerId — vui lòng đăng nhập lại"));
            }

            AuctionDTO result = auctionService.createAuction(request);
            return JsonUtils.toJson(ApiResponse.success(result,
                    "Tạo phiên đấu giá thành công: " + result.getName()));
        } catch (Exception e) {
            e.printStackTrace();
            return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    
}

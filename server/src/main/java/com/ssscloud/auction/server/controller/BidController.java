package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.service.AutoBidService;
import com.ssscloud.auction.server.service.BidService;

public class BidController {
    private final BidService bidService;
    // private final AutoBidService autoBidService = new AutoBidService();
    public BidController (BidService bidService)
    {
        this.bidService = bidService;
    }
    // trong messageHandler chuyển msg thô, chưa xử lí
    public String placeBid(Object data, String bidderId, String bidderUsername) {
        try {
            String raw = JsonUtils.toJson(data);
            PlaceBidRequest req = JsonUtils.fromJson(raw, PlaceBidRequest.class);

            if (req == null) {
                return JsonUtils.toJson(ApiResponse.error("Dữ liệu đặt giá không hợp lệ"));
            }
            // validate cơ bản
            if (req.getAuctionId() == null || req.getAuctionId().isBlank()) {
                return JsonUtils.toJson(ApiResponse.error("Thiếu auctionId"));
            }
            if (bidderId == null || bidderId.isBlank()) {
                return JsonUtils.toJson(ApiResponse.error("Thiếu bidderId"));
            }
            if (BidValidator.isPositiveBid(req.getBidAmount())) {
                return JsonUtils.toJson(ApiResponse.error("Số tiền đặt phải lớn hơn 0"));
            }

            BidDTO result = bidService.placeBid(req, bidderId, bidderUsername);
            return JsonUtils.toJson(ApiResponse.success(result, "Đặt giá thành công"));

        } catch (Exception e) {
            System.err.println("[BidController] Lỗi placeBid: " + e.getMessage());
            return JsonUtils.toJson(ApiResponse.error("Lỗi hệ thống khi đặt giá"));
        }
    }

    // public String registerAutoBid(Object data, String bidderId, String bidderUsername) {
    //     try {
    //         String raw = JsonUtils.toJson(data);
    //         AutoBidRequest req = JsonUtils.fromJson(raw, AutoBidRequest.class);

    //         if (req == null) {
    //             return JsonUtils.toJson(ApiResponse.error("Dữ liệu đặt giá không hợp lệ"));
    //         }
    //         // validate cơ bản
    //         if (req.getAuctionId() == null || req.getAuctionId().isBlank()) {
    //             return JsonUtils.toJson(ApiResponse.error("Thiếu auctionId"));
    //         }
    //         if (bidderId == null || bidderId.isBlank()) {
    //             return JsonUtils.toJson(ApiResponse.error("Thiếu bidderId"));
    //         }
    //         if (BidValidator.isPositiveBid(req.getMaxBid())) {
    //             return JsonUtils.toJson(ApiResponse.error("Số tiền đặt phải lớn hơn 0"));
    //         }

    //         BidDTO result = 
            
    //         return JsonUtils.toJson(ApiResponse.success(result, "Đặt giá thành công"));

    //     } catch (Exception e) {
    //         System.err.println("[BidController] Lỗi placeBid: " + e.getMessage());
    //         return JsonUtils.toJson(ApiResponse.error("Lỗi hệ thống khi đặt giá"));
    //     }
    // }

}

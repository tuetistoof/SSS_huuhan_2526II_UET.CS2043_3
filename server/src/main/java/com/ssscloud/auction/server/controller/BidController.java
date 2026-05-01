package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.service.AutoBidService;
import com.ssscloud.auction.server.service.BidService;

public class BidController {
    private final BidService bidService;
    private final AutoBidService autoBidService;

    public BidController(BidService bidService, AutoBidService autoBidService) {
        this.bidService = bidService;
        this.autoBidService = autoBidService;
    }

    public String placeBid(Object data, String bidderId, String bidderUsername) {
        try {
            String raw = JsonUtils.toJson(data);
            PlaceBidRequest req = JsonUtils.fromJson(raw, PlaceBidRequest.class);

            if (req == null)
                return JsonUtils.toJson(ApiResponse.error("Dữ liệu đặt giá không hợp lệ"));

            bidService.placeBid(req, bidderId, bidderUsername);
            return null; // submit vào queue thành công — không cần response

        } catch (InvalidBidException e) {
            return JsonUtils.toJson(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("[BidController] Lỗi: " + e.getMessage());
            return JsonUtils.toJson(ApiResponse.error("Lỗi hệ thống khi đặt giá"));
        }
    }

    public String registerAutoBid(Object data, String bidderId, String bidderUsername) {
        try {
            String raw = JsonUtils.toJson(data);
            AutoBidRequest req = JsonUtils.fromJson(raw, AutoBidRequest.class);

            if (req == null)
                return JsonUtils.toJson(ApiResponse.error("Dữ liệu đặt giá không hợp lệ"));

            autoBidService.register(req, bidderId, bidderUsername);
            return null; // submit vào queue thành công — không cần response

        } catch (IllegalArgumentException e) {
            return JsonUtils.toJson(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("[BidController] Lỗi: " + e.getMessage());
            return JsonUtils.toJson(ApiResponse.error("Lỗi hệ thống khi đặt giá"));
        }
    }

}

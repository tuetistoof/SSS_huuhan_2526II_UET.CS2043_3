package com.ssscloud.auction.server.controller;

import java.util.List;

import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.service.AutoBidService;
import com.ssscloud.auction.server.service.BidService;

public class BidController {
    private final BidTransactionDAO bidTransactionDAO;
    private final BidService bidService;
    private final AutoBidService autoBidService;

    public BidController(BidService bidService, AutoBidService autoBidService, BidTransactionDAO bidTransactionDAO) {
        this.bidService = bidService;
        this.autoBidService = autoBidService;
        this.bidTransactionDAO = bidTransactionDAO;
    }

    public String placeBid(Object data, String bidderId, String bidderUsername) {
        try {
            String raw = JsonUtils.toJson(data);
            PlaceBidRequest req = JsonUtils.fromJson(raw, PlaceBidRequest.class);

            if (req == null)
                return JsonUtils.toJson(ApiResponse.error("Dữ liệu đặt giá không hợp lệ"));

            bidService.placeBid(req, bidderId, bidderUsername);
            return JsonUtils.toJson(ApiResponse.success(null, "Đặt giá thành công"));

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
            return JsonUtils.toJson(ApiResponse.success(null, "Đăng ký auto bid thành công"));

        } catch (IllegalArgumentException e) {
            return JsonUtils.toJson(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("[BidController] Lỗi: " + e.getMessage());
            return JsonUtils.toJson(ApiResponse.error("Lỗi hệ thống khi đăng ký auto bid"));
        }
    }

    public String getBidHistory(Object data) {
        try {
            String auctionId = JsonUtils.toJson(data).replace("\"", "").trim();
            if (auctionId == null || auctionId.isBlank()) {
                return JsonUtils.toJson(ApiResponse.error("Thiếu auctionId"));
            }
            List<BidTransaction> txList = bidTransactionDAO.findByAuctionId(auctionId);
            List<BidDTO> bidHistory = txList.stream().map(tx -> {
                BidDTO dto = new BidDTO();
                dto.setAuctionId(tx.getAuctionId());
                dto.setBidderUsername(tx.getBidderUsername());
                dto.setBidAmount(tx.getBidAmount());
                dto.setBidTime(tx.getBidTime());
                dto.setBidType(tx.getType().name());
                return dto;
            }).toList();

            return JsonUtils.toJson(ApiResponse.success(bidHistory, "Lịch sử đặt giá của auction " + auctionId));
        } catch (Exception e) {
            System.err.println("[BidController] Lỗi: " + e.getMessage());
            return JsonUtils.toJson(ApiResponse.error("Lỗi server khi lấy lịch sử đặt giá"));
        }
    }
}
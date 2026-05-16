package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.BiddedAuctionsListDAO;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

public class BiddedAuctionsListController {
    private static final Logger logger = Logger.getLogger(BiddedAuctionsListController.class.getName());

    private final BiddedAuctionsListDAO biddedAuctionsListDAO;

    public BiddedAuctionsListController(BiddedAuctionsListDAO biddedAuctionsListDAO) {
        this.biddedAuctionsListDAO = biddedAuctionsListDAO;
    }

    // Hiển thị bidded auctions
    public String getBiddedAuctionslist(String userId) throws ControllerException {
        logger.log(Level.INFO, "Retrieving full bidded auctions for userId: {0}", userId);
        List<AuctionDisplayInfoDTO> auctionIdList = biddedAuctionsListDAO.findBiddedAuctionsDetailsByUser(userId);
        return JsonUtils.toJson(ApiResponse.success(new ListResponse<>(auctionIdList), "User's bidded auctions retrieved successfully."));
    }
}
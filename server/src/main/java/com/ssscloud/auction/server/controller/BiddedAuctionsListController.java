package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.exception.ControllerExceptions;
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
    public String getBiddedAuctionslist(String userId) throws ControllerExceptions {
        logger.log(Level.INFO, "Retrieving full bidded auctions for userId: {0}", userId);
        List<String> auctionIdList = biddedAuctionsListDAO.findAuctionIdsByUser(userId);
        return JsonUtils.toJson(ApiResponse.success(auctionIdList, "User bidded auctions retrieved successfully."));
    }
}

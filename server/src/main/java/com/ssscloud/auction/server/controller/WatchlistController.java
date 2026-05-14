package com.ssscloud.auction.server.controller;
 
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.exception.ControllerExceptions;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.WatchlistDAO;
 
import java.util.List;
 
public class WatchlistController {
 
    private final WatchlistDAO watchlistDAO;
 
    public WatchlistController(WatchlistDAO watchlistDAO) {
        this.watchlistDAO = watchlistDAO;
    }

    public String follow(String auctionId, String userId) throws Exception {
        if (auctionId == null || auctionId.isBlank())
            throw new ControllerExceptions("INVALID_AUCTION_ID", "Auction ID cannot be null or blank");
 
        boolean added = watchlistDAO.add(auctionId, userId); 
        return added
            ? JsonUtils.toJson(ApiResponse.success(null, "Successfully added to Watchlist"))
            : JsonUtils.toJson(ApiResponse.error("You are already following this auction"));
    }

    public String unfollow(String auctionId, String userId) throws Exception {
        // Đã thay thế return error bằng throw Exception cho chuẩn form Validate
        if (auctionId == null || auctionId.isBlank())
            throw new ControllerExceptions("INVALID_AUCTION_ID", "Auction ID cannot be null or blank");

        boolean removed = watchlistDAO.remove(auctionId, userId); 
        return removed
            ? JsonUtils.toJson(ApiResponse.success(null, "Successfully removed from Watchlist"))
            : JsonUtils.toJson(ApiResponse.error("Auction not found in your Watchlist"));
    }

    public String checkFollowing(String auctionId, String userId) throws Exception {
        if (auctionId == null || auctionId.isBlank())
            throw new ControllerExceptions("INVALID_AUCTION_ID", "Auction ID cannot be null or blank");

        boolean following = watchlistDAO.isFollowing(auctionId, userId); 
        return JsonUtils.toJson(ApiResponse.success(following, "Success"));
    }

    public String getWatchlist(String userId) throws Exception {
        // Không cần try-catch nữa vì MessageHandler sẽ lo việc đó nếu DAO có lỗi sập Database
        List<String> auctionIds = watchlistDAO.findAuctionIdsByUser(userId);
        return JsonUtils.toJson(ApiResponse.success(auctionIds, "Success"));
    }
}
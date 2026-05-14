package com.ssscloud.auction.server.controller;
 
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.WatchlistDAO;
 
import java.util.List;
 
public class WatchlistController {
 
    private final WatchlistDAO watchlistDAO;
 
    public WatchlistController(WatchlistDAO watchlistDAO) {
        this.watchlistDAO = watchlistDAO;
    }

    public String follow(String auctionId, String userId) {
        try {
            if (auctionId == null || auctionId.isBlank())
                return JsonUtils.toJson(ApiResponse.error("Thiếu auctionId"));
 
            boolean added = watchlistDAO.add(auctionId, userId); 
            return added
                ? JsonUtils.toJson(ApiResponse.success(null, "Đã thêm vào Watch List"))
                : JsonUtils.toJson(ApiResponse.error("Bạn đã follow phiên này rồi"));
        } catch (Exception e) {
            return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
    public String unfollow(String auctionId, String userId) {
        try {
            if (auctionId == null || auctionId.isBlank())
                return JsonUtils.toJson(ApiResponse.error("Thiếu auctionId"));
 
            boolean removed = watchlistDAO.remove(auctionId, userId); // FIX: đúng thứ tự
            return removed
                ? JsonUtils.toJson(ApiResponse.success(null, "Đã xóa khỏi Watch List"))
                : JsonUtils.toJson(ApiResponse.error("Không tìm thấy trong Watch List"));
        } catch (Exception e) {
            return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
    public String checkFollowing(String auctionId, String userId) {
        try {
            boolean following = watchlistDAO.isFollowing(auctionId, userId); // FIX: đúng thứ tự
            return JsonUtils.toJson(ApiResponse.success(following, "OK"));
        } catch (Exception e) {
            return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
    public String getWatchlist(String userId) {
        try {
            List<AuctionDisplayInfoDTO> watchlistDetails = watchlistDAO.findWatchlistDetailsByUser(userId);
            return JsonUtils.toJson(ApiResponse.success(watchlistDetails, "OK"));
        } catch (Exception e) {
            return JsonUtils.toJson(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}

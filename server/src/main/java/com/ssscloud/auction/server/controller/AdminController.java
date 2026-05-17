package com.ssscloud.auction.server.controller;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.reflect.TypeToken;
import com.ssscloud.auction.common.dto.response.AdminAuctionView;
import com.ssscloud.auction.common.dto.response.AdminMetrics;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.service.AdminService;

/**
 * AdminController xử lý các request từ client dành riêng cho admin:
 *   - Lấy danh sách tất cả auction (có filter theo status tuỳ chọn)
 *   - Lấy số liệu thống kê cho metric cards
 *   - Xóa (cancel) một auction không hợp lệ kèm lý do bắt buộc
 */
public class AdminController {

    private static final Logger logger = Logger.getLogger(AdminController.class.getName());

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ─────────────────────────── PUBLIC METHODS ───────────────────────────

    /**
     * Trả về danh sách tất cả auction, có thể lọc theo status.
     *
     * Payload từ client: String tên status ("RUNNING", "OPEN", "FINISHED", "CANCELED")
     *                    hoặc null / "" để lấy tất cả.
     */
    public String getAuctions(Object rawFilter) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Admin requested auction list. Raw filter: {0}", rawFilter);

            AuctionStatus filter = parseStatusFilter(rawFilter);

            List<AdminAuctionView> auctions = adminService.getAuctions(filter);

            String filterLabel = (filter != null) ? filter.name() : "ALL";
            logger.log(Level.INFO, "Admin auction list returned {0} item(s), filter={1}",
                    new Object[]{auctions.size(), filterLabel});

            return JsonUtils.toJson(ApiResponse.success(auctions,
                    "Admin auction list retrieved successfully. Count: " + auctions.size()));

        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled error in AdminController.getAuctions", exception);
            throw exception;
        }
    }

    /**
     * Trả về 3 con số thống kê cho metric cards trên admin dashboard:
     * runningCount, endedCount, totalUsers.
     */
    public String getMetrics() throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Admin requested dashboard metrics.");

            AdminMetrics metrics = adminService.getMetrics();

            return JsonUtils.toJson(ApiResponse.success(metrics,
                    "Admin dashboard metrics retrieved successfully."));

        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled error in AdminController.getMetrics", exception);
            throw exception;
        }
    }

    /**
     * Xóa (cancel) một auction không hợp lệ.
     * Lý do xóa là bắt buộc — request sẽ bị từ chối nếu thiếu hoặc để trống.
     *
     * Payload từ client (JSON object):
     * {
     *   "auctionId": "abc-123",
     *   "reason":    "Sản phẩm vi phạm chính sách đấu giá"
     * }
     */
    public String cancelAuction(Object rawRequest) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Admin requested auction cancellation. Payload: {0}", rawRequest);

            // Deserialize payload thành Map để lấy 2 field
            // Dùng TypeToken để Gson giữ đúng kiểu String thay vì trả về LinkedTreeMap<String, Object>
            String json = JsonUtils.toJson(rawRequest);
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> payload = JsonUtils.fromJsonGeneric(json, mapType);

            if (payload == null) {
                throw new ControllerException(ErrorCode.INVALID_DATA,
                        "Cancel auction request payload must not be null.");
            }

            String auctionId = payload.get("auctionId");
            String reason    = payload.get("reason");

            // Validate auctionId
            if (auctionId == null || auctionId.isBlank()) {
                throw new ControllerException(ErrorCode.INVALID_AUCTION_ID,
                        "The auction identifier (auctionId) is required to cancel an auction.");
            }

            // Validate reason — bắt buộc phải có lý do trước khi xóa
            if (reason == null || reason.isBlank()) {
                throw new ControllerException(ErrorCode.INVALID_DATA,
                        "A cancellation reason is required. Please describe why this auction is being removed.");
            }

            adminService.cancelAuction(auctionId, reason.trim());

            logger.log(Level.INFO, "Auction [{0}] successfully canceled by admin. Reason: {1}",
                    new Object[]{auctionId, reason});

            return JsonUtils.toJson(ApiResponse.success(null,
                    "Auction [" + auctionId + "] has been canceled successfully."));

        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled error in AdminController.cancelAuction", exception);
            throw exception;
        }
    }

    // ─────────────────────────── PRIVATE HELPERS ──────────────────────────

    /**
     * Chuyển đổi filter string từ client thành AuctionStatus enum.
     * Trả về null nếu client không truyền filter (lấy tất cả).
     * Ném ControllerException nếu giá trị không hợp lệ.
     */
    private AuctionStatus parseStatusFilter(Object rawFilter) throws ControllerException {
        if (rawFilter == null) {
            return null;
        }

        String filterStr = rawFilter.toString().replace("\"", "").trim();

        if (filterStr.isEmpty()) {
            return null;
        }

        try {
            return AuctionStatus.valueOf(filterStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ControllerException(ErrorCode.INVALID_DATA,
                    "Invalid auction status filter: \"" + filterStr + "\". " +
                    "Accepted values: RUNNING, OPEN, FINISHED, CANCELED.");
        }
    }
}
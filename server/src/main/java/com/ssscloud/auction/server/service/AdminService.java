package com.ssscloud.auction.server.service;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.response.AdminDisplayDTO;
import com.ssscloud.auction.common.dto.response.AdminMetrics;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.AdminDAO;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;
import com.ssscloud.auction.server.util.SessionRegistry;

/**
 * AdminService handles business logic for admin operations:
 * viewing all auctions, retrieving dashboard metrics, and canceling auctions.
 */
public class AdminService {

    // Logging Standards: Declared first as a private static final attribute
    private static final Logger logger = Logger.getLogger(AdminService.class.getName());

    private final AdminDAO      adminDAO;
    private final AuctionDAO    auctionDAO;
    private final AutoBidService autoBidService;

    public AdminService(AdminDAO adminDAO, AuctionDAO auctionDAO, AutoBidService autoBidService) {
        this.adminDAO       = adminDAO;
        this.auctionDAO     = auctionDAO;
        this.autoBidService = autoBidService;
    }

    // --- PUBLIC METHODS ---

    /**
     * Lấy danh sách tất cả auction, có thể filter theo status.
     * filter == null thì lấy tất cả.
     */
    public List<AdminDisplayDTO> getAuctions(AuctionStatus filter) throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Admin retrieving auction list, filter: {0}",
                filter != null ? filter.name() : "ALL");

            return adminDAO.findAllAuctions(filter);

        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in AdminService.getAuctions", exception);
            throw exception;
        }
    }

    /**
     * Lấy 3 con số thống kê cho metric cards trên dashboard.
     */
    public AdminMetrics getMetrics() throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Admin retrieving dashboard metrics.");
            return adminDAO.getMetrics();

        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in AdminService.getMetrics", exception);
            throw exception;
        }
    }

    /**
     * Lấy danh sách tất cả user, có thể filter theo role (BIDDER / SELLER).
     * roleFilter == null thì lấy tất cả.
     */
    public List<UserDTO> getUsers(String roleFilter) throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Admin retrieving user list, roleFilter: {0}",
                roleFilter != null ? roleFilter : "ALL");
            return adminDAO.getAllUsers(roleFilter);
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in AdminService.getUsers", exception);
            throw exception;
        }
    }

    /**
     * Admin cancel một auction đang OPEN hoặc RUNNING.
     * Luồng: validate → update DB → xóa khỏi registry → clear auto-bid → broadcast lý do.
     */
    public void cancelAuction(String auctionId, String reason) throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Admin canceling auctionId: {0}, reason: {1}",
                new Object[]{auctionId, reason});

            // Validate input
            if (auctionId == null || auctionId.isBlank()) {
                throw new ServiceException(ErrorCode.INVALID_AUCTION_ID,
                    "The auction identifier is required to perform cancellation.");
            }

            // Lấy auction từ registry — chỉ cancel được auction đang active
            Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
            if (auction == null) {
                throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND,
                    "Auction not found or already ended: " + auctionId);
            }

            // Chỉ cancel được OPEN hoặc RUNNING
            if (!auction.getStatus().isActive()) {
                throw new ServiceException(ErrorCode.AUCTION_CLOSED,
                    "Cannot cancel auction that is not active: " + auctionId);
            }

            // 1. Cập nhật status trong DB
            auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELED);

            // 2. Cập nhật status trên object in-memory
            auction.cancel();

            // 3. Xóa khỏi AuctionRegistry
            AuctionRegistry.getInstance().remove(auctionId);

            // 4. Dọn auto-bid entries
            autoBidService.clearRegistrations(auctionId);

            // 5. Broadcast lý do cancel tới tất cả client đang trong phòng
            broadcastAuctionCanceled(auction, reason);

            // 6. Xóa tất cả observer của auction này khỏi ChangeManager
            ChangeManager.getInstance().detachByAdmin(auction);

            logger.log(Level.INFO, "Auction successfully canceled by admin: {0}", auctionId);

        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in AdminService.cancelAuction", exception);
            throw exception;
        }
    }

    // --- PRIVATE METHODS ---

    /**
     * Push thông báo AUCTION_CANCELED kèm lý do tới tất cả subscriber đang trong phòng.
     * Không dùng ChangeManager.notify() vì cần truyền thêm field reason — 
     * notify() chỉ gọi update() trên Observer mà không có context bổ sung.
     */
    private void broadcastAuctionCanceled(Auction auction, String reason) {
        String auctionId   = auction.getAuctionConfig().getId();
        String auctionName = auction.getAuctionConfig().getName();

        // Build payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId",   auctionId);
        payload.put("auctionName", auctionName);
        payload.put("reason",      reason != null ? reason : "");

        String message = JsonUtils.toJson(ClientMessage.push("AUCTION_CANCELED", payload));

        // Lấy tất cả user đang online và đang subscribe auction này
        // SessionRegistry lưu tất cả session, ChangeManager biết ai đang subscribe
        SessionRegistry.getInstance().getAllWriters().forEach((userId, writer) -> {
            if (ChangeManager.getInstance().hasObserver(auction, userId)) {
                try {
                    synchronized (writer) {
                        writer.println(message);
                    }
                } catch (Exception exception) {
                    logger.log(Level.WARNING,
                        "Failed to deliver AUCTION_CANCELED to userId: " + userId, exception);
                }
            }
        });
    }
}
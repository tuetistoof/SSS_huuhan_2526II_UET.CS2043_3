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
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.AdminDAO;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
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
    private final UserDAO       userDAO;

    public AdminService(AdminDAO adminDAO, AuctionDAO auctionDAO, AutoBidService autoBidService, UserDAO userDAO) {
        this.adminDAO       = adminDAO;
        this.auctionDAO     = auctionDAO;
        this.autoBidService = autoBidService;
        this.userDAO        = userDAO;
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
        logger.log(Level.INFO, "Admin canceling auctionId: {0}, reason: {1}", new Object[]{auctionId, reason});

        // 1. Validate (Giữ nguyên)
        if (auctionId == null || auctionId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_AUCTION_ID, "...");
        }

        Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
        if (auction == null) {
            throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "...");
        }

        // 2. Atomic Status Change
        synchronized (auction) {
            if (!auction.getStatus().isActive()) {
                throw new ServiceException("AUCTION_CLOSED", "Auction is already ended.");
            }
            auction.setStatus(AuctionStatus.CANCELED);
        }

        try {
            // 3. Lấy thông tin trước khi shutdown worker
            long lockAmount = ConcurrentBidManager.getInstance().getWinnerLockAmount(auctionId);
            
            // 4. Shutdown worker ĐỂ NGỪNG nhận bid mới ngay lập tức
            ConcurrentBidManager.getInstance().shutdown(auctionId);

            // 5. Cập nhật DB (Dùng Transaction nếu có thể)
            auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELED);
            
            // 6. Hoàn tiền
            refundWinner(auction, lockAmount);

            // 7. Dọn dẹp tài nguyên (Các tác vụ này ít rủi ro fail hơn)
            AuctionRegistry.getInstance().remove(auctionId);
            autoBidService.clearRegistrations(auctionId);
            broadcastAuctionCanceled(auction, reason);
            ChangeManager.getInstance().detachByAdmin(auction);

            logger.log(Level.INFO, "Auction successfully canceled: {0}", auctionId);

        } catch (Exception e) {
            // Xử lý rollback hoặc logging lỗi nghiêm trọng nếu bước 5-7 thất bại
            logger.log(Level.SEVERE, "CRITICAL: Auction canceled but cleanup failed for: " + auctionId, e);
            throw e;
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

    private void refundWinner(Auction auction, long lockAmount) throws Exception {
        String winnerId = auction.getHighestBidderId();
        long winningBid = lockAmount > 0 ? lockAmount : auction.getCurrentPrice();
        String sellerId = auction.getSellerId();

        if (winningBid == 0) winningBid = auction.getCurrentPrice();

        if (winnerId != null && winningBid > 0) {
            try {
                // Unlock bidder's balance
                userDAO.unlockBidderBalance(winnerId, winningBid);
                SessionRegistry.getInstance().addUnsettledBalance(winnerId, -winningBid);
                notifyUnsettledBalanceUpdate(winnerId, SessionRegistry.getInstance().getUnsettledBalance(winnerId));

                // Deduct seller's pending balance
                if (sellerId != null) {
                    userDAO.updatePendingBalance(sellerId, -winningBid);
                    SessionRegistry.getInstance().addUnsettledBalance(sellerId, -winningBid);
                    notifyUnsettledBalanceUpdate(sellerId, SessionRegistry.getInstance().getUnsettledBalance(sellerId));
                }

                logger.log(Level.INFO, "Refunded winning bid of {0} to bidderId: {1} and updated pending balance for sellerId: {2} after auction cancellation.",
                        new Object[]{winningBid, winnerId, sellerId});
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to refund winning bid to bidderId: " + winnerId + " or update sellerId: " + sellerId, e);
                throw new DAOException(ErrorCode.ACCOUNT_BALANCE_UPDATE_FAILED, "Failed to refund winning bid", e);
            }
        }
    }

    private void notifyUnsettledBalanceUpdate(String userId, long unsettledBalance) {
        PrintWriter writer = SessionRegistry.getInstance().getWriter(userId);
        if (writer == null) return;

        try {
            synchronized (writer) {
                writer.println(JsonUtils.toJson(ClientMessage.push("UNSETTLED_UPDATE", unsettledBalance)));
                writer.flush();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to notify balance update for userId: " + userId, e);
        }
    }
}
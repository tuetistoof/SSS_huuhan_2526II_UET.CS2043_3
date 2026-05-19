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
     * Luồng: validate → đổi status RAM → shutdown worker → update DB → refund → dọn dẹp.
     *
     * FIX #1: Bổ sung getLockAmount(auctionId) thay cho dòng syntax lỗi bị bỏ dở.
     * FIX #3: Cải thiện rollback — phân biệt lỗi xảy ra TRƯỚC hay SAU khi DB đã commit,
     *         tránh tình trạng RAM và DB lệch nhau.
     */
    public void cancelAuction(String auctionId, String reason) throws ServiceException, Exception {
        logger.log(Level.INFO, "Admin canceling auctionId: {0}, reason: {1}", new Object[]{auctionId, reason});

        // 1. Validate
        if (auctionId == null || auctionId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_AUCTION_ID, "The auction identifier is required.");
        }

        Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
        if (auction == null) {
            throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "Auction not found or already ended: " + auctionId);
        }

        // 2. Atomic Status Change (Khóa Race Condition)
        AuctionStatus previousStatus;
        synchronized (auction) {
            if (!auction.getStatus().isActive()) {
                throw new ServiceException(ErrorCode.AUCTION_CLOSED, "Auction is already ended.");
            }
            previousStatus = auction.getStatus();
            auction.setStatus(AuctionStatus.CANCELED);
        }

        // FIX #3: Dùng flag để biết DB đã được update chưa.
        // Nếu DB chưa update → rollback RAM là hợp lệ.
        // Nếu DB đã update → KHÔNG rollback RAM (tránh RAM lệch DB), chỉ log CRITICAL.
        boolean dbUpdated = false;

        try {
            // 3. Tắt Worker
            ConcurrentBidManager.getInstance().shutdown(auctionId);

            // FIX #1: Lấy lockAmount của người đang giữ giá cao nhất từ lastBidTransaction
            // (thay cho dòng "ConcurrentBidManager.getInstance()." bị bỏ dở)
            long lockAmount = 0;
            if (auction.getLastBidTransaction() != null) {
                lockAmount = auction.getLastBidTransaction().getLockedBalance();
            }

            // 4. Cập nhật DB
            auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELED);
            dbUpdated = true; // DB đã commit — từ đây KHÔNG rollback RAM nữa

            // 5. Hoàn tiền cho người đang giữ giá (nếu có)
            refundWinner(auction, lockAmount);

            // 6. Dọn dẹp tài nguyên
            AuctionRegistry.getInstance().remove(auctionId);
            autoBidService.clearRegistrations(auctionId);
            broadcastAuctionCanceled(auction, reason);
            ChangeManager.getInstance().detachByAdmin(auction);

            logger.log(Level.INFO, "Auction successfully canceled by admin: {0}", auctionId);

        } catch (Exception e) {
            if (!dbUpdated) {
                // DB chưa bị thay đổi → rollback RAM về trạng thái cũ là an toàn
                synchronized (auction) {
                    auction.setStatus(previousStatus);
                }
                logger.log(Level.WARNING, "Auction cancel aborted (before DB update), RAM rolled back for: " + auctionId, e);
            } else {
                // DB đã CANCELED → KHÔNG rollback RAM, giữ nguyên CANCELED trong RAM
                // để RAM đồng bộ với DB. Chỉ log để ops team xử lý cleanup thủ công.
                logger.log(Level.SEVERE,
                    "CRITICAL: DB was updated to CANCELED but post-cancel cleanup failed for auctionId: "
                    + auctionId + ". Manual cleanup (refund/registry/autobid) may be required.", e);
            }
            throw e;
        }
    }

    // --- PRIVATE METHODS ---

    /**
     * Push thông báo AUCTION_CANCELED kèm lý do tới tất cả subscriber đang trong phòng.
     *
     * FIX #4: Thêm writer.flush() để đảm bảo message được gửi ngay,
     *         nhất quán với notifyUnsettledBalanceUpdate() đã có flush().
     */
    private void broadcastAuctionCanceled(Auction auction, String reason) {
        String auctionId   = auction.getAuctionConfig().getId();
        String auctionName = auction.getAuctionConfig().getName();

        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId",   auctionId);
        payload.put("auctionName", auctionName);
        payload.put("reason",      reason != null ? reason : "");

        String message = JsonUtils.toJson(ClientMessage.push("AUCTION_CANCELED", payload));

        SessionRegistry.getInstance().getAllWriters().forEach((userId, writer) -> {
            if (ChangeManager.getInstance().hasObserver(auction, userId)) {
                try {
                    synchronized (writer) {
                        writer.println(message);
                        writer.flush(); // FIX #4: flush để đảm bảo message không nằm trong buffer
                    }
                } catch (Exception exception) {
                    logger.log(Level.WARNING,
                        "Failed to deliver AUCTION_CANCELED to userId: " + userId, exception);
                }
            }
        });
    }

    /**
     * Hoàn tiền cho bidder đang giữ giá cao nhất khi auction bị cancel.
     *
     * FIX #2 (cải thiện): Bỏ dòng check thừa "if (winningBid == 0) winningBid = ..."
     *   vì nhánh ternary đã bao phủ hoàn toàn trường hợp lockAmount == 0.
     *   Nếu cả hai đều bằng 0 (chưa có bid nào) → winnerId sẽ null → không vào refund block,
     *   hành vi đúng.
     */
    private void refundWinner(Auction auction, long lockAmount) throws Exception {
        String winnerId   = auction.getHighestBidderId();
        String sellerId   = auction.getSellerId();
        long   winningBid = lockAmount > 0 ? lockAmount : auction.getCurrentPrice();

        // Chỉ refund khi thực sự có người đang giữ giá VÀ số tiền > 0
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
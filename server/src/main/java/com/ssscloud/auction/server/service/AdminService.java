package com.ssscloud.auction.server.service;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
// FILE: server/src/main/java/com/ssscloud/auction/server/service/AdminService.java
//
// THAY ĐỔI: cancelAuction() → chia thành 2 bước:
//   1. gracefulCancel()  : softClose + broadcast countdown + delay 10s + cancel thật
//   2. doCancel()        : logic cancel hiện tại (đổi status, DB, refund, cleanup) — không đổi
//
// THÊM MỚI:
//   - field: ScheduledExecutorService cancelScheduler
//   - method: gracefulCancel()
//   - method: broadcastCancelCountdown()
//
// IMPORT CẦN THÊM:
//   import java.util.concurrent.Executors;
//   import java.util.concurrent.ScheduledExecutorService;
//   import java.util.concurrent.TimeUnit;

public class AdminService {

    private static final Logger logger = Logger.getLogger(AdminService.class.getName());
    private static final int CANCEL_COUNTDOWN_SECONDS = 10;

    private final AdminDAO      adminDAO;
    private final AuctionDAO    auctionDAO;
    private final AutoBidService autoBidService;
    private final UserDAO       userDAO;

    // Scheduler riêng cho delayed cancel — dùng single thread vì cancel không cần parallel
    private final ScheduledExecutorService cancelScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "auction-cancel-scheduler");
                t.setDaemon(true);
                return t;
            });

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
     * Entry point cho admin cancel — thay thế cancelAuction() cũ.
     *
     * Flow:
     *   1. Validate + check auction còn active
     *   2. softClose() → chặn bid/autoBid mới ngay lập tức
     *   3. Broadcast AUCTION_CANCEL_COUNTDOWN tới tất cả user trong phòng
     *   4. Schedule doCancel() sau CANCEL_COUNTDOWN_SECONDS giây
     *
     * Method này return ngay (non-blocking) sau khi schedule xong.
     * doCancel() chạy async trên cancelScheduler.
     */
    public void cancelAuction(String auctionId, String reason) throws ServiceException, Exception {
        logger.log(Level.INFO, "Admin initiating graceful cancel for auctionId: {0}, reason: {1}",
            new Object[]{auctionId, reason});

        // 1. Validate
        if (auctionId == null || auctionId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_AUCTION_ID,
                "The auction identifier is required.");
        }

        Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
        if (auction == null) {
            throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND,
                "Auction not found or already ended: " + auctionId);
        }

        // 2. Check status trước khi commit vào graceful flow
        synchronized (auction) {
            if (!auction.getStatus().isActive()) {
                throw new ServiceException(ErrorCode.AUCTION_CLOSED,
                    "Auction is already ended.");
            }
        }

        // 3. softClose — chặn bid mới ngay, worker vẫn drain queue hiện tại
        ConcurrentBidManager.getInstance().softClose(auctionId);

        // 4. Broadcast countdown tới client
        broadcastCancelCountdown(auction, reason, CANCEL_COUNTDOWN_SECONDS);

        // 5. Schedule doCancel() sau 10s — chạy async, không block response về admin
        cancelScheduler.schedule(() -> {
            try {
                doCancel(auctionId, reason);
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] doCancel failed for auctionId: " + auctionId, e);
            }
        }, CANCEL_COUNTDOWN_SECONDS, TimeUnit.SECONDS);

        logger.log(Level.INFO,
            "Graceful cancel scheduled for auctionId: {0} — fires in {1}s",
            new Object[]{auctionId, CANCEL_COUNTDOWN_SECONDS});
    }

    // --- PRIVATE METHODS ---

    /**
     * Thực hiện cancel thật sau khi countdown hết.
     * Logic giống cancelAuction() cũ — validate → đổi status RAM → shutdown worker
     * → update DB → refund → cleanup.
     */
    private void doCancel(String auctionId, String reason) throws ServiceException, Exception {
        logger.log(Level.INFO, "doCancel executing for auctionId: {0}", auctionId);

        Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
        if (auction == null) {
            // Có thể auction đã bị scheduleClose() kết thúc trong 10s chờ
            logger.log(Level.WARNING,
                "doCancel: auction no longer in registry (may have finished naturally): " + auctionId);
            return;
        }

        // Atomic status change — dùng synchronized để race với scheduleClose()
        AuctionStatus previousStatus;
        synchronized (auction) {
            if (!auction.getStatus().isActive()) {
                // scheduleClose() đã chạy trước → auction đã FINISHED → không cancel nữa
                logger.log(Level.WARNING,
                    "doCancel: auction already in terminal state {0}, skipping cancel: {1}",
                    new Object[]{auction.getStatus(), auctionId});
                return;
            }
            previousStatus = auction.getStatus();
            auction.setStatus(AuctionStatus.CANCELED);
        }

        boolean dbUpdated = false;
        Exception refundException = null;

        try {
            // shutdown() — interrupt worker + join(5000)
            // Worker đã drain queue trong 10s chờ nên lúc này queue gần như trống.
            ConcurrentBidManager.getInstance().shutdown(auctionId);

            long lockAmount = 0;
            if (auction.getLastBidTransaction() != null) {
                lockAmount = auction.getLastBidTransaction().getLockedBalance();
            }

            auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELED);
            dbUpdated = true;

            try {
                refundWinner(auction, lockAmount);
            } catch (Exception e) {
                refundException = e;
                logger.log(Level.SEVERE,
                    "Refund failed after DB cancel for auctionId: " + auctionId
                    + ". Balance may need manual correction for winnerId: "
                    + auction.getHighestBidderId(), e);
            }

        } catch (Exception e) {
            if (!dbUpdated) {
                synchronized (auction) {
                    auction.setStatus(previousStatus);
                }
                logger.log(Level.WARNING,
                    "doCancel aborted before DB update, RAM rolled back: " + auctionId, e);
            } else {
                logger.log(Level.SEVERE,
                    "CRITICAL: DB updated to CANCELED but subsequent step failed for auctionId: " + auctionId, e);
            }
            throw e;

        } finally {
            if (dbUpdated) {
                cleanupCanceledAuction(auction, auctionId, reason);
            }
        }

        if (refundException != null) {
            throw refundException;
        }

        logger.log(Level.INFO, "Auction successfully canceled: {0}", auctionId);
    }

    /**
     * Broadcast AUCTION_CANCEL_COUNTDOWN tới tất cả user đang trong phòng đấu giá.
     * Client dùng để hiện countdown "Auction sẽ bị hủy sau Xs" và disable nút bid.
     */
    private void broadcastCancelCountdown(Auction auction, String reason, int countdownSeconds) {
        String auctionId   = auction.getAuctionConfig().getId();
        String auctionName = auction.getAuctionConfig().getName();

        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId",        auctionId);
        payload.put("auctionName",      auctionName);
        payload.put("reason",           reason != null ? reason : "");
        payload.put("countdownSeconds", countdownSeconds);

        String message = JsonUtils.toJson(ClientMessage.push("AUCTION_CANCEL_COUNTDOWN", payload));

        SessionRegistry.getInstance().getAllWriters().forEach((userId, writer) -> {
            if (ChangeManager.getInstance().hasObserver(auction, userId)) {
                try {
                    synchronized (writer) {
                        writer.println(message);
                        writer.flush();
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                        "Failed to deliver AUCTION_CANCEL_COUNTDOWN to userId: " + userId, e);
                }
            }
        });

        logger.log(Level.INFO,
            "Broadcast AUCTION_CANCEL_COUNTDOWN for auctionId: {0}, countdown: {1}s",
            new Object[]{auctionId, countdownSeconds});
    }

    /**
     * Dọn dẹp tài nguyên sau khi DB đã commit CANCELED.
     * Mỗi bước được try/catch riêng — lỗi ở một bước không chặn các bước còn lại.
     */
    private void cleanupCanceledAuction(Auction auction, String auctionId, String reason) {
        try {
            AuctionRegistry.getInstance().remove(auctionId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to remove auction from registry: " + auctionId, e);
        }

        try {
            autoBidService.clearRegistrations(auctionId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to clear auto-bid registrations: " + auctionId, e);
        }

        try {
            broadcastAuctionCanceled(auction, reason);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to broadcast AUCTION_CANCELED: " + auctionId, e);
        }

        try {
            ChangeManager.getInstance().detachByAdmin(auction);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to detach observers: " + auctionId, e);
        }
    }

    /**
     * Push thông báo AUCTION_CANCELED kèm lý do tới tất cả subscriber đang trong phòng.
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
                        writer.flush();
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
     */
    private void refundWinner(Auction auction, long lockAmount) throws Exception {
        String winnerId   = auction.getHighestBidderId();
        String sellerId   = auction.getSellerId();
        long   winningBid = lockAmount > 0 ? lockAmount : auction.getCurrentPrice();

        if (winnerId != null && winningBid > 0) {
            try {
                userDAO.unlockBidderBalance(winnerId, winningBid);
                SessionRegistry.getInstance().addUnsettledBalance(winnerId, -winningBid);
                notifyUnsettledBalanceUpdate(winnerId, SessionRegistry.getInstance().getUnsettledBalance(winnerId));

                if (sellerId != null) {
                    userDAO.updatePendingBalance(sellerId, -winningBid);
                    SessionRegistry.getInstance().addUnsettledBalance(sellerId, -winningBid);
                    notifyUnsettledBalanceUpdate(sellerId, SessionRegistry.getInstance().getUnsettledBalance(sellerId));
                }

                logger.log(Level.INFO,
                    "Refunded winning bid of {0} to bidderId: {1} and updated pending balance for sellerId: {2} after auction cancellation.",
                    new Object[]{winningBid, winnerId, sellerId});
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                    "Failed to refund winning bid to bidderId: " + winnerId + " or update sellerId: " + sellerId, e);
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
package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.response.DTO.AdminDisplayDTO;
import com.ssscloud.auction.common.payload.response.DTO.UserDTO;
import com.ssscloud.auction.common.payload.response.request.AdminMetrics;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.AdminDAO;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;
import com.ssscloud.auction.server.util.SessionRegistry;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminService {

  private static final Logger logger = Logger.getLogger(AdminService.class.getName());

  private final AdminDAO adminDAO;
  private final AuctionDAO auctionDAO;
  private final AutoBidService autoBidService;
  private final UserDAO userDAO;


  private final Set<String> cancelInProgress = ConcurrentHashMap.newKeySet();

  public AdminService(
      AdminDAO adminDAO, AuctionDAO auctionDAO, AutoBidService autoBidService, UserDAO userDAO) {
    this.adminDAO = adminDAO;
    this.auctionDAO = auctionDAO;
    this.autoBidService = autoBidService;
    this.userDAO = userDAO;
  }


  public List<AdminDisplayDTO> getAuctions(AuctionStatus filter)
      throws ServiceException, Exception {
    try {
      logger.log(
          Level.INFO,
          "Admin retrieving auction list, filter: {0}",
          filter != null ? filter.name() : "ALL");
      return adminDAO.findAllAuctions(filter);
    } catch (ServiceException serviceException) {
      throw serviceException;
    } catch (Exception exception) {
      logger.log(
          Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in AdminService.getAuctions", exception);
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
      logger.log(
          Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in AdminService.getMetrics", exception);
      throw exception;
    }
  }

  public List<UserDTO> getUsers(String roleFilter) throws ServiceException, Exception {
    try {
      logger.log(
          Level.INFO,
          "Admin retrieving user list, roleFilter: {0}",
          roleFilter != null ? roleFilter : "ALL");
      return adminDAO.getAllUsers(roleFilter);
    } catch (ServiceException serviceException) {
      throw serviceException;
    } catch (Exception exception) {
      logger.log(
          Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in AdminService.getUsers", exception);
      throw exception;
    }
  }

  /**
   * Hủy một auction đang chạy.
   *
   * 1. Validate auctionId + tồn tại trong registry hoặc database 
   * 2. 
   * guard cancelInProgress — reject ngay nếu đang cancel 
   * 3. Synchronized check isActive() — reject nếu đã terminated 
   * 4. softClose() + broadcast AUCTION_CANCEL_COUNTDOWN ngay lập tức 
   * 5. doCancel() chạy đồng bộ trên cùng thread — đảm bảo side effects (DB update, registry remove,
   * refund) hoàn tất trước khi method return.
   *
  **/

  public void cancelAuction(String auctionId, String reason) throws ServiceException, Exception {
    logger.log(
        Level.INFO,
        "Admin initiating cancel for auctionId: {0}, reason: {1}",
        new Object[] {auctionId, reason});
 
    if (auctionId == null || auctionId.isBlank()) {
      throw new ServiceException(
          ErrorCode.INVALID_AUCTION_ID, "The auction identifier is required.");
    }
 
    
    Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId); //thử lấy từ Registry trước, nếu không db
    if (!cancelInProgress.add(auctionId)) {  //chỉ 1 auction cancel 1 thời điểm
      throw new ServiceException(
          ErrorCode.AUCTION_CLOSED, "Auction cancellation is already in progress: " + auctionId);
    }
 
    try {
      if (auction != null) { //đang có trong ram
        synchronized (auction) {
          if (!auction.getStatus().isActive()) {
            throw new ServiceException(ErrorCode.AUCTION_CLOSED, "Auction is already ended.");
          }
        }
        ConcurrentBidManager.getInstance().softClose(auctionId);
        autoBidService.clearRegistrations(auctionId);
        broadcastCancelCountdown(auction, reason, 0);
        doCancel(auctionId, reason);
 
      } else {
        //cancel thẳng qua DB
        Auction dbAuction = auctionDAO.findByAuctionId(auctionId);
        if (dbAuction == null) {
          throw new ServiceException(
              ErrorCode.AUCTION_NOT_FOUND, "Auction not found: " + auctionId);
        }
        if (!dbAuction.getStatus().isActive()) {
          throw new ServiceException(
              ErrorCode.AUCTION_CLOSED, "Auction is already ended: " + auctionId);
        }
 
        // Không có RAM object → update DB trực tiếp
        auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELED);
        autoBidService.clearRegistrations(auctionId);
 
        if (dbAuction.getLastBidTransaction() != null) {
          long lockAmount = dbAuction.getLastBidTransaction().getLockedBalance();
          refundWinner(dbAuction, lockAmount);
        }
 
        logger.log(Level.INFO,
            "Auction cancelled via DB-only path (not in registry): {0}", auctionId);
      }
 
    } finally {
      cancelInProgress.remove(auctionId);
    }
 
    logger.log(Level.INFO, "Auction successfully cancelled: {0}", auctionId);
  }

  // =========================================================================
  // PRIVATE METHODS
  // =========================================================================

  
  private void doCancel(String auctionId, String reason) throws ServiceException, Exception {
    logger.log(Level.INFO, "doCancel executing for auctionId: {0}", auctionId);

    Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
    if (auction == null) {
      // scheduleClose() đã xử lý auction này rồi — không cần cancel nữa
      logger.log(
          Level.WARNING,
          "doCancel: auction no longer in registry (may have finished naturally): " + auctionId);
      return;
    }

    // Atomic status change — race với scheduleClose()
    AuctionStatus previousStatus;
    synchronized (auction) {
      if (!auction.getStatus().isActive()) {
        logger.log(
            Level.WARNING,
            "doCancel: auction already in terminal state {0}, skipping cancel: {1}",
            new Object[] {auction.getStatus(), auctionId});
        return;
      }
      previousStatus = auction.getStatus();
      auction.setStatus(AuctionStatus.CANCELED);
    }

    boolean dbUpdated = false;
    Exception refundException = null;

    try {
      // Shutdown worker — interrupt + join(5000ms)
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
        logger.log(
            Level.SEVERE,
            "Refund failed after DB cancel for auctionId: "
                + auctionId
                + ". Balance may need manual correction for winnerId: "
                + auction.getHighestBidderId(),
            e);
      }

    } catch (Exception e) {
      if (!dbUpdated) {
        // DB chưa update → rollback RAM status
        synchronized (auction) {
          auction.setStatus(previousStatus);
        }
        logger.log(
            Level.WARNING, "doCancel aborted before DB update, RAM rolled back: " + auctionId, e);
      } else {
        logger.log(
            Level.SEVERE,
            "CRITICAL: DB updated to CANCELED but subsequent step failed for auctionId: "
                + auctionId,
            e);
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

    logger.log(Level.INFO, "doCancel completed for auctionId: {0}", auctionId);
  }


  private void broadcastCancelCountdown(Auction auction, String reason, int countdownSeconds) {
    String auctionId = auction.getAuctionConfig().getId();
    String auctionName = auction.getAuctionConfig().getName();

    Map<String, Object> payload = new HashMap<>();
    payload.put("auctionId", auctionId);
    payload.put("auctionName", auctionName);
    payload.put("reason", reason != null ? reason : "");
    payload.put("countdownSeconds", countdownSeconds);

    String message = JsonUtils.toJson(ClientMessage.push("AUCTION_CANCEL_COUNTDOWN", payload));

    SessionRegistry.getInstance()
        .getAllWriters()
        .forEach(
            (userId, writer) -> {
              if (ChangeManager.getInstance().hasObserver(auction, userId)) {
                try {
                  synchronized (writer) {
                    writer.println(message);
                    writer.flush();
                  }
                } catch (Exception e) {
                  logger.log(
                      Level.WARNING,
                      "Failed to deliver AUCTION_CANCEL_COUNTDOWN to userId: " + userId,
                      e);
                }
              }
            });

    logger.log(
        Level.INFO,
        "Broadcast AUCTION_CANCEL_COUNTDOWN for auctionId: {0}, countdown: {1}s",
        new Object[] {auctionId, countdownSeconds});
  }


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

  /** Push thông báo AUCTION_CANCELED kèm lý do tới tất cả subscriber trong phòng. */
  private void broadcastAuctionCanceled(Auction auction, String reason) {
    String auctionId = auction.getAuctionConfig().getId();
    String auctionName = auction.getAuctionConfig().getName();

    Map<String, Object> payload = new HashMap<>();
    payload.put("auctionId", auctionId);
    payload.put("auctionName", auctionName);
    payload.put("reason", reason != null ? reason : "");

    String message = JsonUtils.toJson(ClientMessage.push("AUCTION_CANCELED", payload));

    SessionRegistry.getInstance()
        .getAllWriters()
        .forEach(
            (userId, writer) -> {
              if (ChangeManager.getInstance().hasObserver(auction, userId)) {
                try {
                  synchronized (writer) {
                    writer.println(message);
                    writer.flush();
                  }
                } catch (Exception exception) {
                  logger.log(
                      Level.WARNING,
                      "Failed to deliver AUCTION_CANCELED to userId: " + userId,
                      exception);
                }
              }
            });
  }

  /** Hoàn tiền cho bidder đang giữ giá cao nhất khi auction bị cancel. */
  private void refundWinner(Auction auction, long lockAmount) throws Exception {
    String winnerId = auction.getHighestBidderId();
    String sellerId = auction.getSellerId();
    long winningBid = lockAmount > 0 ? lockAmount : auction.getCurrentPrice();

    if (winnerId != null && winningBid > 0) {
      try {
        userDAO.unlockBidderBalance(winnerId, winningBid);
        SessionRegistry.getInstance().addUnsettledBalance(winnerId, -winningBid);
        notifyUnsettledBalanceUpdate(
            winnerId, SessionRegistry.getInstance().getUnsettledBalance(winnerId));

        if (sellerId != null) {
          userDAO.updatePendingBalance(sellerId, -winningBid);
          SessionRegistry.getInstance().addUnsettledBalance(sellerId, -winningBid);
          notifyUnsettledBalanceUpdate(
              sellerId, SessionRegistry.getInstance().getUnsettledBalance(sellerId));
        }

        logger.log(
            Level.INFO,
            "Refunded winning bid of {0} to bidderId: {1} and updated pending balance for sellerId:"
                + " {2} after auction cancellation.",
            new Object[] {winningBid, winnerId, sellerId});
      } catch (Exception e) {
        logger.log(
            Level.SEVERE,
            "Failed to refund winning bid to bidderId: "
                + winnerId
                + " or update sellerId: "
                + sellerId,
            e);
        throw new DAOException(
            ErrorCode.ACCOUNT_BALANCE_UPDATE_FAILED, "Failed to refund winning bid", e);
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

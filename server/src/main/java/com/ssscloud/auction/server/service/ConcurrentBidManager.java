package com.ssscloud.auction.server.service;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.controller.NotificationController;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.SessionRegistry;

/**
 * ConcurrentBidManager handles asynchronous bid processing for active auctions.
 * It utilizes a per-auction queue and worker thread model to ensure thread safety 
 * and sequential consistency of bids within a single auction room.
 */
public class ConcurrentBidManager {
    private static final Logger logger = Logger.getLogger(ConcurrentBidManager.class.getName()); // Logging Standards: Declared first

    private static volatile ConcurrentBidManager instance = null;

    private UserDAO userDAO;
    private BidTransactionDAO bidTransactionDAO; // Dependency Injection: Short name for DAO
    private AutoBidService autoBidService;
    private AuctionDAO auctionDAO;
    private NotificationController notificationController;
    private final Map<String, BlockingQueue<BidTask>> bidTaskQueues = new ConcurrentHashMap<>(); // Internal Logic: Descriptive naming
    private final Map<String, Thread> workerThreads = new ConcurrentHashMap<>(); // Internal Logic: Descriptive naming
    private final Set<String> closedAuctions = ConcurrentHashMap.newKeySet(); // Internal Logic: Track closed auctions to prevent new bids

    private ConcurrentBidManager() {}

    private ConcurrentBidManager(UserDAO userDAO, BidTransactionDAO bidTransactionDAO, AutoBidService autoBidService, AuctionDAO auctionDAO, NotificationController notificationController ) {
        this.userDAO = userDAO;
        this.bidTransactionDAO = bidTransactionDAO;
        this.autoBidService = autoBidService;
        this.auctionDAO = auctionDAO;
        this.notificationController = notificationController;

    } // Constructor section

    // --- PUBLIC METHODS ---

    public static ConcurrentBidManager getInstance() {
        if (instance == null) {
            synchronized (ConcurrentBidManager.class) {
                if (instance == null) {
                    instance = new ConcurrentBidManager();
                }
            }
        }
        return instance;
    }

    public static ConcurrentBidManager resetInstance(){
        if (instance != null) {
            synchronized (ConcurrentBidManager.class) {
                if (instance != null) {
                    instance.workerThreads.forEach((auctionId, thread) -> thread.interrupt());
                    instance.workerThreads.clear();
                    instance.bidTaskQueues.clear();
                    instance.closedAuctions.clear();
                    instance = null;
                }
            }
        }
        return instance;
    }

    public static ConcurrentBidManager initialize(UserDAO userDAO, BidTransactionDAO bidTransactionDAO, AutoBidService autoBidService, AuctionDAO auctionDAO, NotificationController notificationController) throws Exception {
        try {
            synchronized (ConcurrentBidManager.class) {
                if (instance == null) {
                    instance = new ConcurrentBidManager(userDAO,bidTransactionDAO, autoBidService, auctionDAO, notificationController);
                } else {
                    instance.updateDependencies(userDAO, bidTransactionDAO, autoBidService, auctionDAO, notificationController);
                }
            }
            return instance;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during ConcurrentBidManager initialization", exception);
            throw exception;
        }
    }

    public void submitBid(Auction auctionEntity, String bidderId, String bidderUsername,
                          long bidAmount, BidType bidType) throws Exception {
        try {
            String auctionId = auctionEntity.getAuctionConfig().getId();
            ensureWorkerRunning(auctionId);
            bidTaskQueues.get(auctionId).offer(new BidTask(
                    auctionEntity, bidderId, bidderUsername, bidAmount, bidType));
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error while submitting bid for auctionId: " + auctionEntity.getAuctionConfig().getId(), exception);
            throw exception;
        }
    }

    private void updateDependencies(UserDAO userDAO, BidTransactionDAO bidTransactionDAO, AutoBidService autoBidService, AuctionDAO auctionDAO, NotificationController notificationController) throws Exception {
        try {
            this.userDAO = userDAO;
            this.bidTransactionDAO = bidTransactionDAO;
            this.autoBidService = autoBidService;
            this.auctionDAO = auctionDAO;
            this.notificationController = notificationController;

        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error updating dependencies", exception);
            throw exception;
        }
    }

    public void shutdown(String auctionId) throws Exception {
        try {
            closedAuctions.add(auctionId);
            bidTaskQueues.remove(auctionId);
            Thread workerThread = workerThreads.remove(auctionId);
            if (workerThread != null) {
                workerThread.interrupt();
                workerThread.join(5000); // Wait up to 5 seconds for the thread to terminate gracefully
                logger.log(Level.INFO, "Bid worker thread terminated for auctionId: " + auctionId);
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during shutdown for auctionId: " + auctionId, exception);
            throw exception;
        }
    }

    // --- PRIVATE METHODS ---

    private void ensureWorkerRunning(String auctionId) throws Exception {
        try {
            if (closedAuctions.contains(auctionId)) {
                throw new ServiceException(ErrorCode.INVALID_BID_REQUEST, "Cannot place bid: auction is closed for auctionId: " + auctionId);
            }
            bidTaskQueues.computeIfAbsent(auctionId, k -> new LinkedBlockingQueue<>());
            workerThreads.computeIfAbsent(auctionId, k -> {
                Thread workerThread = new Thread(() -> runWorker(auctionId));
                workerThread.setDaemon(true);
                workerThread.setName("bid-worker-" + auctionId);
                workerThread.start();
                logger.log(Level.INFO, "Sequential bid worker thread started for auctionId: " + auctionId);
                return workerThread;
            });
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error ensuring worker thread for auctionId: " + auctionId, exception);
            throw exception;
        }
    }

    private void runWorker(String auctionId) {
        try {
            BlockingQueue<BidTask> taskQueue = bidTaskQueues.get(auctionId);
            if (taskQueue == null) {
                return;
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    BidTask task = taskQueue.take();
                    processTask(task);
                } catch (InterruptedException e) {
                    logger.log(Level.INFO, "Execution interrupted for bid worker associated with auctionId: " + auctionId);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unexpected error while processing bid tasks for auctionId: " + auctionId, e);
                }
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error in bid worker thread for auctionId: " + auctionId, exception);
        }
    }

    private void processTask(BidTask task) throws Exception {
        try {
            Auction auctionEntity = task.auction;
            if (!auctionEntity.getStatus().isActive()) {
                throw new ServiceException(ErrorCode.INVALID_BID_REQUEST, "Cannot place bid: auction is not active for auctionId: " + auctionEntity.getAuctionConfig().getId());
            }
            String auctionId = auctionEntity.getAuctionConfig().getId();
            BidTransaction lastBidTransaction = auctionEntity.getLastBidTransaction();
            long currentAuctionPrice = lastBidTransaction != null ? lastBidTransaction.getBidAmount() : auctionEntity.getAuctionConfig().getStartPrice();
            if (task.bidAmount > currentAuctionPrice) {
                String previousBidderId = lastBidTransaction != null ? lastBidTransaction.getBidderId() : null;
                if (previousBidderId != null) {
                    userDAO.unlockBidderBalance(previousBidderId, currentAuctionPrice);
                    SessionRegistry.getInstance().addUnsettledBalance(previousBidderId, -currentAuctionPrice);
                    notifyUnsettledBalanceUpdate(previousBidderId, SessionRegistry.getInstance().getUnsettledBalance(previousBidderId));
                }

                // Lock bidder mới
                userDAO.lockBidderBalance(task.bidderId, task.bidAmount);
                SessionRegistry.getInstance().addUnsettledBalance(task.bidderId, task.bidAmount);
                notifyUnsettledBalanceUpdate(task.bidderId, SessionRegistry.getInstance().getUnsettledBalance(task.bidderId));

                long delta = task.bidAmount - currentAuctionPrice;

                // Seller
                userDAO.updatePendingBalance(auctionEntity.getSellerId(), delta);
                SessionRegistry.getInstance().addUnsettledBalance(auctionEntity.getSellerId(), delta);
                notifyUnsettledBalanceUpdate(auctionEntity.getSellerId(), SessionRegistry.getInstance().getUnsettledBalance(auctionEntity.getSellerId()));

                BidTransaction bidTransaction = new BidTransaction(auctionId, task.bidderId, task.bidderUsername,
                        task.bidAmount, LocalDateTime.now(), task.bidType);

                auctionEntity.placeBid(bidTransaction);
                if (bidTransactionDAO != null) {
                    try {
                        bidTransactionDAO.saveBidTransaction(bidTransaction);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Database persistence failure: unable to save bid transaction for auctionId: " + auctionId, e);
                    }
                }
                ChangeManager.getInstance().notify(auctionEntity);
                notificationController.notifyWatchers(auctionEntity.getAuctionConfig().getId(), auctionEntity.getHighestBidderId());
                if (auctionEntity.getStatus() == AuctionStatus.OPEN) {
                    auctionEntity.setStatus(AuctionStatus.RUNNING);
                    auctionDAO.updateStatus(auctionId, AuctionStatus.RUNNING);
                }
                
                LocalDateTime updatedEndTime = AntiSnipingService.processAntiSniping(auctionEntity.getAuctionConfig());
                if (updatedEndTime != null && auctionDAO != null) {
                    auctionDAO.updateEndTime(auctionId, updatedEndTime);
                }

            } else {
                logger.log(Level.INFO, "Bid task skipped: amount " + task.bidAmount + " is not higher than current price " + currentAuctionPrice);
            }

            if (autoBidService != null) {
                try {
                    autoBidService.trigger(auctionEntity);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Auto-bid trigger failed for auctionId: " + auctionId, e);
                }
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during bid task processing", exception);
            throw exception;
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

    private static class BidTask {
        final Auction auction;
        final String bidderId;
        final String bidderUsername;
        final long bidAmount;
        final BidType bidType;

        BidTask(Auction auction, String bidderId, String bidderUsername, long bidAmount, BidType bidType) {
            this.auction = auction;
            this.bidderId = bidderId;
            this.bidderUsername = bidderUsername;
            this.bidAmount = bidAmount;
            this.bidType = bidType;
        }
    }
}
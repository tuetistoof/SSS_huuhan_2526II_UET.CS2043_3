package com.ssscloud.auction.server.service;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.model.auction.BidTransaction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.controller.NotificationController;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.SessionRegistry;

/**
 * ConcurrentBidManager handles ordered bid processing for active auctions.
 * It utilizes a per-auction queue and worker thread model to ensure thread safety 
 * and sequential consistency of bids within a single auction room. submitBid()
 * returns only after the worker has committed or rejected the bid.
 */
public class ConcurrentBidManager {
    private static final Logger logger = Logger.getLogger(ConcurrentBidManager.class.getName()); // Logging Standards: Declared first
    private static final int QUEUE_CAPACITY_PER_AUCTION = 10_000;
    private static final long ENQUEUE_TIMEOUT_MILLIS = 250;

    private static volatile ConcurrentBidManager instance = null;

    private UserDAO userDAO;
    private BidTransactionDAO bidTransactionDAO; // Dependency Injection: Short name for DAO
    private AutoBidService autoBidService;
    private AuctionDAO auctionDAO;
    private NotificationController notificationController;
    private final Map<String, BlockingQueue<BidTask>> bidTaskQueues = new ConcurrentHashMap<>(); // Internal Logic: Descriptive naming
    private final Map<String, Thread> workerThreads = new ConcurrentHashMap<>(); // Internal Logic: Descriptive naming
    private final Set<String> closedAuctions = ConcurrentHashMap.newKeySet(); // Internal Logic: Track closed auctions to prevent new bids
    private final Set<String> drainingAuctions = ConcurrentHashMap.newKeySet();

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
                          long bidAmount, long lockedAmount, BidType bidType) throws Exception {
        try {
            String auctionId = auctionEntity.getAuctionConfig().getId();
            ensureWorkerRunning(auctionId);

            BidTask bidTask = new BidTask(
                    auctionEntity, bidderId, bidderUsername, bidAmount, lockedAmount, bidType);
            Thread workerThread = workerThreads.get(auctionId);
            if (Thread.currentThread() == workerThread) {
                processTask(bidTask);
                return;
            }

            BlockingQueue<BidTask> taskQueue = bidTaskQueues.get(auctionId);
            if (taskQueue == null
                    || !taskQueue.offer(bidTask, ENQUEUE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                throw new ServiceException(
                        ErrorCode.CONCURRENT_BID_PROCESSING_ERROR,
                        "Bid queue is overloaded for auctionId: " + auctionId);
            }

            waitForCommit(bidTask, auctionId);
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

    private void waitForCommit(BidTask bidTask, String auctionId) throws Exception {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    bidTask.awaitCommit();
                    return;
                } catch (InterruptedException exception) {
                    interrupted = true;
                }
            }
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception originalException) {
                throw originalException;
            }
            throw new ServiceException(
                    ErrorCode.CONCURRENT_BID_PROCESSING_ERROR,
                    "Bid processing failed in auctionId: " + auctionId,
                    cause);
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown(String auctionId) throws Exception {
        drainingAuctions.add(auctionId);          
        Thread workerThread = workerThreads.remove(auctionId);
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread.join(5000);               
        }
        // ← Sau join(), mới xóa
        bidTaskQueues.remove(auctionId);           // ← Xóa queue (sau join)
        drainingAuctions.remove(auctionId);        // ← Clear drain flag (sau join)
        closedAuctions.add(auctionId);
    }

    // --- PRIVATE METHODS ---

    private void ensureWorkerRunning(String auctionId) throws Exception {
        try {
            if (closedAuctions.contains(auctionId)) {
                throw new ServiceException(ErrorCode.INVALID_BID_REQUEST, "Cannot place bid: auction is closed for auctionId: " + auctionId);
            }
            bidTaskQueues.computeIfAbsent(auctionId, k -> new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_AUCTION));
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

    public void softClose(String auctionId) {
        closedAuctions.add(auctionId);    // chặn bid/autobid mới
        drainingAuctions.add(auctionId);  // đánh dấu worker cần tự thoát khi queue rỗng
        logger.log(Level.INFO,
            "softClose: auction {0} marked closed — no new bids accepted. Worker draining.",
            auctionId);
    }

    private void runWorker(String auctionId) {
            try {
            BlockingQueue<BidTask> taskQueue = bidTaskQueues.get(auctionId);
            if (taskQueue == null) return;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Nếu đang drain: poll có timeout thay vì take() block mãi
                    BidTask task;
                    if (drainingAuctions.contains(auctionId)) {
                        task = taskQueue.poll(200, TimeUnit.MILLISECONDS);
                        if (task == null) {
                            // Queue rỗng + đang drain → worker tự thoát
                            logger.log(Level.INFO,
                                "bid-worker-{0}: queue drained, exiting gracefully.", auctionId);
                            break;
                        }
                    } else {
                        task = taskQueue.take(); // block bình thường
                    }
                    processTask(task);
                } catch (InterruptedException e) {
                    logger.log(Level.INFO,
                        "Execution interrupted for bid worker associated with auctionId: " + auctionId);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.log(Level.SEVERE,
                        "Unexpected error while processing bid tasks for auctionId: " + auctionId, e);
                }
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE,
                "Unexpected error in bid worker thread for auctionId: " + auctionId, exception);
        }
    }

    private void processTask(BidTask task) throws Exception {
        Auction auctionEntity = task.auction;
        String auctionId = auctionEntity.getAuctionConfig().getId();
 
        boolean bidCommitted = false;
 
        try {
            if (!auctionEntity.getStatus().isActive()) {
                logger.log(Level.WARNING,
                    "processTask: auction not active, rejecting bid for bidderId={0}, auctionId={1}",
                    new Object[]{task.bidderId, auctionId});
                throw new ServiceException(
                        ErrorCode.AUCTION_CLOSED,
                        "Cannot place bid: auction is no longer active for auctionId: " + auctionId);
            }
 
            BidTransaction lastBidTransaction = auctionEntity.getLastBidTransaction();
            long currentAuctionPrice = lastBidTransaction != null
                    ? lastBidTransaction.getBidAmount()
                    : auctionEntity.getAuctionConfig().getStartPrice();
            long minIncrement = auctionEntity.getAuctionConfig().getMinIncrement();
            boolean bidMeetsPriceRule = lastBidTransaction == null
                    ? task.bidAmount > currentAuctionPrice
                    : task.bidAmount - currentAuctionPrice >= minIncrement;
 
            if (bidMeetsPriceRule) {
                // --- Nhánh BID THẮNG ---
 
                // Bước 1: Unlock bidder CŨ (nếu có)
                String previousBidderId = lastBidTransaction != null ? lastBidTransaction.getBidderId() : null;
                if (previousBidderId != null) {
                    long unlockAmount = lastBidTransaction.getLockedBalance();
                    userDAO.unlockBidderBalance(previousBidderId, unlockAmount);
                    SessionRegistry.getInstance().addUnsettledBalance(previousBidderId, -unlockAmount);
                }
 
                // Bước 2: Cập nhật pending balance seller
                long delta = task.bidAmount - currentAuctionPrice;
                userDAO.updatePendingBalance(auctionEntity.getSellerId(), delta);
                SessionRegistry.getInstance().addUnsettledBalance(auctionEntity.getSellerId(), delta);
 
                // Bước 3: Tạo và commit bid transaction
                BidTransaction bidTransaction = new BidTransaction(
                        auctionId, task.bidderId, task.bidderUsername,
                        task.bidAmount, task.lockedAmount, LocalDateTime.now(), task.bidType);
 
                boolean shouldMarkRunning = auctionEntity.getStatus() == AuctionStatus.OPEN;
                LocalDateTime updatedEndTime = AntiSnipingService.calculateExtendedEndTime(
                        auctionEntity.getAuctionConfig());
                auctionEntity.commitBid(bidTransaction, shouldMarkRunning, updatedEndTime);
 
                bidCommitted = true;
                task.completeSuccess();
 
                if (bidTransactionDAO != null) {
                    try {
                        bidTransactionDAO.saveBidTransaction(bidTransaction);
                    } catch (Exception e) {
                        // Chỉ log — không rollback vì in-memory state đã commit
                        logger.log(Level.SEVERE,
                            "Database persistence failure: unable to save bid transaction for auctionId: " + auctionId, e);
                    }
                }
 
                if (shouldMarkRunning && auctionDAO != null) {
                    auctionDAO.updateStatus(auctionId, AuctionStatus.RUNNING);
                }
 
                if (updatedEndTime != null && auctionDAO != null) {
                    auctionDAO.updateEndTime(auctionId, updatedEndTime);
                }
                ChangeManager.getInstance().notify(auctionEntity);
                notificationController.notifyWatchers(auctionEntity,
                        auctionEntity.getHighestBidderId());
 
            } else {
                // --- Nhánh BID THUA ---
                logger.log(Level.INFO,
                    "Bid task skipped: amount " + task.bidAmount
                    + " does not satisfy current price " + currentAuctionPrice
                    + " with min increment " + minIncrement);
                throw new ServiceException(
                        ErrorCode.INCREMENT_TOO_LOW,
                        "The bid no longer satisfies the current price and minimum increment.");
            }
 
            if (autoBidService != null) {
                try {
                    autoBidService.trigger(auctionEntity);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Auto-bid trigger failed for auctionId: " + auctionId, e);
                }
            }
 
        } catch (Exception exception) {
            if (!bidCommitted) {
                logger.log(Level.SEVERE,
                    "[BID_REJECTED_BEFORE_COMMIT] bidderId={0}, amount={1}, auctionId={2}. Cause: {3}",
                    new Object[]{task.bidderId, task.lockedAmount, auctionId, exception.getMessage()});
                task.completeFailure(exception);
                throw exception;
            } else {
                task.completeSuccess();
                logger.log(Level.SEVERE,
                    "[CRITICAL] processTask failed AFTER bid commit for bidderId=" + task.bidderId
                    + ", auctionId=" + auctionId
                    + ". Balance NOT unlocked — will be handled by settle/cancel flow. Error: "
                    + exception.getMessage(),
                    exception);
            }
        }
    }

    private static class BidTask {
        final Auction auction;
        final String bidderId;
        final String bidderUsername;
        final long bidAmount;
        final long lockedAmount;
        final BidType bidType;
        private final CompletableFuture<Void> commitFuture = new CompletableFuture<>();

        BidTask(Auction auction, String bidderId, String bidderUsername, long bidAmount, long lockedAmount, BidType bidType) {
            this.auction = auction;
            this.bidderId = bidderId;
            this.bidderUsername = bidderUsername;
            this.bidAmount = bidAmount;
            this.lockedAmount = lockedAmount;
            this.bidType = bidType;
        }

        void completeSuccess() {
            commitFuture.complete(null);
        }

        void completeFailure(Exception exception) {
            commitFuture.completeExceptionally(exception);
        }

        void awaitCommit() throws InterruptedException, ExecutionException {
            commitFuture.get();
        }
    }
}

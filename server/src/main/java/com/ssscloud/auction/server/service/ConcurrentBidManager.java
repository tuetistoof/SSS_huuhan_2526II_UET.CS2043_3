package com.ssscloud.auction.server.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;

/**
 * ConcurrentBidManager handles asynchronous bid processing for active auctions.
 * It utilizes a per-auction queue and worker thread model to ensure thread safety 
 * and sequential consistency of bids within a single auction room.
 */
public class ConcurrentBidManager {
    private static final Logger logger = Logger.getLogger(ConcurrentBidManager.class.getName()); // Logging Standards: Declared first

    private static volatile ConcurrentBidManager instance = null;

    private BidTransactionDAO bidDAO; // Dependency Injection: Short name for DAO
    private AutoBidService autoBidService;
    private AuctionDAO auctionDAO;
    private final Map<String, BlockingQueue<BidTask>> bidTaskQueues = new ConcurrentHashMap<>(); // Internal Logic: Descriptive naming
    private final Map<String, Thread> workerThreads = new ConcurrentHashMap<>(); // Internal Logic: Descriptive naming

    private ConcurrentBidManager() {}

    private ConcurrentBidManager(BidTransactionDAO bidDAO, AutoBidService autoBidService, AuctionDAO auctionDAO) {
        this.bidDAO = bidDAO;
        this.autoBidService = autoBidService;
        this.auctionDAO = auctionDAO;
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

    public static ConcurrentBidManager initialize(BidTransactionDAO bidDAO, AutoBidService autoBidService, AuctionDAO auctionDAO) {
        synchronized (ConcurrentBidManager.class) {
            if (instance == null) {
                instance = new ConcurrentBidManager(bidDAO, autoBidService, auctionDAO);
            } else {
                instance.updateDependencies(bidDAO, autoBidService, auctionDAO);
            }
        }
        return instance;
    }

    public void submitBid(Auction auctionEntity, String bidderId, String bidderUsername,
                          long bidAmount, BidType bidType) { // Naming: full descriptive names
        String auctionId = auctionEntity.getAuctionConfig().getId();
        ensureWorkerRunning(auctionId);
        bidTaskQueues.get(auctionId).offer(new BidTask(
                auctionEntity, bidderId, bidderUsername, bidAmount, bidType));
    }

    private void updateDependencies(BidTransactionDAO bidDAO, AutoBidService autoBidService, AuctionDAO auctionDAO) {
        this.bidDAO = bidDAO;
        this.autoBidService = autoBidService;
        this.auctionDAO = auctionDAO;
    }

    public void shutdown(String auctionId) {
        bidTaskQueues.remove(auctionId);
        Thread workerThread = workerThreads.remove(auctionId);
        if (workerThread != null) {
            workerThread.interrupt();
            logger.log(Level.INFO, "Bid worker thread terminated for auctionId: " + auctionId); // Log style
        }
    }

    // --- PRIVATE METHODS ---

    private void ensureWorkerRunning(String auctionId) {
        bidTaskQueues.computeIfAbsent(auctionId, k -> new LinkedBlockingQueue<>());
        workerThreads.computeIfAbsent(auctionId, k -> {
            Thread workerThread = new Thread(() -> runWorker(auctionId));
            workerThread.setDaemon(true);
            workerThread.setName("bid-worker-" + auctionId);
            workerThread.start();
            logger.log(Level.INFO, "Sequential bid worker thread started for auctionId: " + auctionId);
            return workerThread;
        });
    }

    private void runWorker(String auctionId) {
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
                logger.log(Level.SEVERE, "Unexpected critical failure while processing bid tasks for auctionId: " + auctionId, e);
            }
        }
    }

    private void processTask(BidTask task) {
        Auction auctionEntity = task.auction;
        String auctionId = auctionEntity.getAuctionConfig().getId();
        long currentAuctionPrice = auctionEntity.getCurrentPrice();
        if (task.bidAmount > currentAuctionPrice) {
            BidTransaction bidTransaction = new BidTransaction(auctionId, task.bidderId, task.bidderUsername,
                    task.bidAmount, LocalDateTime.now(), task.bidType);
            
            if (auctionEntity.getStatus() == AuctionStatus.OPEN) {
                auctionEntity.setStatus(AuctionStatus.RUNNING);
                auctionDAO.updateStatus(auctionId, AuctionStatus.RUNNING);
            }

            auctionEntity.placeBid(bidTransaction);
            LocalDateTime updatedEndTime = AntiSnipingService.processAntiSniping(auctionEntity.getAuctionConfig());
            if (updatedEndTime != null && auctionDAO != null) {
                auctionDAO.updateEndTime(auctionId, updatedEndTime);
            }

            if (bidDAO != null) {
                try {
                    bidDAO.saveBidTransaction(bidTransaction);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Database persistence failure: unable to save bid transaction for auctionId: " + auctionId, e);
                }
            }
            ChangeManager.getInstance().notify(auctionEntity);
            NotificationService.getInstance().notifyWatchers(auctionEntity, auctionEntity.getHighestBidderId());
        } else {
            logger.log(Level.INFO, "Bid task skipped: amount " + task.bidAmount + " is not higher than current price " + currentAuctionPrice);
        }
        // if (task.bidAmount <= currentAuctionPrice) 
        //     return;
        // if (auctionEntity.getStatus().isEnded()) {
        //     logger.log(Level.WARNING, "Incoming bid rejected: the target auction has already concluded. AuctionId: " + auctionId + ", bidderId: " + task.bidderId);
        //     return;
        // }
        // if (auctionEntity.isExpired()) {
        //     logger.log(Level.WARNING, "Incoming bid rejected: the target auction has reached its expiration time. AuctionId: " + auctionId + ", bidderId: " + task.bidderId);
        //     return;
        // }

        // BidTransaction bidTransaction = new BidTransaction(auctionId, task.bidderId, task.bidderUsername,
        //         task.bidAmount, LocalDateTime.now(), task.bidType); // Naming: full descriptive name
        
        // if (auctionEntity.getStatus() == AuctionStatus.OPEN)
        // {
        //     auctionEntity.setStatus(AuctionStatus.RUNNING);
        //     auctionDAO.updateStatus(auctionId, AuctionStatus.RUNNING);
        // }

        // auctionEntity.placeBid(bidTransaction);
        // LocalDateTime updatedEndTime = AntiSnipingService.processAntiSniping(auctionEntity.getAuctionConfig());
        // if (updatedEndTime != null && auctionDAO != null) {
        //     auctionDAO.updateEndTime(auctionId, updatedEndTime);
        // }

        // if (bidDAO != null) {
        //     try {
        //         bidDAO.saveBidTransaction(bidTransaction);
        //     } catch (Exception e) {
        //         logger.log(Level.SEVERE, "Database persistence failure: unable to save bid transaction for auctionId: " + auctionId + ". Bid remains valid in memory.", e);
        //     }
        // }
        // ChangeManager.getInstance().notify(auctionEntity);
        // NotificationService.getInstance().notifyWatchers(auctionEntity, auctionEntity.getHighestBidderId());

        if (autoBidService != null) {
            autoBidService.trigger(auctionEntity);
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
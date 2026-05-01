package com.ssscloud.auction.server.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.BidTransactionDAO;

public class ConcurrentBidManager {
    private static volatile ConcurrentBidManager instance = null;

    private ConcurrentBidManager() {
    }

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

    private BidTransactionDAO bidTransactionDAO;
    private AutoBidService    autoBidService;

    public void init(BidTransactionDAO bidTransactionDAO, AutoBidService autoBidService) {
        this.bidTransactionDAO = bidTransactionDAO;
        this.autoBidService    = autoBidService;
    }

    private final Map<String, BlockingQueue<BidTask>> queues = new ConcurrentHashMap<>();
    private final Map<String, Thread> workers = new ConcurrentHashMap<>();
    
    public void submitBid(Auction auction, String bidderId, String bidderUsername,
                          long amount, BidType type) {
        String auctionId = auction.getAuctionConfig().getId();
        ensureWorkerRunning(auctionId);
        queues.get(auctionId).offer(new BidTask(
                auction, bidderId, bidderUsername, amount, type));
    }
    public void shutdown(String auctionId) {
        queues.remove(auctionId);
        Thread worker = workers.remove(auctionId);
        if (worker != null) worker.interrupt();
    }

    private void ensureWorkerRunning(String auctionId) {
        queues.computeIfAbsent(auctionId, k -> new LinkedBlockingQueue<>());
        workers.computeIfAbsent(auctionId, k -> {
            Thread worker = new Thread(() -> runWorker(auctionId));
            worker.setDaemon(true);
            worker.setName("bid-worker-" + auctionId);
            worker.start();
            return worker;
        });
    }

    private void runWorker(String auctionId) {
        BlockingQueue<BidTask> queue = queues.get(auctionId);
        if (queue == null) return;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                BidTask task = queue.take();
                processTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[BidWorker-" + auctionId + "] Lỗi: " + e.getMessage());
            }
        }
    }

     private void processTask(BidTask task) {
        Auction auction  = task.auction;
        String auctionId = auction.getAuctionConfig().getId();

        if (auction.getStatus().isEnded()) {
            System.err.println("[BidWorker] Phiên đã kết thúc: " + task.bidderId);
            return;
        }
        if (auction.isExpired()) {
            System.err.println("[BidWorker] Phiên hết giờ: " + task.bidderId);
            return;
        }
        long minIncrement = auction.getAuctionConfig().getMinIncrement();
        if (!BidValidator.isValidBid(task.bidAmount, auction.getCurrentPrice(), minIncrement)) {
            System.err.println("[BidWorker] Bid không hợp lệ — " + task.bidderId
                    + " | bidAmount: " + task.bidAmount
                    + " | yêu cầu: " + (auction.getCurrentPrice() + minIncrement));
            return;
        }

        BidTransaction bid = new BidTransaction(auctionId, task.bidderId, task.bidderUsername,
                task.bidAmount, LocalDateTime.now(), task.type);
        auction.placeBid(bid);

        AntiSnipingService.processAntiSniping(auction.getAuctionConfig());
        
        if (bidTransactionDAO != null) {
            try {
                bidTransactionDAO.saveBidTransaction(bid);
            } catch (Exception e) {
                System.err.println("[BidWorker] Lưu DB thất bại: " + e.getMessage()
                        + " — bid vẫn hợp lệ trong memory");
            }
        }

        ChangeManager.getInstance().notify(auction);

        if (autoBidService != null) {
            autoBidService.trigger(auction);
        }
    }


    private static class BidTask {
        final Auction auction;
        final String bidderId;
        final String bidderUsername;
        final long bidAmount;
        final BidType type;

        BidTask(Auction auction, String bidderId, String bidderUsername,long bidAmount, BidType type) {
            this.auction = auction;
            this.bidderId = bidderId;
            this.bidderUsername = bidderUsername;
            this.bidAmount = bidAmount;
            this.type = type;
        }
    }
}
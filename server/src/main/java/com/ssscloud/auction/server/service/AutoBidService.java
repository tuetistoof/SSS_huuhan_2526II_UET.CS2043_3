package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.model.auction.BidTransaction;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.common.model.user.Bidder;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.request.AutoBidRequest;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.SessionRegistry;
import com.ssscloud.auction.server.util.AuctionRegistry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AutoBidService manages the automated bidding logic for auction participants.
 * It maintains registrations and bid frequencies per auction room.
 *
 * CONCURRENCY MODEL (Per-Auction Worker Thread):
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ Auction A: autobid-worker-AuctionA ← triggerQueue-AuctionA │
 * │ Auction B: autobid-worker-AuctionB ← triggerQueue-AuctionB │
 * │ Auction C: autobid-worker-AuctionC ← triggerQueue-AuctionC │
 * │ ... │
 * │ Mỗi auction có 1 THREAD RIÊNG + 1 QUEUE RIÊNG. │
 * │ Các auction xử lí SONG SONG, không nghẽn cổ chai. │
 * │ Trong cùng 1 auction, trigger xử lí TUẦN TỰ (1 thread duy nhất).│
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * Flow:
 * 1. register() / processTask() gọi tryTrigger()
 * → Đẩy tín hiệu vào triggerQueue của auction đó → return NGAY (non-blocking)
 * 2. Worker thread (autobid-worker-XXX) nhận tín hiệu từ queue
 * → Gộp nhiều tín hiệu thành 1 lần xử lí (drain queue)
 * → Lấy trạng thái auction MỚI NHẤT từ AuctionRegistry
 * → Chạy trigger() để tìm winner và submit bid
 * 3. Nếu submit thất bại → loại winner, thử candidate tiếp (retry loop)
 * 4. Nếu submit thành công → ConcurrentBidManager worker xử lí bid
 * → Worker gọi lại tryTrigger() → tín hiệu mới vào queue → lặp lại
 * 5. Khi không còn candidate → worker chờ (block) cho đến khi có AutoBid mới
 */
public class AutoBidService {
    private static final Logger logger = Logger.getLogger(AutoBidService.class.getName());

    private final Map<String, List<AutoBidEntry>> registrationsMap = new ConcurrentHashMap<>();

    private final Set<String> cancelledAuctionIds = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<String, Integer>> bidCounts = new ConcurrentHashMap<>();

    private final Map<String, BlockingQueue<String>> triggerQueues = new ConcurrentHashMap<>();
    private final Map<String, Thread> autoBidWorkers = new ConcurrentHashMap<>();
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final SessionRegistry sessionRegistry = SessionRegistry.getInstance();

    public AutoBidService(AuctionDAO auctionDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
    }

    /**
     * Register an auto-bid request for a bidder in a specific auction.
     * Validates request integrity, auction status, and bidder financial capacity.
     *
     * Sau khi validate và thêm entry vào registrationsMap,
     * gọi tryTrigger() để đẩy tín hiệu vào queue của auction.
     * Thread client RETURN NGAY, không bị chặn.
     */
    public void register(AutoBidRequest autoBidRequest, String bidderId, String bidderUsername)
            throws ServiceException, Exception {
        try {
            validateAutoBidRequest(autoBidRequest, bidderId, bidderUsername);
            logger.log(Level.INFO, "Initiating auto-bid registration for auctionId: " + autoBidRequest.getAuctionId()
                    + " for bidderId: " + bidderId);
            Auction auctionEntity = AuctionRegistry.getInstance()
                    .retrieveAndValidateAuction(autoBidRequest.getAuctionId());
            validateAutoBidTerms(auctionEntity, autoBidRequest, bidderId);
            User bidder = userDAO.findById(bidderId);
            validateBidderAccount(bidder, autoBidRequest.getMaxBid(), auctionEntity);
            if (cancelledAuctionIds.contains(autoBidRequest.getAuctionId())) {
                throw new ServiceException(ErrorCode.AUCTION_CLOSED,
                        "Cannot register auto-bid: The auction has been cancelled.");
            }
            List<AutoBidEntry> autoBidEntriesList = registrationsMap.computeIfAbsent(
                    autoBidRequest.getAuctionId(), key -> new CopyOnWriteArrayList<>());
            autoBidEntriesList.removeIf(entry -> entry.bidderId.equals(bidderId));
            autoBidEntriesList.add(new AutoBidEntry(bidderId, bidderUsername, autoBidRequest.getMaxBid()));

            tryTrigger(autoBidRequest.getAuctionId());
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.register: " + exception.getMessage(),
                    exception);
            throw exception;
        }
    }

    /**
     * Non-blocking: đẩy tín hiệu trigger vào queue của auction.
     *
     * - Khởi tạo worker thread cho auction (nếu chưa có).
     * - Đẩy 1 tín hiệu "thức dậy" vào triggerQueue.
     * - Thread gọi (client socket / ConcurrentBidManager worker) RETURN NGAY.
     * - Worker thread riêng của auction sẽ nhận tín hiệu và chạy trigger().
     *
     * Gọi từ:
     * 1. register() — sau khi thêm entry vào registrationsMap
     * 2. ConcurrentBidManager.processTask() — sau khi worker xử lí xong 1 bid
     */
    public void tryTrigger(String auctionId) throws ServiceException, Exception {
        if (auctionId == null || auctionId.isBlank()) {
            logger.log(Level.WARNING, "AutoBid trigger failed: Missing or invalid auctionId.");
            throw new ServiceException(ErrorCode.MISSING_AUCTION_ID,
                    "Auto-bid trigger failed: The auction identifier is required.");
        }
        Auction auctionEntity = AuctionRegistry.getInstance().retrieveAndValidateAuction(auctionId);
        if (auctionEntity == null || auctionEntity.getStatus().isEnded()
                || auctionEntity.isExpired() || !auctionEntity.getStatus().isActive()) {
            return;
        }
        ensureAutoBidWorkerRunning(auctionId);
        triggerQueues.get(auctionId).offer(auctionId);
    }

    /**
     * Core trigger logic — xử lí auto-bid matching cho 1 auction.
     *
     * ★ CHỈ ĐƯỢC GỌI BỞI WORKER THREAD CỦA AUCTION ĐÓ. ★
     * Không cần lock vì mỗi auction chỉ có DUY NHẤT 1 worker thread.
     *
     * Flow:
     * 1. Lấy snapshot entries → tìm winner (maxBid cao nhất) → tính giá proxy
     * 2. Thử lock balance winner → submit bid vào ConcurrentBidManager queue
     * 3. Nếu lock balance thất bại hoặc submit thất bại:
     * → loại winner đó khỏi registrationsMap
     * → quay lại bước 1 thử candidate tiếp (retry loop)
     * 4. Nếu submit thành công:
     * → loại tất cả thua cuộc, giữ winner
     * → return (ConcurrentBidManager worker sẽ gọi lại tryTrigger sau khi xử lí
     * bid)
     * 5. Nếu không còn candidate nào thỏa mãn → dừng
     */
    public void trigger(String auctionId) throws ServiceException, Exception {
        if (auctionId == null || auctionId.isBlank()) {
            logger.log(Level.WARNING, "AutoBid trigger failed: Missing or invalid auctionId.");
            throw new ServiceException(ErrorCode.MISSING_AUCTION_ID,
                    "Auto-bid trigger failed: The auction identifier is required.");
        }
        try {
            while (true) {
                List<AutoBidEntry> autoBidEntriesList = registrationsMap.get(auctionId);
                if (autoBidEntriesList == null || autoBidEntriesList.isEmpty()) {
                    return;
                }
                Auction auctionEntity = AuctionRegistry.getInstance().retrieveAndValidateAuction(auctionId);
                if (auctionEntity == null || auctionEntity.getStatus().isEnded() || auctionEntity.isExpired()
                        || !auctionEntity.getStatus().isActive()) {
                    return;
                }
                auctionId = auctionEntity.getAuctionConfig().getId();
                long increment = auctionEntity.getAuctionConfig().getMinIncrement();
                BidTransaction lastBidTransaction = auctionEntity.getLastBidTransaction();
                boolean isFirstBid = (lastBidTransaction == null);
                long currentAuctionPrice = lastBidTransaction == null ? auctionEntity.getCurrentPrice()
                        : lastBidTransaction.getBidAmount();
                String highestBidderId = lastBidTransaction == null ? null : lastBidTransaction.getBidderId();
                long highestBidderLock = lastBidTransaction == null ? 0 : lastBidTransaction.getLockedBalance();
                BidType lastBidType = lastBidTransaction == null ? null : lastBidTransaction.getType();
                logger.log(Level.INFO, "Auto-bid matching triggered for auctionId: " + auctionId + " with "
                        + autoBidEntriesList.size() + " candidates.");
                List<AutoBidEntry> entriesSnapshotList = new ArrayList<>(autoBidEntriesList);
                List<AutoBidEntry> otherCompetitorsList = new ArrayList<>();
                for (AutoBidEntry entry : entriesSnapshotList) {
                    if (!entry.bidderId.equals(highestBidderId) || !(entry.maxBid == highestBidderLock)
                            || lastBidType != BidType.AUTO) {
                        otherCompetitorsList.add(entry);
                    }
                }
                if (otherCompetitorsList.isEmpty()) {
                    return;
                }
                AutoBidEntry winningEntry = entriesSnapshotList.get(0);
                for (AutoBidEntry entry : entriesSnapshotList) {
                    if (entry.maxBid > winningEntry.maxBid
                            || (entry.maxBid == winningEntry.maxBid
                                    && entry.registeredAt.isBefore(winningEntry.registeredAt))) {
                        winningEntry = entry;
                    }
                }
                long secondHighestBidAmount = -1;
                for (AutoBidEntry entry : entriesSnapshotList) {
                    if (!entry.bidderId.equals(winningEntry.bidderId) && entry.maxBid > secondHighestBidAmount) {
                        secondHighestBidAmount = entry.maxBid;
                    }
                }
                long bidAmount = 0;
                if (isFirstBid && entriesSnapshotList.size() <= 1) {
                    bidAmount = currentAuctionPrice;
                } else {
                    long basePrice = Math.max(secondHighestBidAmount, currentAuctionPrice);
                    bidAmount = Math.min(basePrice + increment, winningEntry.maxBid);
                }
                if (validateAutoBidSubmit(auctionEntity, bidAmount)) {

                    if (!userDAO.lockBidderBalance(winningEntry.bidderId, winningEntry.maxBid)) {
                        logger.log(Level.WARNING,
                                "Balance lock failed for bidderId: " + winningEntry.bidderId
                                        + " in auctionId: " + auctionId
                                        + ". Removing and retrying next candidate.");
                        autoBidEntriesList.remove(winningEntry);
                        notifyAutoBidStopped(winningEntry.bidderId);
                        continue;
                    }
                    SessionRegistry.getInstance().addUnsettledBalance(winningEntry.bidderId, winningEntry.maxBid);
                    try {
                        ConcurrentBidManager.getInstance().submitBid(
                                auctionEntity, winningEntry.bidderId, winningEntry.bidderUsername,
                                bidAmount, winningEntry.maxBid, BidType.AUTO);
                        bidCounts.computeIfAbsent(auctionId, k -> new ConcurrentHashMap<>())
                                .merge(winningEntry.bidderId, 1, Integer::sum);
                    } catch (Exception submitException) {
                        userDAO.unlockBidderBalance(winningEntry.bidderId, winningEntry.maxBid);
                        SessionRegistry.getInstance().addUnsettledBalance(winningEntry.bidderId, -winningEntry.maxBid);
                        logger.log(Level.WARNING,
                                "Bid submission failed for bidderId: " + winningEntry.bidderId
                                        + " in auctionId: " + auctionId
                                        + ". Removing and retrying next candidate.",
                                submitException);
                        autoBidEntriesList.remove(winningEntry);
                        notifyAutoBidStopped(winningEntry.bidderId);
                        continue;
                    }

                    List<AutoBidEntry> entriesToRemoveList = new ArrayList<>();
                    for (AutoBidEntry entry : entriesSnapshotList) {
                        if (!entry.bidderId.equals(winningEntry.bidderId)) {
                            entriesToRemoveList.add(entry);
                        }
                    }
                    autoBidEntriesList.removeAll(entriesToRemoveList);
                    entriesToRemoveList.forEach(entry -> notifyAutoBidStopped(entry.bidderId));
                    return;
                } else {
                    List<AutoBidEntry> toRemove = new ArrayList<>(entriesSnapshotList);
                    autoBidEntriesList.removeAll(toRemove);
                    toRemove.forEach(entry -> notifyAutoBidStopped(entry.bidderId));
                    return;
                }
            } // end while(true)
        } catch (Exception exception) {
            logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.trigger for auctionId: "
                            + auctionId,
                    exception);
            throw exception;
        }
    }

    /**
     * Retrieve all auto-bid registrations for a specific auction.
     */
    public List<AutoBidEntry> getRegistrations(String auctionId) throws Exception {
        try {
            List<AutoBidEntry> autoBidEntriesList = registrationsMap.getOrDefault(auctionId, List.of());
            return autoBidEntriesList;
        } catch (Exception exception) {
            logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.getRegistrations for auctionId: "
                            + auctionId,
                    exception);
            throw exception;
        }
    }

    /**
     * Clear all auto-bid registrations for a specific auction.
     * Đánh dấu auctionId vào cancelledAuctionIds TRƯỚC khi remove khỏi map,
     * để register() đang chạy đồng thời sẽ thấy flag và từ chối insert entry mới.
     * Dừng worker thread của auction này.
     */
    public void clearRegistrations(String auctionId) throws Exception {
        try {
            cancelledAuctionIds.add(auctionId);
            registrationsMap.remove(auctionId);
            bidCounts.remove(auctionId);
            Thread worker = autoBidWorkers.remove(auctionId);
            if (worker != null) {
                worker.interrupt();
                try {
                    worker.join(2000);
                } catch (InterruptedException ignored) {
                }
            }
            triggerQueues.remove(auctionId);
            logger.log(Level.INFO, "Auto-bid registrations and worker cleared for auctionId: " + auctionId);
        } catch (Exception exception) {
            logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.clearRegistrations for auctionId: "
                            + auctionId,
                    exception);
            throw exception;
        }
    }

    public boolean removeRegistration(String auctionId, String bidderId) throws Exception {
        try {
            List<AutoBidEntry> entries = registrationsMap.get(auctionId);
            if (entries != null) {
                boolean isRemoved = entries.removeIf(entry -> entry.bidderId.equals(bidderId));
                if (isRemoved) {
                    logger.log(Level.INFO,
                            "Auto-bid registration removed for bidderId: " + bidderId + " in auctionId: " + auctionId);
                } else {
                    logger.log(Level.INFO, "BidderId: " + bidderId + " not found in auctionId: " + auctionId);
                }
                return isRemoved;
            }
            logger.log(Level.INFO, "No auto-bid entries found for auctionId: " + auctionId);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] Unexpected error in AutoBidService.removeRegistration for bidderId: " + bidderId,
                    exception);
            throw exception;
        }
    }

    // --- PRIVATE METHODS ---
    /**
     * Khởi tạo worker thread cho auction nếu chưa có.
     * Mỗi auction có DUY NHẤT 1 worker thread xử lí trigger tuần tự.
     * Giống hệt ensureWorkerRunning() trong ConcurrentBidManager.
     */
    private void ensureAutoBidWorkerRunning(String auctionId) {
        triggerQueues.computeIfAbsent(auctionId, k -> new LinkedBlockingQueue<>());
        autoBidWorkers.computeIfAbsent(auctionId, k -> {
            Thread worker = new Thread(() -> runAutoBidWorker(auctionId));
            worker.setDaemon(true);
            worker.setName("autobid-worker-" + auctionId);
            worker.start();
            logger.log(Level.INFO,
                    "AutoBid worker thread started for auctionId: " + auctionId);
            return worker;
        });
    }

    /**
     * Vòng lặp chính của worker thread cho mỗi auction.
     * Giống hệt runWorker() trong ConcurrentBidManager.
     *
     * 1. Block chờ tín hiệu từ triggerQueue (queue.take())
     * 2. Drain tất cả tín hiệu tích lũy (gộp nhiều request thành 1 lần xử lí)
     * 3. Lấy trạng thái auction MỚI NHẤT từ AuctionRegistry
     * 4. Chạy trigger() để xử lí
     * 5. Quay lại bước 1 chờ tín hiệu tiếp
     */
    private void runAutoBidWorker(String auctionId) {
        try {
            BlockingQueue<String> queue = triggerQueues.get(auctionId);
            if (queue == null)
                return;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    queue.take();
                    queue.clear();
                    trigger(auctionId);
                } catch (InterruptedException e) {
                    logger.log(Level.INFO,
                            "AutoBid worker interrupted for auctionId: " + auctionId);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.log(Level.SEVERE,
                            "Error in AutoBid worker for auctionId: " + auctionId, e);
                }
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE,
                    "Unexpected error in AutoBid worker thread for auctionId: " + auctionId, exception);
        }
    }

    /**
     * Notify a bidder that their auto-bid has been stopped.
     */
    private void notifyAutoBidStopped(String bidderId) {
        java.io.PrintWriter writer = sessionRegistry.getWriter(bidderId);
        if (writer != null) {
            try {
                synchronized (writer) {
                    logger.log(Level.INFO, ("stop autobid bidder: " + bidderId));
                    writer.println(JsonUtils.toJson(
                            ClientMessage.push("AUTO_BID_STOPPED",
                                    java.util.Map.of("message",
                                            "Your auto-bid has been deactivated because the current price exceeded your maximum threshold."))));
                }
            } catch (Exception exception) {
                logger.log(Level.WARNING,
                        "Notification transmission failure: Unable to deliver AUTO_BID_STOPPED to bidderId: "
                                + bidderId,
                        exception);
            }
        }
    }

    private void validateAutoBidRequest(AutoBidRequest autoBidRequest, String bidderId, String bidderUsername)
            throws ServiceException {
        if (autoBidRequest == null) {
            throw new ServiceException(ErrorCode.AUTO_BID_VALIDATION_ERROR,
                    "The auto-bid request object cannot be null.");
        }
        if (bidderId == null || bidderId.isBlank()) {
            throw new ServiceException(ErrorCode.MISSING_BIDDER_ID,
                    "Operation failed: The bidder identifier is mandatory for auto-bid registration.");
        }
        if (bidderUsername == null || bidderUsername.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_DATA, "Validation error: Bidder username is required.");
        }
        if (autoBidRequest.getAuctionId() == null || autoBidRequest.getAuctionId().isBlank()) {
            throw new ServiceException(ErrorCode.MISSING_AUCTION_ID,
                    "Validation error: Auction identification is missing.");
        }
        if (!BidValidator.isPositiveBid(autoBidRequest.getMaxBid())) {
            throw new ServiceException(ErrorCode.INVALID_BID_AMOUNT,
                    "The maximum auto-bid amount must be greater than zero.");
        }
    }

    private boolean validateAutoBidSubmit(Auction auction, long bidAmount)
            throws ServiceException {
        if (auction == null) {
            throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND,
                    "Resource not found: The specified auction does not exist.");
        }
        long minIncrement = auction.getAuctionConfig().getMinIncrement();
        BidTransaction lastBid = auction.getLastBidTransaction();
        if (lastBid == null) {
            if (bidAmount < auction.getAuctionConfig().getStartPrice()) {

                logger.log(Level.INFO, "Auto-bid submission rejected: bidAmount " + bidAmount +
                        "Bid must be at least the starting price of " + auction.getAuctionConfig().getStartPrice());
                return false;
            }
        } else {
            if (bidAmount - auction.getCurrentPrice() < minIncrement) {
                logger.log(Level.INFO, "Auto-bid submission rejected: bidAmount " + bidAmount +
                        "The bid increment is lower than the required minimum of " + minIncrement);
                return false;
            }
        }
        return true;
    }

    private void validateAutoBidTerms(Auction auction, AutoBidRequest autoBidRequest, String bidderId)
            throws ServiceException {
        if (auction == null) {
            throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND,
                    "Resource not found: The specified auction does not exist.");
        }
        if (bidderId.equals(auction.getSellerId())) {
            throw new ServiceException(ErrorCode.AUTO_SELLER_CANNOT_AUTOBID,
                    "Sellers are prohibited from registering auto-bids for their own auctions.");
        }
        long bidAmount = autoBidRequest.getMaxBid();
        long minIncrement = auction.getAuctionConfig().getMinIncrement();
        BidTransaction lastBid = auction.getLastBidTransaction();
        if (lastBid == null) {
            // Chưa có bid — chỉ cần >= startPrice
            if (bidAmount < auction.getAuctionConfig().getStartPrice()) {
                throw new ServiceException(ErrorCode.INCREMENT_TOO_LOW,
                        "Bid must be at least the starting price of " + auction.getAuctionConfig().getStartPrice());
            }
        } else {
            if (bidAmount - auction.getCurrentPrice() < minIncrement) {
                throw new ServiceException(ErrorCode.INCREMENT_TOO_LOW,
                        "The bid increment is lower than the required minimum of " + minIncrement);
            }
        }
    }

    private void validateBidderAccount(User bidder, long bidAmount, Auction auction)
            throws ServiceException {
        if (!(bidder instanceof Bidder bidderAccount)) {
            throw new ServiceException(ErrorCode.NOT_BIDDER, "...");
        }

        BidTransaction lastBid = auction.getLastBidTransaction();
        long alreadyLocked = 0;
        if (lastBid != null && lastBid.getBidderId().equals(bidderAccount.getId())) {
            alreadyLocked = lastBid.getLockedBalance();
        }

        long netRequired = bidAmount - alreadyLocked;
        if (bidderAccount.getAvailableBalance() < netRequired) {
            throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE,
                    "Insufficient balance. Need: " + netRequired
                            + ", available: " + bidderAccount.getAvailableBalance());
        }
    }

    /**
     * Represents a single auto-bid entry for an auction.
     * Stores bidder information, bid limits, and registration timestamp.
     */
    public static class AutoBidEntry {
        public final String bidderId;
        public final String bidderUsername;
        public final long maxBid;
        public final LocalDateTime registeredAt;

        public AutoBidEntry(String bidderId, String bidderUsername, long maxBid) {
            this.bidderId = bidderId;
            this.bidderUsername = bidderUsername;
            this.maxBid = maxBid;
            this.registeredAt = LocalDateTime.now();
        }

        public String getBidderId() {
            return bidderId;
        }
    }
}
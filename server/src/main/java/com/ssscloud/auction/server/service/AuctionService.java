package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.enums.AppConstant;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.model.auction.BidTransaction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.common.model.user.Bidder;
import com.ssscloud.auction.common.model.user.Seller;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.request.CreateAuctionRequest;
import com.ssscloud.auction.common.payload.response.DTO.AuctionDTO;
import com.ssscloud.auction.common.payload.response.DTO.ItemDTO;
import com.ssscloud.auction.common.payload.response.DTO.UserDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.QueryDAO;
import com.ssscloud.auction.server.dao.QueryDAO.AuctionScheduleInfo;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.factory.ItemDTOFactory;
import com.ssscloud.auction.server.factory.ItemFactory;
import com.ssscloud.auction.server.util.AuctionRegistry;
import com.ssscloud.auction.server.util.SessionRegistry;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuctionService manages the business logic for auction lifecycle operations.
 *
 * scheduleClose pipeline:
 *   pool1 (ScheduledThreadPool) — tác vụ nhanh, không block:
 *     1. Đọc endTime từ RAM (không query DB)
 *     2. Reschedule nếu endTime đã bị anti-sniping extend
 *     3. Atomic status transition RAM → FINISHED
 *     4. softClose() — chặn bid mới
 *     5. ChangeManager.notify() — push snapshot xuống client ngay lập tức
 *     6. Submit IoTask sang pool2
 *
 *   pool2 (FixedThreadPool) — tác vụ I/O nặng:
 *     1. auctionDAO.updateStatus(FINISHED)
 *     2. AuctionRegistry.remove()
 *     3. ConcurrentBidManager.shutdown()
 *     4. autoBidService.clearRegistrations()
 *     5. settleAuctionBalances()
 *     6. notificationService.notifyAuctionEnded()
 *
 * Anti-sniping: không giới hạn lần — reschedule chỉ check RAM, không tốn DB round-trip.
 * endTime mới được ghi vào DB bởi ConcurrentBidManager (processTask), không phải ở đây.
 */
public class AuctionService {
    private static final Logger logger = Logger.getLogger(AuctionService.class.getName()); // Logging Standards: Declared first
    
    private final AuctionDAO auctionDAO;
    private final QueryDAO queryDAO;
    private final UserDAO userDAO; 
    private final UserService userService;
    private final ItemService itemService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final NotificationService notificationService;
    private final AutoBidService autoBidService;

    private final ScheduledExecutorService pool1 = Executors.newScheduledThreadPool(2);
    private final ExecutorService pool2 = Executors.newFixedThreadPool(4);
     
    

    // Sửa constructor
    public AuctionService(AuctionDAO auctionDAO, QueryDAO queryDAO, UserDAO userDAO, UserService userService,
                        ItemService itemService, NotificationService notificationService,
                        AutoBidService autoBidService) {
        this.auctionDAO          = auctionDAO;
        this.queryDAO            = queryDAO;
        this.userDAO             = userDAO;
        this.userService         = userService;
        this.itemService         = itemService;
        this.notificationService = notificationService;
        this.autoBidService      = autoBidService;
    }

    // --- PUBLIC METHODS ---

    public AuctionDTO createAuction(CreateAuctionRequest request, String sellerId) throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Initiating auction creation process for sellerId: " + sellerId);
            
            validateCreateAuctionRequest(request, sellerId); // Step 1: Validate input data
    
            Item item = createItemFromFactory(request, sellerId); // Step 2: Create item using factory
            
            itemService.saveItem(item); // Step 3: Persist item data; throws ServiceExceptions if failed
    
            LocalDateTime startTime = (request.getStartTime() != null)
                    ? request.getStartTime()
                    : LocalDateTime.now();
    
            AuctionConfig auctionConfig = new AuctionConfig(
                    request.getName(),
                    request.getStartPrice(),
                    request.getMinIncrement(),
                    startTime,
                    request.getEndTime(),
                    AppConstant.DEFAULT_EXTENSION_SECONDS.getValue()); // Default anti-sniping extension duration
    
            Auction auction = new Auction(auctionConfig, AuctionStatus.OPEN, sellerId, item.getId());
            
            boolean isAuctionSaved = auctionDAO.saveAuction(auction);
            if (!isAuctionSaved) {
                logger.log(Level.SEVERE, "Critical failure: Unable to persist auction record for name: " + auctionConfig.getName());
                throw new ServiceException(ErrorCode.AUCTION_CREATION_FAILED, "Failed to persist the auction to the database: " + auctionConfig.getName());
            }
            scheduleClose(auctionConfig.getId(), auctionConfig.getEndTime());
        

            logger.log(Level.INFO, "Auction successfully created and registered with ID: {0}", auctionConfig.getId());
    
            UserDTO sellerDto = userService.getByUserId(sellerId);
            ItemDTO itemDto = ItemDTOFactory.toDto(item);
            
            return auction.toAuctionDto(sellerDto, itemDto);
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionService.createAuction", exception);
            throw exception;
        }
    }

    public AuctionDTO getAuctionById(String auctionId) throws ServiceException, Exception {
    try {
        logger.log(Level.INFO, "Retrieving auction details for auctionId: " + auctionId);
        validateAuctionId(auctionId);

        // Thử lấy từ registry trước (nếu đang live)
        AuctionRegistry registry = AuctionRegistry.getInstance();
        Auction auction = (registry != null) ? registry.get(auctionId) : null;

        // Nếu không có trong registry (auction đã ended/won), query DB
        if (auction == null) {
            auction = auctionDAO.findByAuctionId(auctionId);
        }

        if (auction == null) {
            throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND,
                    "Auction not found: " + auctionId);
        }

        UserDTO sellerDto = userService.getByUserId(auction.getSellerId());
        ItemDTO itemDto = itemService.getItemById(auction.getItemId());

        if (itemDto == null) {
            throw new ServiceException(ErrorCode.ITEM_NOT_FOUND,
                    "Item not found for auction: " + auctionId);
        }

        return auction.toAuctionDto(sellerDto, itemDto);
    } catch (ServiceException serviceException) {
        throw serviceException;
    } catch (Exception exception) {
        logger.log(Level.SEVERE,
                "[SYSTEM_FAILURE] Unexpected system error in AuctionService.getAuctionById: "
                        + exception.getMessage(), exception);
        throw exception;
    }
}


    public void scheduleClose(Auction auction) throws Exception {
        scheduleClose(
                auction.getAuctionConfig().getId(),
                auction.getAuctionConfig().getEndTime());
    }
    //pool 1: tác vụ nhanh, không block — chỉ schedule lại nếu endTime đã bị anti-sniping extend
    public void scheduleClose(String auctionId, LocalDateTime endTime) {
        if (endTime == null) {
            logger.log(Level.WARNING,
                    "scheduleClose skipped because endTime is null for auctionId: " + auctionId);
            return;
        }

        long delayMilliseconds = Duration.between(LocalDateTime.now(), endTime).toMillis();
        if (delayMilliseconds < 0) {
            delayMilliseconds = 0;
        }

        pool1.schedule(() -> {
            try {
                // Lấy live auction từ Registry — không query DB
                Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);
                if (auction == null) {
                    logger.log(Level.INFO,
                            "[Pool1] scheduleClose skipped — auction not in registry: " + auctionId);
                    return;
                }

                // Anti-sniping: check endTime mới nhất từ RAM
                // ConcurrentBidManager.processTask() đã update auctionConfig.endTime khi extend
                LocalDateTime latestEndTime = auction.getAuctionConfig().getEndTime();
                if (LocalDateTime.now().isBefore(latestEndTime)) {
                    // endTime đã bị extend — reschedule, không làm gì thêm
                    logger.log(Level.INFO,
                            "[Pool1] Anti-sniping extended — rescheduling auctionId: " + auctionId
                                    + " to " + latestEndTime);
                    scheduleClose(auctionId, latestEndTime);
                    return;
                }
                //1. Atomic status transition RAM → FINISHED
                synchronized (auction) {
                    if (!auction.getStatus().isActive()) {
                        logger.log(Level.INFO,
                                "[Pool1] scheduleClose skipped — already terminal: " + auctionId);
                        return;
                    }
                    auction.finish(); 
                }
                //2. softClose() — chặn bid mới, vẫn giữ auction trong registry để push snapshot về client
                ConcurrentBidManager.getInstance().softClose(auctionId);
                ChangeManager.getInstance().notify(auction); //3. ChangeManager.notify() — push snapshot xuống client ngay lập tức
                logger.log(Level.INFO, "[Pool1] Snapshot notified, submitting IO tasks for auctionId: " + auctionId);

                final Auction snapshot = auction;
                pool2.execute(() -> runCloseIo(auctionId, snapshot)); //4. Submit IoTask sang pool2
                

            } catch (ServiceException serviceException) {
                logger.log(Level.WARNING,
                        "Business logic error in scheduled auction closure for auctionId: " + auctionId,
                        serviceException);
            } catch (Exception exception) {
                logger.log(Level.SEVERE,
                        "[SYSTEM_FAILURE] Unexpected system error during scheduled auction closure for auctionId: "
                                + auctionId,
                        exception);
            }
        }, delayMilliseconds, TimeUnit.MILLISECONDS);
    }

    private void runCloseIo(String auctionId, Auction auction){
        try {
            boolean updated = auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
            if (!updated) {
                // Admin cancel hoặc safety-net đã close trước — bỏ qua
                logger.log(Level.INFO,
                        "[Pool2] DB update skipped — already terminal in DB: " + auctionId);
                return;
            }
            // Bước 2: Cleanup in-memory state
            AuctionRegistry.getInstance().remove(auctionId);
            autoBidService.clearRegistrations(auctionId);

            ConcurrentBidManager.getInstance().shutdown(auctionId); // Đảm bảo không có task nào đang chạy hoặc sẽ chạy cho auction này nữa
            settleAuctionBalances(auction); 
            notificationService.notifyAuctionEnded(auction); 
            logger.log(Level.INFO, "[Pool2] Auction fully closed: " + auctionId);

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "[Pool2] runCloseIo failed for auctionId: " + auctionId, e);


        }
    }
    


    public boolean closeAuctionById(String auctionId) throws ServiceException, Exception {
        validateAuctionId(auctionId);

        Auction auction = AuctionRegistry.getInstance().get(auctionId);
        if (auction == null) {
            auction = auctionDAO.findByAuctionId(auctionId);
        }
        if (auction == null) {
            throw new ServiceException(
                    ErrorCode.AUCTION_NOT_FOUND,
                    "Auction not found with identifier: " + auctionId);
        }

        return closeAuction(auction);
    }

    public boolean closeAuction(Auction auction) throws ServiceException, Exception {
        if (auction == null) {
            throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "Auction cannot be null.");
        }

        String auctionId = auction.getAuctionConfig().getId();
        Auction liveAuction = AuctionRegistry.getInstance().get(auctionId);
        Auction targetAuction = (liveAuction != null) ? liveAuction : auction;

        synchronized (targetAuction) {
            AuctionStatus currentStatus = targetAuction.getStatus();
            if (currentStatus != AuctionStatus.OPEN && currentStatus != AuctionStatus.RUNNING) {
                logger.log(Level.INFO,
                        "closeAuction skipped - auction already in terminal state: " + auctionId);
                return false;
            }
        }

        if (!auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED)) {
            logger.log(Level.INFO,
                    "closeAuction skipped - DB status is no longer active: " + auctionId);
            return false;
        }

        try {
            synchronized (targetAuction) {
                targetAuction.finish();
            }

            AuctionRegistry.getInstance().remove(auctionId);
            ConcurrentBidManager.getInstance().shutdown(auctionId);
            autoBidService.clearRegistrations(auctionId);
            settleAuctionBalances(targetAuction);

            ChangeManager.getInstance().notify(targetAuction);
            notificationService.notifyAuctionEnded(targetAuction);

            logger.log(Level.INFO, "Auction has been concluded: " + auctionId);
            return true;
        } catch (Exception exception) {
            logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] closeAuction failed after status transition for auctionId: "
                            + auctionId,
                    exception);
            throw exception;
        }
    }
    
    public void startAuctionCloser() {
        pool1.scheduleAtFixedRate(() -> {
            try {
                List<AuctionScheduleInfo> overdueList = queryDAO.findOverdueScheduleInfos(LocalDateTime.now());

                for (AuctionScheduleInfo scheduleInfo : overdueList) {
                    String auctionId = scheduleInfo.getAuctionId();
                    Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);

                    if (auction != null) {
                        // Auction còn trong registry chạy full pipeline giống scheduleClose
                        synchronized (auction) {
                            if (!auction.getStatus().isActive()) continue;
                            auction.finish();
                        }
                        ConcurrentBidManager.getInstance().softClose(auctionId);
                        ChangeManager.getInstance().notify(auction);
 
                        final Auction snapshot = auction;
                        pool2.execute(() -> runCloseIo(auctionId, snapshot));
 
                    } else {
                        // Auction không có trong RAM (không ai subscribe) →
                        // chỉ cần update DB và settle, không cần notify RAM
                        pool2.execute(() -> {
                            try {
                                boolean updated = auctionDAO.updateStatus(
                                        auctionId, AuctionStatus.FINISHED);
                                if (!updated) return;
 
                                Auction loaded = auctionDAO.findByAuctionId(auctionId);
                                if (loaded != null) {
                                    settleAuctionBalances(loaded);
                                    notificationService.notifyAuctionEnded(loaded);
                                }
                                logger.log(Level.INFO,
                                        "[Safety-net] Closed offline auctionId: " + auctionId);
                            } catch (Exception e) {
                                logger.log(Level.SEVERE,
                                        "[Safety-net] Failed to close auctionId: " + auctionId, e);
                            }
                        });
                    }
                }
                
            } catch (Exception exception) {
                logger.log(Level.SEVERE,
                        "[AuctionCloser] Maintenance task encountered a scheduled check failure.",
                        exception);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    //Cleanup

    public void shutdownScheduler() {
        logger.info("[Shutdown] Stopping pool1 (scheduler)...");
        pool1.shutdown();
        try {
            if (!pool1.awaitTermination(10, TimeUnit.SECONDS)) {
                pool1.shutdownNow();
                logger.warning("[Shutdown] pool1 did not terminate cleanly.");
            }
        } catch (InterruptedException e) {
            pool1.shutdownNow();
            Thread.currentThread().interrupt();
        }
 
        logger.info("[Shutdown] Draining pool2 (IO worker)...");
        pool2.shutdown();
        try {
            if (!pool2.awaitTermination(15, TimeUnit.SECONDS)) {
                pool2.shutdownNow();
                logger.warning("[Shutdown] pool2 did not terminate cleanly.");
            }
        } catch (InterruptedException e) {
            pool2.shutdownNow();
            Thread.currentThread().interrupt();
        }
 
        logger.info("[Shutdown] AuctionService pools stopped.");
    }



//HELPERS
    public Auction ensureLiveAuctionLoaded(String auctionId) throws ServiceException, Exception {
        validateAuctionId(auctionId);

        Auction auction = AuctionRegistry.getInstance().get(auctionId);
        if (auction != null) {
            if (auction.getStatus().isActive() && !auction.isExpired()) {
                return auction;
            }
            if (auction.getStatus().isActive() && auction.isExpired()) {
                closeAuctionById(auctionId);
            }
            throw new ServiceException(ErrorCode.AUCTION_CLOSED, "Auction has already concluded.");
        }

        auction = auctionDAO.findByAuctionId(auctionId);
        if (auction == null) {
            throw new ServiceException(
                    ErrorCode.AUCTION_NOT_FOUND,
                    "Auction not found with identifier: " + auctionId);
        }
        if (!auction.getStatus().isActive()) {
            throw new ServiceException(ErrorCode.AUCTION_CLOSED, "Auction has already concluded.");
        }
        if (auction.isExpired()) {
            closeAuctionById(auctionId);
            throw new ServiceException(ErrorCode.AUCTION_CLOSED, "Auction has already concluded.");
        }

        AuctionRegistry.getInstance().registerIfAbsent(auction);
        return AuctionRegistry.getInstance().get(auctionId);
    }

    /**
     * Public wrapper để AuctionSocketServer (safety-net + recovery) gọi settle.
     */
    public void settleAuctionBalancesPublic(Auction auction) {
        settleAuctionBalances(auction);
    }

    private void settleAuctionBalances(Auction auction) {
        BidTransaction lastBid = auction.getLastBidTransaction();
        String sellerId   = auction.getSellerId();
        
        String auctionId  = auction.getAuctionConfig().getId();
 
        if (lastBid == null) {
            logger.log(Level.INFO,
                    "No bids placed for auctionId: {0} — skipping balance settlement.", auctionId);
            return;
        }

        long lockToRelease = lastBid.getLockedBalance();
        long finalPrice = lastBid.getBidAmount();
        String winnerId = lastBid.getBidderId();
 
        try {
 
            // Trừ finalPrice khỏi account, trừ lockToRelease khỏi locked
            boolean winnerSettled = userDAO.settleWinnerBalance(winnerId, finalPrice, lockToRelease);
            if (!winnerSettled) {
                logger.log(Level.WARNING,
                        "settleWinnerBalance: no rows affected — locked insufficient? "
                                + "winnerId={0}, finalPrice={1}",
                        new Object[]{winnerId, finalPrice});
            }
 
            User winnerUser = userDAO.findById(winnerId);
            if (winnerUser instanceof Bidder bidder) {
                long newBalance  = bidder.getAccountBalance();
                long newLocked   = bidder.getLockedBalance(); // = 0 sau settle thành công
 
                // Sync session — dùng set (không phải add) để đảm bảo khớp DB
                SessionRegistry.getInstance().setUnsettledBalance(winnerId, newLocked);
 
                // Push về client nếu đang online
                pushBalanceUpdate(winnerId, newBalance);
                pushUnsettledUpdate(winnerId, newLocked);
 
                logger.log(Level.INFO,
                        "Winner settled: winnerId={0}, deducted={1}, newBalance={2}, newLocked={3}",
                        new Object[]{winnerId, finalPrice, newBalance, newLocked});
            } else {
                logger.log(Level.WARNING,
                        "settleAuctionBalances: winnerId={0} not found or not a Bidder after settle.",
                        winnerId);
            }
 
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] Failed to settle winner balance. "
                            + "auctionId=" + auctionId, e);
        }
 
        try {
            boolean sellerSettled = userDAO.settleSellerBalance(sellerId, finalPrice);
            if (!sellerSettled) {
                logger.log(Level.WARNING,
                        "settleSellerBalance: no rows affected — pending insufficient? "
                                + "sellerId={0}, finalPrice={1}",
                        new Object[]{sellerId, finalPrice});
            }
 
            User sellerUser = userDAO.findById(sellerId);
            if (sellerUser instanceof Seller seller) {
                long newBalance  = seller.getAccountBalance();
                long newPending  = seller.getPendingBalance();
                SessionRegistry.getInstance().setUnsettledBalance(sellerId, newPending);
 
                pushBalanceUpdate(sellerId, newBalance);
                pushUnsettledUpdate(sellerId, newPending);
 
                logger.log(Level.INFO,
                        "Seller settled: sellerId={0}, received={1}, newBalance={2}, newPending={3}",
                        new Object[]{sellerId, finalPrice, newBalance, newPending});
            } else {
                logger.log(Level.WARNING,
                        "settleAuctionBalances: sellerId={0} not found or not a Seller after settle.",
                        sellerId);
            }
 
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] Failed to settle seller balance. "
                            + "auctionId=" + auctionId + ", sellerId=" + sellerId
                            + ", finalPrice=" + finalPrice, e);
        }
    }
    /**
     * Push số dư tài khoản mới về client (nếu đang online).
     * Client dùng để cập nhật label "Balance" trên MainLayoutController.
     */

    private void pushBalanceUpdate(String userId, long newBalance) {
        PrintWriter writer = SessionRegistry.getInstance().getWriter(userId);
        if (writer == null) return; // offline — bỏ qua, client sẽ query lại khi login lần tiếp
        try {
            synchronized (writer) {
                writer.println(JsonUtils.toJson(
                        ClientMessage.push("BALANCE_UPDATE", newBalance)));
                writer.flush();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "Failed to push BALANCE_UPDATE to userId: " + userId, e);
        }
    }
    private void pushUnsettledUpdate(String userId, long unsettled) {
        PrintWriter writer = SessionRegistry.getInstance().getWriter(userId);
        if (writer == null) return;
        try {
            synchronized (writer) {
                writer.println(JsonUtils.toJson(
                        ClientMessage.push("UNSETTLED_UPDATE", unsettled)));
                writer.flush();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "Failed to push UNSETTLED_UPDATE to userId: " + userId, e);
        }
    }
    // --- PRIVATE HELPERS ---

    private void validateCreateAuctionRequest(CreateAuctionRequest request, String sellerId) throws ServiceException {
        if (request == null) {
            throw new ServiceException(ErrorCode.INVALID_DATA, "The auction request payload cannot be null.");
        }
        if (sellerId == null || sellerId.isEmpty()) {
            throw new ServiceException(ErrorCode.MISSING_BIDDER_ID, "The seller identifier is required for auction creation.");
        }
    }

    private void validateAuctionId(String auctionId) throws ServiceException {
        if (auctionId == null || auctionId.isBlank()) {
            throw new ServiceException(ErrorCode.MISSING_AUCTION_ID, "The auction identification is required to perform this operation.");
        }
    }

    private Item createItemFromFactory(CreateAuctionRequest request, String sellerId) throws ServiceException, Exception {
        try {
            return ItemFactory.createItem(request, sellerId);
        } catch (IllegalArgumentException illegalArgumentException) {
            logger.log(Level.SEVERE, "ItemFactory encountered a creation failure: " + illegalArgumentException.getMessage(), illegalArgumentException);
            throw new ServiceException(ErrorCode.INVALID_ITEM_DATA, "Validation failure: The provided item data is invalid: " + illegalArgumentException.getMessage());
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error during item creation via ItemFactory: " + exception.getMessage(), exception);
            throw exception; // Naming Convention: English log and rethrow
        }
    }
}
package com.ssscloud.auction.server.service;

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
import com.ssscloud.auction.common.payload.response.DTO.BidDTO;
import com.ssscloud.auction.common.payload.response.DTO.BidderDisplayDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuctionService manages the business logic for auction lifecycle operations.
 *
 * The createAuction execution flow:
 * 1. ItemFactory.create(request, sellerId) -> Item (Art/Vehicle/Electronic)
 * 2. ItemDAO.save*(item) -> Persist item and subtype records
 * 3. Construct AuctionConfig and initialize Auction with OPEN status
 * 4. AuctionDAO.saveAuction(auction) -> Persist auction record
 * 5. scheduleClose(auction) -> Initialize scheduler to transition OPEN to FINISHED status
 * 6. Return AuctionDTO.
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
                    36); // Default anti-sniping extension duration
    
            Auction auction = new Auction(auctionConfig, AuctionStatus.OPEN, sellerId, item.getId());
            
            boolean isAuctionSaved = auctionDAO.saveAuction(auction);
            if (!isAuctionSaved) {
                logger.log(Level.SEVERE, "Critical failure: Unable to persist auction record for name: " + auctionConfig.getName());
                throw new ServiceException(ErrorCode.AUCTION_CREATION_FAILED, "Failed to persist the auction to the database: " + auctionConfig.getName());
            }
    
            AuctionRegistry.getInstance().registerIfAbsent(auction); 
    
            scheduleClose(auction); 
            logger.log(Level.INFO, "Auction successfully created and registered with ID: {0}", auctionConfig.getId());
    
            UserDTO sellerDto = userService.getByUserId(sellerId);
            ItemDTO itemDto = ItemDTOFactory.toDto(item);
            
            return toAuctionDto(auction, sellerDto, itemDto);
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
            
            Auction auction = auctionDAO.findByAuctionId(auctionId);
            if (auction == null) {
                throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "Resource not found: The auction with ID " + auctionId + " does not exist.");
            }
    
            UserDTO sellerDto = userService.getByUserId(auction.getSellerId());
            ItemDTO itemDto = itemService.getItemById(auction.getItemId());
            
            if (itemDto == null) {
                throw new ServiceException(ErrorCode.ITEM_NOT_FOUND, "Data integrity error: The item associated with auction " + auctionId + " was not found.");
            }
    
            return toAuctionDto(auction, sellerDto, itemDto);
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionService.getAuctionById: " + exception.getMessage(), exception);
            throw exception;
        }
    }


    public void scheduleClose(Auction auction) throws Exception {
        scheduleClose(
                auction.getAuctionConfig().getId(),
                auction.getAuctionConfig().getEndTime());
    }

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

        scheduler.schedule(() -> {
            try {
                AuctionScheduleInfo scheduleInfo = queryDAO.findActiveScheduleInfoById(auctionId);
                if (scheduleInfo == null) {
                    logger.log(Level.INFO,
                            "scheduleClose skipped - auction is no longer active: " + auctionId);
                    return;
                }

                LocalDateTime latestEndTime = scheduleInfo.getEndTime();
                if (latestEndTime != null && LocalDateTime.now().isBefore(latestEndTime)) {
                    scheduleClose(auctionId, latestEndTime);
                    return;
                }

                closeAuctionById(auctionId);
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
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<AuctionScheduleInfo> overdueScheduleInfoList =
                        queryDAO.findOverdueScheduleInfos(LocalDateTime.now());

                for (AuctionScheduleInfo scheduleInfo : overdueScheduleInfoList) {
                    String auctionId = scheduleInfo.getAuctionId();
                    if (closeAuctionById(auctionId)) {
                        logger.log(Level.INFO,
                                "[AuctionCloser] Safety-net finalized auctionId: " + auctionId);
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
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
        String winnerId   = auction.getHighestBidderId();
        String sellerId   = auction.getSellerId();
        long   finalPrice = auction.getCurrentPrice();
        String auctionId  = auction.getAuctionConfig().getId();
 
        if (winnerId == null || finalPrice <= 0) {
            logger.log(Level.INFO,
                    "No bids placed for auctionId: {0} — skipping balance settlement.", auctionId);
            return;
        }
 
        try {
            // Atomic: account_balance -= finalPrice, locked_balance -= finalPrice
            boolean winnerSettled = userDAO.settleWinnerBalance(winnerId, finalPrice, finalPrice);
            if (!winnerSettled) {
                logger.log(Level.WARNING,
                        "settleWinnerBalance: no rows affected — locked insufficient? "
                                + "winnerId={0}, finalPrice={1}",
                        new Object[]{winnerId, finalPrice});
            }
 
            // Query lại DB sau settle để lấy giá trị chính xác
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
                            + "auctionId=" + auctionId + ", winnerId=" + winnerId
                            + ", finalPrice=" + finalPrice, e);
        }
 
        try {
            // Atomic: account_balance += finalPrice, pending_balance -= finalPrice
            boolean sellerSettled = userDAO.settleSellerBalance(sellerId, finalPrice);
            if (!sellerSettled) {
                logger.log(Level.WARNING,
                        "settleSellerBalance: no rows affected — pending insufficient? "
                                + "sellerId={0}, finalPrice={1}",
                        new Object[]{sellerId, finalPrice});
            }
 
            // Query lại DB sau settle để lấy giá trị chính xác
            User sellerUser = userDAO.findById(sellerId);
            if (sellerUser instanceof Seller seller) {
                long newBalance  = seller.getAccountBalance();
                long newPending  = seller.getPendingBalance(); // = 0 sau settle thành công
 
                // Sync session
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

    private AuctionDTO toAuctionDto(Auction auction, UserDTO sellerDto, ItemDTO itemDto) {
        AuctionDTO auctionDto = new AuctionDTO();

        auctionDto.setId(auction.getAuctionConfig().getId());
        auctionDto.setName(auction.getAuctionConfig().getName());
        auctionDto.setStartPrice(auction.getAuctionConfig().getStartPrice());
        auctionDto.setMinIncrement(auction.getAuctionConfig().getMinIncrement());
        auctionDto.setStartTime(auction.getAuctionConfig().getStartTime());
        auctionDto.setEndTime(auction.getAuctionConfig().getEndTime());
        auctionDto.setStatus(auction.getStatus());

        List <BidTransaction> bidTransactions = auction.getBidTransaction();
        List <BidDTO> bidDto = new ArrayList<>();
        if (bidTransactions != null && !bidTransactions.isEmpty())
            for (BidTransaction bidTransaction : bidTransactions) {
                try {
                    bidDto.add(toBidDto(bidTransaction));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        auctionDto.setBidDto(bidDto);
        auctionDto.setSellerDTO(sellerDto);
        auctionDto.setItemDTO(itemDto);
        return auctionDto;
    }

    public BidDTO toBidDto(BidTransaction bidTransaction) throws Exception {
        try {
            BidDTO bidDto = new BidDTO();
            bidDto.setAuctionId(bidTransaction.getAuctionId());
            bidDto.setBidderId(bidTransaction.getBidderId());
            bidDto.setBidderUsername(bidTransaction.getBidderUsername());
            bidDto.setBidAmount(bidTransaction.getBidAmount());
            bidDto.setLockedBalance(bidTransaction.getLockedBalance());
            bidDto.setBidTime(bidTransaction.getBidTime());
            bidDto.setBidType(bidTransaction.getType().name());
            return bidDto;
        } catch (Exception exception) {
            logger.log(Level.SEVERE,
                    "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.toBidDto: " + exception.getMessage(),
                    exception);
            throw exception;
        }
    }

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
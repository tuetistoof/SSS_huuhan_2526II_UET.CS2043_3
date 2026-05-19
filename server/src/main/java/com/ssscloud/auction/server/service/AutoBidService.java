package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.common.observer.ChangeManager;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AutoBidService manages the automated bidding logic for auction participants.
 * It maintains registrations and bid frequencies per auction room.
 */
public class AutoBidService {
    private static final Logger logger = Logger.getLogger(AutoBidService.class.getName()); // Logging Standards: Declared first

    // --- ATTRIBUTES ---

    private final Map<String, List<AutoBidEntry>> registrationsMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AtomicInteger>> bidCountsMap = new ConcurrentHashMap<>();
    // Race-condition guard: auctionId được đánh dấu ngay khi clearRegistrations() chạy,
    // để register() gọi sau đó không thể tạo entry mới cho auction đã bị cancel.
    private final Set<String> cancelledAuctionIds = ConcurrentHashMap.newKeySet();
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final SessionRegistry sessionRegistry = SessionRegistry.getInstance();

    // --- CONSTRUCTOR ---

    public AutoBidService(AuctionDAO auctionDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
    }

    // --- PUBLIC METHODS ---

    /**
     * Register an auto-bid request for a bidder in a specific auction.
     * Validates request integrity, auction status, and bidder financial capacity.
     */
    public void register(AutoBidRequest autoBidRequest, String bidderId, String bidderUsername) throws ServiceException, Exception {
        try {
            validateAutoBidRequest(autoBidRequest, bidderId, bidderUsername);
            logger.log(Level.INFO, "Initiating auto-bid registration for auctionId: " + autoBidRequest.getAuctionId() + " for bidderId: " + bidderId);
            
            
            Auction auctionEntity = retrieveAndValidateAuction(autoBidRequest.getAuctionId());
            if (auctionEntity.getStatus().isEnded() || auctionEntity.isExpired() || !auctionEntity.getStatus().isActive()) {
                throw new ServiceException(ErrorCode.AUCTION_CLOSED, "Cannot register auto-bid: The auction has already concluded.");
            }

            validateAutoBidTerms(auctionEntity, autoBidRequest, bidderId);
            
            User bidder = userDAO.findById(bidderId);
            validateBidderAccount(bidder, autoBidRequest.getMaxBid());

            // Race-condition guard: kiểm tra lại sau khi validate xong, trước khi ghi vào map.
            // Nếu clearRegistrations() đã chạy (cancel) trong khoảng thời gian validate,
            // cancelledAuctionIds sẽ chứa auctionId này → từ chối insert.
            if (cancelledAuctionIds.contains(autoBidRequest.getAuctionId())) {
                throw new ServiceException(ErrorCode.AUCTION_CLOSED,
                    "Cannot register auto-bid: The auction has been cancelled.");
            }

            List<AutoBidEntry> autoBidEntriesList = registrationsMap.computeIfAbsent(
                    autoBidRequest.getAuctionId(), key -> new CopyOnWriteArrayList<>());
                    
            autoBidEntriesList.removeIf(entry -> entry.bidderId.equals(bidderId));
            autoBidEntriesList.add(new AutoBidEntry(bidderId, bidderUsername, (long) autoBidRequest.getMaxBid(), (long) autoBidRequest.getIncrement()));
            
            trigger(auctionEntity);
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.register: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    public void trigger(Auction auctionEntity) throws Exception {
        try {
            if (auctionEntity == null || auctionEntity.getStatus().isEnded() || auctionEntity.isExpired() || !auctionEntity.getStatus().isActive()) {
                return;
            }
    
            String auctionId = auctionEntity.getAuctionConfig().getId();
            BidTransaction bidTransaction = auctionEntity.getLastBidTransaction();
            long currentAuctionPrice = bidTransaction == null ? auctionEntity.getCurrentPrice() : bidTransaction.getBidAmount();
            String highestAuctionBidderId = bidTransaction == null ? null : bidTransaction.getBidderId();
    
            List<AutoBidEntry> autoBidEntriesList = registrationsMap.get(auctionId);
            if (autoBidEntriesList == null || autoBidEntriesList.isEmpty()) {
                return;
            }
    
            logger.log(Level.INFO, "Auto-bid matching triggered for auctionId: " + auctionId + " with " + autoBidEntriesList.size() + " candidates.");
    
            List<AutoBidEntry> entriesSnapshotList = new ArrayList<>(autoBidEntriesList);
            
            List<AutoBidEntry> otherCompetitorsList = new ArrayList<>();
            for (AutoBidEntry entry : entriesSnapshotList) {
                if (!entry.bidderId.equals(highestAuctionBidderId)) {
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
            
            long secondHighestBidAmount = 0;
            for (AutoBidEntry entry : entriesSnapshotList) {
                if (!entry.bidderId.equals(winningEntry.bidderId) && entry.maxBid > secondHighestBidAmount) {
                    secondHighestBidAmount = entry.maxBid;
                }
            }
    
            long basePrice = Math.max(secondHighestBidAmount, currentAuctionPrice);
            long calculatedBidAmount = Math.min(basePrice + winningEntry.increment, winningEntry.maxBid);
            
            if (calculatedBidAmount <= currentAuctionPrice) {
                logger.log(Level.INFO, "Calculated auto-bid " + calculatedBidAmount + " is insufficient against current price " + currentAuctionPrice);
                //
                autoBidEntriesList.removeAll(entriesSnapshotList);

                entriesSnapshotList.forEach(entry -> notifyAutoBidStopped(entry.bidderId));
                //
                return;
            }
    
            List<AutoBidEntry> entriesToRemoveList = new ArrayList<>();
            for (AutoBidEntry entry : autoBidEntriesList) {
                if (!entry.bidderId.equals(winningEntry.bidderId)) {
                    entriesToRemoveList.add(entry);
                }
            }
    
            autoBidEntriesList.removeAll(entriesToRemoveList);
            incrementBidCount(auctionId, winningEntry.bidderId);
            entriesToRemoveList.forEach(entry -> notifyAutoBidStopped(entry.bidderId));
            
            userDAO.lockBidderBalance(winningEntry.bidderId, winningEntry.maxBid);
            SessionRegistry.getInstance().addUnsettledBalance(winningEntry.bidderId, winningEntry.maxBid);

            ConcurrentBidManager.getInstance().submitBid(auctionEntity, winningEntry.bidderId, winningEntry.bidderUsername, calculatedBidAmount, winningEntry.maxBid, BidType.AUTO);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.trigger for auctionId: " + auctionEntity.getAuctionConfig().getId(), exception);
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
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.getRegistrations for auctionId: " + auctionId, exception);
            throw exception;
        }
    }

    /**
     * Get the total number of auto-bids placed by a specific bidder in an auction.
     */
    public int getAutoBidCount(String auctionId, String bidderId) throws Exception {
        try {
            Map<String, AtomicInteger> auctionCountsMap = bidCountsMap.get(auctionId);
            if (auctionCountsMap == null) {
                return 0;
            }
            AtomicInteger count = auctionCountsMap.get(bidderId);
            return count == null ? 0 : count.get();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.getAutoBidCount for bidderId: " + bidderId, exception);
            throw exception;
        }
    }

    /**
     * Clear all auto-bid registrations for a specific auction.
     * Đánh dấu auctionId vào cancelledAuctionIds TRƯỚC khi remove khỏi map,
     * để register() đang chạy đồng thời sẽ thấy flag và từ chối insert entry mới.
     */
    public void clearRegistrations(String auctionId) throws Exception {
        try {
            // 1. Đánh dấu trước — register() check flag này sau validate
            cancelledAuctionIds.add(auctionId);
            // 2. Xóa entries và bidCounts
            registrationsMap.remove(auctionId);
            bidCountsMap.remove(auctionId);
            logger.log(Level.INFO, "Auto-bid registrations successfully cleared for auctionId: " + auctionId);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.clearRegistrations for auctionId: " + auctionId, exception);
            throw exception;
        }
    }

    /**
     * Convert BidTransaction to BidDTO for client response.
     */
    public BidDTO toBidDto(BidTransaction bidTransaction, long currentAuctionPrice) throws Exception {
        try {
            BidDTO bidDto = new BidDTO();
            bidDto.setAuctionId(bidTransaction.getAuctionId());
            bidDto.setBidderUsername(bidTransaction.getBidderUsername());
            bidDto.setBidAmount(bidTransaction.getBidAmount());
            bidDto.setCurrentPrice(currentAuctionPrice);
            bidDto.setBidTime(bidTransaction.getBidTime());
            bidDto.setBidType(bidTransaction.getType().name());
            return bidDto;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.toBidDto: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    // --- PRIVATE METHODS ---

    /**
     * Increment auto-bid count for a bidder in an auction.
     */
    private void incrementBidCount(String auctionId, String bidderId) throws Exception {
        try {
            bidCountsMap.computeIfAbsent(auctionId, auctionKey -> new ConcurrentHashMap<>())
                    .computeIfAbsent(bidderId, bidderKey -> new AtomicInteger(0))
                    .incrementAndGet();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.incrementBidCount for bidderId: " + bidderId, exception);
            throw exception;
        }
    }

    /**
     * Get the total number of auto-bids placed by a specific bidder in an auction. (Duplicated logic removed or kept as internal helper)
     */
    private int getBidCount(String auctionId, String bidderId) {
        Map<String, AtomicInteger> auctionCountsMap = bidCountsMap.get(auctionId);
        if (auctionCountsMap == null) {
            return 0;
        }
        AtomicInteger count = auctionCountsMap.get(bidderId);
        return count == null ? 0 : count.get();
    }

    /**
     * Notify a bidder that their auto-bid has been stopped.
     */
    private void notifyAutoBidStopped(String bidderId) {
        java.io.PrintWriter writer = sessionRegistry.getWriter(bidderId);
        if (writer != null) {
            try {
                synchronized (writer) {
                    writer.println(JsonUtils.toJson(
                        ClientMessage.push("AUTO_BID_STOPPED",
                            java.util.Map.of("message",
                                "Your auto-bid has been deactivated because the current price exceeded your maximum threshold."))));
                }
            } catch (Exception exception) {
                logger.log(Level.WARNING, "Notification transmission failure: Unable to deliver AUTO_BID_STOPPED to bidderId: " + bidderId, exception);
                // Notification failures are non-critical, thus not rethrown.
            }
        }
    }

    private void validateAutoBidRequest(AutoBidRequest autoBidRequest, String bidderId, String bidderUsername) throws ServiceException {
        if (autoBidRequest == null) {
            throw new ServiceException(ErrorCode.AUTO_BID_VALIDATION_ERROR, "The auto-bid request object cannot be null.");
        }
        if (bidderId == null || bidderId.isBlank()) {
            throw new ServiceException(ErrorCode.MISSING_BIDDER_ID, "Operation failed: The bidder identifier is mandatory for auto-bid registration.");
        }
        if (bidderUsername == null || bidderUsername.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_DATA, "Validation error: Bidder username is required.");
        }
        if (autoBidRequest.getAuctionId() == null || autoBidRequest.getAuctionId().isBlank()) {
            throw new ServiceException(ErrorCode.MISSING_AUCTION_ID, "Validation error: Auction identification is missing.");
        }
        if (!BidValidator.isPositiveBid(autoBidRequest.getMaxBid())) {
            throw new ServiceException(ErrorCode.INVALID_BID_AMOUNT, "The maximum auto-bid amount must be greater than zero.");
        }
        if (!BidValidator.isPositiveBid(autoBidRequest.getIncrement())) {
            throw new ServiceException(ErrorCode.INVALID_INCREMENT, "The auto-bid increment value must be greater than zero.");
        }
        if (autoBidRequest.getIncrement() > autoBidRequest.getMaxBid()) {
            throw new ServiceException(ErrorCode.AUTO_BID_INVALID_RANGE, "Constraint violation: The increment cannot exceed the maximum bid threshold.");
        }
    }

    private void validateAutoBidTerms(Auction auctionEntity, AutoBidRequest autoBidRequest, String bidderId) throws ServiceException {
        if (auctionEntity == null) {
            throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "Resource not found: The specified auction does not exist.");
        }
        long minIncrement = auctionEntity.getAuctionConfig().getMinIncrement();
        if (autoBidRequest.getIncrement() < minIncrement) {
            throw new ServiceException(ErrorCode.INCREMENT_TOO_LOW, "Validation failure: Auto-bid increment " + autoBidRequest.getIncrement() + " is below the required minimum of " + minIncrement);
        }
        if (bidderId.equals(auctionEntity.getSellerId())) {
            throw new ServiceException(ErrorCode.AUTO_SELLER_CANNOT_AUTOBID, "Sellers are prohibited from registering auto-bids for their own auctions.");
        }
    }

    private void validateBidderAccount(User bidder, double maxBidAmount) throws ServiceException {
        if (!(bidder instanceof Bidder bidderAccount)) {
            throw new ServiceException(ErrorCode.NOT_BIDDER, "Authentication failure: User is not authorized as a Bidder.");
        }
        if (bidderAccount.getAvailableBalance() < maxBidAmount) {
            throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE, "Account balance is insufficient to cover maximum auto-bid amount.");
        }
    }

    private Auction retrieveAndValidateAuction(String auctionId) throws ServiceException, Exception {
        try {
            // Step 1: Check Registry first (cache strategy)
            Auction auction = AuctionRegistry.getInstance().get(auctionId);
            if (auction == null || auction.getStatus().isEnded() || auction.isExpired() || !auction.getStatus().isActive()) {
                // Step 2: If not in Registry, query from DAO
                auction = auctionDAO.findByAuctionId(auctionId);
                if (auction == null ) {
                    throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "Data integrity error: Auction not found for identifier: " + auctionId);
                }
                // Step 3: Validate auction state
                if (auction.getStatus().isEnded() || auction.isExpired() || !auction.getStatus().isActive()) {
                    throw new ServiceException(ErrorCode.AUCTION_CLOSED, "Operation rejected: This auction has already concluded.");
                }
                // Step 4: Register into Registry to ensure we use the shared instance
                AuctionRegistry.getInstance().registerIfAbsent(auction);
                auction = AuctionRegistry.getInstance().get(auctionId);
            }
            return auction;
        } catch (ServiceException serviceException) {
            // Rethrow business exceptions as-is
            throw serviceException;
        } catch (Exception exception) {
            // Final safety net for system-level failures
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AutoBidService.retrieveAndValidateAuction for auctionId: " + auctionId, exception);
            throw exception;
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
        public final long increment;
        public final LocalDateTime registeredAt;

        public AutoBidEntry(String bidderId, String bidderUsername, long maxBid, long increment) {
            this.bidderId = bidderId;
            this.bidderUsername = bidderUsername;
            this.maxBid = maxBid;
            this.increment = increment;
            this.registeredAt = LocalDateTime.now();
        }
        public String getBidderId() {
            return bidderId;
        }
    }
}
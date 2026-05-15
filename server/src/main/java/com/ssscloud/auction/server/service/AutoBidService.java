package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.ServiceExceptions;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;
import com.ssscloud.auction.server.util.SessionRegistry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    // Logging Standards: Declared first
    private static final Logger logger = Logger.getLogger(AutoBidService.class.getName());

    // Attributes: Dependency Injection with short names
    private final Map<String, List<AutoBidEntry>> autoBidRegistrationsByAuction = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AtomicInteger>> bidCountsByAuction = new ConcurrentHashMap<>();
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final SessionRegistry sessionRegistry = SessionRegistry.getInstance();

    // Constructor section
    public AutoBidService(AuctionDAO auctionDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
    }

    // Public Methods

    /**
     * Register an auto-bid request for a bidder in a specific auction.
     * Validates request integrity, auction status, and bidder financial capacity.
     */
    public void register(AutoBidRequest autoBidRequest, String bidderId, String bidderUsername) throws ServiceExceptions {
        logger.log(Level.INFO, "Processing auto-bid registration for auctionId: " + autoBidRequest.getAuctionId() + " by bidderId: " + bidderId);
        
        validateAutoBidRequest(autoBidRequest, bidderId, bidderUsername);
        Auction auctionEntity = retrieveAndValidateAuction(autoBidRequest.getAuctionId());
        validateAutoBidTerms(auctionEntity, autoBidRequest, bidderId);
        
        User bidder = userDAO.findById(bidderId);
        validateBidderAccount(bidder, autoBidRequest.getMaxBid());

        List<AutoBidEntry> autoBidEntriesList = autoBidRegistrationsByAuction.computeIfAbsent(
                autoBidRequest.getAuctionId(), key -> new CopyOnWriteArrayList<>());
        autoBidEntriesList.removeIf(entry -> entry.bidderId.equals(bidderId));
        autoBidEntriesList.add(new AutoBidEntry(bidderId, bidderUsername, (long) autoBidRequest.getMaxBid(), (long) autoBidRequest.getIncrement()));
        
        trigger(auctionEntity);
    }

    /**
     * Trigger auto-bid matching and execute the highest eligible bid.
     */
    public void trigger(Auction auctionEntity) {
        if (auctionEntity.getStatus().isEnded() || auctionEntity.isExpired()) {
            return;
        }

        String auctionId = auctionEntity.getAuctionConfig().getId();
        BidTransaction bidTransaction = auctionEntity.getLastBidTransaction();
        long currentAuctionPrice = bidTransaction == null ? auctionEntity.getCurrentPrice() : bidTransaction.getBidAmount();
        String highestAuctionBidderId = bidTransaction == null ? null : bidTransaction.getBidderId();

        List<AutoBidEntry> autoBidEntriesList = autoBidRegistrationsByAuction.get(auctionId);
        if (autoBidEntriesList == null || autoBidEntriesList.isEmpty()) {
            return;
        }

        logger.log(Level.INFO, "Auto-bid trigger initiated for auctionId: " + auctionId + " with " + autoBidEntriesList.size() + " active entries");

        List<AutoBidEntry> entriesSnapshotList = new ArrayList<>(autoBidEntriesList);
        
        List<AutoBidEntry> otherCompetitors = new ArrayList<>();
        for (AutoBidEntry entry : entriesSnapshotList) {
            if (!entry.bidderId.equals(highestAuctionBidderId)) {
                otherCompetitors.add(entry);
            }
        }

        if (otherCompetitors.isEmpty()) {
            return;
        }

        // Determine the entry with the highest maximum bid
        AutoBidEntry winningEntry = entriesSnapshotList.get(0);
        for (AutoBidEntry entry : entriesSnapshotList) {
            if (entry.maxBid > winningEntry.maxBid
                    || (entry.maxBid == winningEntry.maxBid
                            && entry.registeredAt.isBefore(winningEntry.registeredAt))) {
                winningEntry = entry;
            }
        }
        
        // Determine the second highest bid to calculate the new price
        long secondHighestBidAmount = 0;
        for (AutoBidEntry entry : entriesSnapshotList) {
            if (!entry.bidderId.equals(winningEntry.bidderId) && entry.maxBid > secondHighestBidAmount) {
                secondHighestBidAmount = entry.maxBid;
            }
        }

        long basePrice = Math.max(secondHighestBidAmount, currentAuctionPrice);
        long calculatedBidAmount = Math.min(basePrice + winningEntry.increment, winningEntry.maxBid);
        
        if (calculatedBidAmount <= currentAuctionPrice) {
            logger.log(Level.INFO, "Auto-bid calculation result: " + calculatedBidAmount + " does not exceed current price: " + currentAuctionPrice);
            return;
        }

        // Xác định và xóa những người thua cuộc ngay lập tức để tránh trigger vòng lặp
        List<AutoBidEntry> entriesToRemoveList = new ArrayList<>();
        for (AutoBidEntry entry : autoBidEntriesList) {
            if (!entry.bidderId.equals(winningEntry.bidderId)) {
                entriesToRemoveList.add(entry);
            }
        }

        autoBidEntriesList.removeAll(entriesToRemoveList);
        incrementBidCount(auctionId, winningEntry.bidderId);
        entriesToRemoveList.forEach(entry -> notifyAutoBidStopped(entry.bidderId));
        ConcurrentBidManager.getInstance().submitBid(auctionEntity, winningEntry.bidderId, winningEntry.bidderUsername, calculatedBidAmount, BidType.AUTO);
    }

    /**
     * Handle successful bid execution and notify observers.
     */
    public void onBidSuccess(Auction auctionEntity, BidTransaction bidTransaction) {
        ChangeManager.getInstance().notify(auctionEntity);
    }

    /**
     * Retrieve all auto-bid registrations for a specific auction.
     */
    public List<AutoBidEntry> getRegistrations(String auctionId) {
        return autoBidRegistrationsByAuction.getOrDefault(auctionId, List.of());
    }

    /**
     * Get the total number of auto-bids placed by a specific bidder in an auction.
     */
    public int getAutoBidCount(String auctionId, String bidderId) {
        Map<String, AtomicInteger> auctionCounts = bidCountsByAuction.get(auctionId);
        if (auctionCounts == null) {
            return 0;
        }
        AtomicInteger count = auctionCounts.get(bidderId);
        return count == null ? 0 : count.get();
    }

    /**
     * Clear all auto-bid registrations for a specific auction.
     */
    public void clearRegistrations(String auctionId) {
        autoBidRegistrationsByAuction.remove(auctionId);
        bidCountsByAuction.remove(auctionId);
        logger.log(Level.INFO, "Auto-bid registrations cleared for auctionId: " + auctionId);
    }

    /**
     * Convert BidTransaction to BidDTO for client response.
     */
    public BidDTO toBidDto(BidTransaction bidTransaction, long currentAuctionPrice) {
        BidDTO bidDto = new BidDTO();
        bidDto.setAuctionId(bidTransaction.getAuctionId());
        bidDto.setBidderUsername(bidTransaction.getBidderUsername());
        bidDto.setBidAmount(bidTransaction.getBidAmount());
        bidDto.setCurrentPrice(currentAuctionPrice);
        bidDto.setBidTime(bidTransaction.getBidTime());
        bidDto.setBidType(bidTransaction.getType().name());
        return bidDto;
    }

    // Private Methods - Internal Logic

    /**
     * Increment auto-bid count for a bidder in an auction.
     */
    private void incrementBidCount(String auctionId, String bidderId) {
        bidCountsByAuction.computeIfAbsent(auctionId, auctionKey -> new ConcurrentHashMap<>())
                .computeIfAbsent(bidderId, bidderKey -> new AtomicInteger(0))
                .incrementAndGet();
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
                logger.log(Level.WARNING, "Failed to deliver AUTO_BID_STOPPED notification to bidderId: " + bidderId, exception);
            }
        }
    }

    // Private Methods - Validation

    /**
     * Validate auto-bid request parameters and data integrity.
     * @throws ServiceExceptions if any validation fails
     */
    private void validateAutoBidRequest(AutoBidRequest autoBidRequest, String bidderId, String bidderUsername) throws ServiceExceptions {
        if (autoBidRequest == null) {
            throw new ServiceExceptions(ErrorCode.AUTO_BID_VALIDATION_ERROR, "The auto-bid request object cannot be null.");
        }
        if (bidderId == null || bidderId.isBlank()) {
            throw new ServiceExceptions(ErrorCode.MISSING_BIDDER_ID, "The bidder identifier is missing.");
        }
        if (bidderUsername == null || bidderUsername.isBlank()) {
            throw new ServiceExceptions(ErrorCode.INVALID_DATA, "The bidder username is required for auto-bid registration.");
        }
        if (autoBidRequest.getAuctionId() == null || autoBidRequest.getAuctionId().isBlank()) {
            throw new ServiceExceptions(ErrorCode.MISSING_AUCTION_ID, "The auction identifier must not be empty.");
        }
        if (!BidValidator.isPositiveBid(autoBidRequest.getMaxBid())) {
            throw new ServiceExceptions(ErrorCode.INVALID_BID_AMOUNT, "The maximum auto-bid amount must be greater than zero.");
        }
        if (!BidValidator.isPositiveBid(autoBidRequest.getIncrement())) {
            throw new ServiceExceptions(ErrorCode.INVALID_INCREMENT, "The auto-bid increment value must be greater than zero.");
        }
        if (autoBidRequest.getIncrement() > autoBidRequest.getMaxBid()) {
            throw new ServiceExceptions(ErrorCode.AUTO_BID_INVALID_RANGE, "The increment value cannot exceed the specified maximum bid.");
        }
    }

    /**
     * Validate auto-bid business constraints and auction state.
     * @throws ServiceExceptions if any business constraint is violated
     */
    private void validateAutoBidTerms(Auction auctionEntity, AutoBidRequest autoBidRequest, String bidderId) throws ServiceExceptions {
        long minIncrement = auctionEntity.getAuctionConfig().getMinIncrement();
        if (autoBidRequest.getIncrement() < minIncrement) {
            throw new ServiceExceptions(ErrorCode.INCREMENT_TOO_LOW, "Auto-bid increment " + autoBidRequest.getIncrement() + " is below auction minimum of " + minIncrement);
        }
        if (bidderId.equals(auctionEntity.getSellerId())) {
            throw new ServiceExceptions(ErrorCode.AUTO_SELLER_CANNOT_AUTOBID, "Sellers are prohibited from registering auto-bids for their own auctions.");
        }
    }

    /**
     * Validate bidder account status and financial capacity.
     * @throws ServiceExceptions if bidder account is invalid or insufficient balance
     */
    private void validateBidderAccount(User bidder, double maxBidAmount) throws ServiceExceptions {
        if (!(bidder instanceof Bidder bidderAccount)) {
            throw new ServiceExceptions(ErrorCode.NOT_BIDDER, "User is not authorized as a Bidder.");
        }
        if (bidderAccount.getAccountBalance() < maxBidAmount) {
            throw new ServiceExceptions(ErrorCode.INSUFFICIENT_BALANCE, "Account balance is insufficient to cover maximum auto-bid amount.");
        }
    }

    /**
     * Retrieve auction from Registry first (cache strategy), then from DAO if not found.
     * Validates auction state before returning.
     * @throws ServiceExceptions if auction not found or is already concluded
     */
    private Auction retrieveAndValidateAuction(String auctionId) throws ServiceExceptions {
        // Step 1: Check Registry first (cache strategy)
        Auction auction = AuctionRegistry.getInstance().get(auctionId);
        if (auction == null) {
            // Step 2: If not in Registry, query from DAO
            auction = auctionDAO.findByAuctionId(auctionId);
            if (auction == null) {
                throw new ServiceExceptions(ErrorCode.AUCTION_NOT_FOUND, "Auction not found with identifier: " + auctionId);
            }
            // Step 3: Validate auction state
            if (auction.getStatus().isEnded() || auction.isExpired()) {
                throw new ServiceExceptions(ErrorCode.AUCTION_CLOSED, "Auction has already concluded.");
            }
            // Step 4: Register into Registry
            AuctionRegistry.getInstance().registerIfAbsent(auction);
            auction = AuctionRegistry.getInstance().get(auctionId);
        }
        return auction;
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

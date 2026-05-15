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
    private static final Logger logger = Logger.getLogger(AutoBidService.class.getName()); // Logging Standards: Declared first

    private final Map<String, List<AutoBidEntry>> autoBidRegistrationsByAuction = new ConcurrentHashMap<>(); // Plural naming
    private final Map<String, Map<String, AtomicInteger>> bidCountsByAuction = new ConcurrentHashMap<>(); // Plural naming
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final SessionRegistry sessionRegistry = SessionRegistry.getInstance();

    public AutoBidService(AuctionDAO auctionDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
    } // Constructor section

    // --- PUBLIC METHODS ---

    public void register(AutoBidRequest autoBidRequest, String bidderId, String bidderUsername) throws ServiceExceptions {
        logger.log(Level.INFO, "Service: Processing auto-bid registration for auctionId: " + autoBidRequest.getAuctionId() + " by bidderId: " + bidderId);
        
        validateAutoBidRequest(autoBidRequest, bidderId, bidderUsername); // Step 1: Validate request data

        Auction auctionEntity = retrieveAndValidateAuction(autoBidRequest.getAuctionId()); // Step 2: Retrieve subject auction

        validateAutoBidTerms(auctionEntity, autoBidRequest, bidderId); // Step 3: Validate business constraints

        User bidder = userDAO.findById(bidderId);
        validateBidderAccount(bidder, autoBidRequest.getMaxBid()); // Step 4: Validate bidder financial status

        List<AutoBidEntry> autoBidEntriesList = autoBidRegistrationsByAuction.computeIfAbsent(
                autoBidRequest.getAuctionId(), key -> new CopyOnWriteArrayList<>());
        autoBidEntriesList.removeIf(entry -> entry.bidderId.equals(bidderId));
        autoBidEntriesList.add(new AutoBidEntry(bidderId, bidderUsername, (long) autoBidRequest.getMaxBid(), (long) autoBidRequest.getIncrement()));
        
        trigger(auctionEntity);
    }

    public void trigger(Auction auctionEntity) {
        if (auctionEntity.getStatus().isEnded() || auctionEntity.isExpired()) {
            return;
        }

        String auctionId = auctionEntity.getAuctionConfig().getId();
        long currentAuctionPrice = auctionEntity.getCurrentPrice();

        List<AutoBidEntry> autoBidEntriesList = autoBidRegistrationsByAuction.get(auctionId); // List suffix
        if (autoBidEntriesList == null || autoBidEntriesList.isEmpty()) {
            return;
        }

        logger.log(Level.FINE, "Auto-bid trigger initiated for auctionId: " + auctionId + ". Active entries: " + autoBidEntriesList.size());

        List<AutoBidEntry> entriesSnapshotList = new ArrayList<>(autoBidEntriesList); // Take a point-in-time snapshot
        if (entriesSnapshotList.size() <= 1) {
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
            logger.log(Level.FINE, "Calculated auto-bid amount: " + calculatedBidAmount + " does not exceed current price: " + currentAuctionPrice + ". Skipping.");
            return;
        }

        ConcurrentBidManager.getInstance().submitBid(auctionEntity, winningEntry.bidderId, winningEntry.bidderUsername, calculatedBidAmount, BidType.AUTO);
        incrementBidCount(auctionId, winningEntry.bidderId);

        List<AutoBidEntry> entriesToRemoveList = new ArrayList<>();
        for (AutoBidEntry entry : entriesSnapshotList) {
            if (entry.bidderId.equals(winningEntry.bidderId)) {
                continue;
            }
            entriesToRemoveList.add(entry);
        }

        autoBidEntriesList.removeAll(entriesToRemoveList);
        entriesToRemoveList.forEach(entry -> {
            notifyAutoBidStopped(entry.bidderId);
        });
    }

    public void onBidSuccess(Auction auctionEntity, BidTransaction bidTransaction) {
        ChangeManager.getInstance().notify(auctionEntity);
    }

    public List<AutoBidEntry> getRegistrations(String auctionId) {
        return autoBidRegistrationsByAuction.getOrDefault(auctionId, List.of());
    }

    public int getAutoBidCount(String auctionId, String bidderId) {
        Map<String, AtomicInteger> auctionCounts = bidCountsByAuction.get(auctionId);
        if (auctionCounts == null) {
            return 0;
        }
        AtomicInteger count = auctionCounts.get(bidderId);
        return count == null ? 0 : count.get();
    }

    public void clearRegistrations(String auctionId) {
        autoBidRegistrationsByAuction.remove(auctionId);
        bidCountsByAuction.remove(auctionId);
        logger.log(Level.INFO, "[AutoBidService] Successfully cleared auto-bid registrations for auctionId: " + auctionId);
    }

    public BidDTO toBidDto(BidTransaction bidTransaction, long currentAuctionPrice) {
        BidDTO bidDto = new BidDTO(); // DTO suffix
        bidDto.setAuctionId(bidTransaction.getAuctionId());
        bidDto.setBidderUsername(bidTransaction.getBidderUsername());
        bidDto.setBidAmount(bidTransaction.getBidAmount());
        bidDto.setCurrentPrice(currentAuctionPrice);
        bidDto.setBidTime(bidTransaction.getBidTime());
        bidDto.setBidType(bidTransaction.getType().name());
        return bidDto;
    }

    // --- PRIVATE HELPERS ---

    private void incrementBidCount(String auctionId, String bidderId) {
        bidCountsByAuction.computeIfAbsent(auctionId, auctionKey -> new ConcurrentHashMap<>())
                .computeIfAbsent(bidderId, bidderKey -> new AtomicInteger(0))
                .incrementAndGet(); // Atomic internal logic
    }

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
                logger.log(Level.WARNING, "System failure: Unable to deliver AUTO_BID_STOPPED notification to bidderId: " + bidderId, exception);
            }
        }
    }

    // --- VALIDATION METHODS ---

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

    private void validateAutoBidTerms(Auction auctionEntity, AutoBidRequest autoBidRequest, String bidderId) throws ServiceExceptions {
        long minIncrement = auctionEntity.getAuctionConfig().getMinIncrement();
        if (autoBidRequest.getIncrement() < minIncrement) {
            throw new ServiceExceptions(ErrorCode.INCREMENT_TOO_LOW, "Business logic violation: The specified increment is below the required auction minimum of " + minIncrement);
        }
        if (bidderId.equals(auctionEntity.getSellerId())) {
            throw new ServiceExceptions(ErrorCode.AUTO_SELLER_CANNOT_AUTOBID, "Authorization failure: Sellers are prohibited from registering auto-bids for their own auctions.");
        }
    }

    private void validateBidderAccount(User bidder, double maxBidAmount) throws ServiceExceptions {
        if (!(bidder instanceof Bidder bidderAccount)) {
            throw new ServiceExceptions(ErrorCode.NOT_BIDDER, "The user role is not authorized as a 'Bidder'; registration rejected.");
        }
        if (bidderAccount.getAccountBalance() < maxBidAmount) {
            throw new ServiceExceptions(ErrorCode.INSUFFICIENT_BALANCE, "Financial failure: The account balance is insufficient to cover the maximum auto-bid threshold.");
        }
    }

    private Auction retrieveAndValidateAuction(String auctionId) throws ServiceExceptions {
        Auction auctionEntity = auctionDAO.findByAuctionId(auctionId);
        if (auctionEntity == null) {
            throw new ServiceExceptions(ErrorCode.AUCTION_NOT_FOUND, "Resource error: Unable to locate an auction with identifier: " + auctionId);
        }
        if (auctionEntity.getStatus().isEnded() || auctionEntity.isExpired()) {
            throw new ServiceExceptions(ErrorCode.AUCTION_CLOSED, "Auto-bid registration is unavailable because the auction has already concluded.");
        }
        AuctionRegistry.getInstance().registerIfAbsent(auctionEntity); 
        return AuctionRegistry.getInstance().get(auctionId); // Logic remains unchanged
    }

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
    }
}

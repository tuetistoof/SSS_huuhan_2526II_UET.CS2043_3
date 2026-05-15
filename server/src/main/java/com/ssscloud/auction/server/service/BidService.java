package com.ssscloud.auction.server.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.ServiceExceptions;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;

/**
 * BidService is responsible for handling the business logic of manual bid placements.
 */
public class BidService {
    private static final Logger logger = Logger.getLogger(BidService.class.getName()); // Logging Standards: Declared first

    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;

    public BidService(AuctionDAO auctionDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
    }

    // --- PUBLIC METHODS ---

    public void placeBid(PlaceBidRequest placeBidRequest, String bidderId, String bidderUsername) throws ServiceExceptions {
        logger.log(Level.INFO, "Initiating manual bid placement for auctionId: " + (placeBidRequest != null ? placeBidRequest.getAuctionId() : "null") + " by bidderId: " + bidderId);

        validatePlaceBidRequest(placeBidRequest, bidderId);

        Auction auction = AuctionRegistry.getInstance().get(placeBidRequest.getAuctionId());
        if (auction == null) {
            auction = retrieveAndValidateAuction(placeBidRequest.getAuctionId());
        }
        validatePlaceBidTerms(auction, placeBidRequest, bidderId); // Validation: Specific business terms

        User bidder = userDAO.findById(bidderId);
        validatePlaceBidderAccount(bidder, placeBidRequest.getBidAmount()); // Validation: Account state

        ConcurrentBidManager.getInstance().submitBid(auction, bidderId, bidderUsername, placeBidRequest.getBidAmount(), BidType.MANUAL);
    }

    // --- PRIVATE METHODS ---

    private void validatePlaceBidRequest(PlaceBidRequest placeBidRequest, String bidderId) throws ServiceExceptions {
        if (placeBidRequest == null) {
            throw new ServiceExceptions(ErrorCode.INVALID_BID_REQUEST, "The bid request object cannot be null.");
        }
        if (placeBidRequest.getAuctionId() == null || placeBidRequest.getAuctionId().isBlank()) {
            throw new ServiceExceptions(ErrorCode.MISSING_AUCTION_ID, "The auction identifier is required.");
        }
        if (bidderId == null || bidderId.isBlank()) {
            throw new ServiceExceptions(ErrorCode.MISSING_BIDDER_ID, "The bidder identifier is missing from the current session.");
        }
        if (!BidValidator.isPositiveBid(placeBidRequest.getBidAmount())) {
            throw new ServiceExceptions(ErrorCode.INVALID_BID_AMOUNT, "The bid amount must be a positive value greater than zero.");
        }
    }

    private void validatePlaceBidTerms(Auction auction, PlaceBidRequest placeBidRequest, String bidderId) throws ServiceExceptions {
        long minIncrement = auction.getAuctionConfig().getMinIncrement();
        if (placeBidRequest.getBidAmount() - auction.getCurrentPrice() < minIncrement) {
            throw new ServiceExceptions(ErrorCode.INCREMENT_TOO_LOW, "The bid increment is lower than the required minimum of " + minIncrement);
        }
        if (bidderId.equals(auction.getSellerId())) {
            throw new ServiceExceptions(ErrorCode.SELLER_CANNOT_BID, "Sellers are prohibited from placing bids on their own auction items.");
        }
    }

    private void validatePlaceBidderAccount(User bidder, long bidAmountValue) throws ServiceExceptions {
        if (!(bidder instanceof Bidder bidderAccount)) {
            throw new ServiceExceptions(ErrorCode.NOT_BIDDER, "Only users with the 'Bidder' role are authorized to place bids.");
        }
        if (bidderAccount.getAccountBalance() < bidAmountValue) {
            throw new ServiceExceptions(ErrorCode.INSUFFICIENT_BALANCE, "The account balance is insufficient to place this bid.");
        }
    }

    /**
     * Retrieves an auction by ID and performs initial validation on its state.
     * @param auctionId The ID of the auction to retrieve.
     * @return The validated Auction entity.
     * @throws ServiceExceptions if the auction is not found or has already concluded.
     */
    private Auction retrieveAndValidateAuction(String auctionId) throws ServiceExceptions {
        Auction auction = auctionDAO.findByAuctionId(auctionId);
        if (auction == null) {
            throw new ServiceExceptions(ErrorCode.AUCTION_NOT_FOUND, "The system could not locate an auction with the specified ID: " + auctionId);
        }
        if (auction.getStatus().isEnded() || auction.isExpired()) {
            throw new ServiceExceptions(ErrorCode.AUCTION_CLOSED, "Bidding is unavailable because the auction has already concluded.");
        }
        AuctionRegistry.getInstance().registerIfAbsent(auction); // Register if not already in registry
        return AuctionRegistry.getInstance().get(auctionId); // Return the registered instance
    }
}

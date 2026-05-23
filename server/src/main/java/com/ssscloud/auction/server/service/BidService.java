package com.ssscloud.auction.server.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.model.auction.BidTransaction;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.common.model.user.Bidder;
import com.ssscloud.auction.common.payload.request.PlaceBidRequest;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;
import com.ssscloud.auction.server.util.SessionRegistry;

/**
 * BidService is responsible for handling the business logic of manual bid
 * placements.
 */
public class BidService {
    private static final Logger logger = Logger.getLogger(BidService.class.getName()); // Logging Standards: Declared
                                                                                       // first

    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;

    public BidService(AuctionDAO auctionDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
    }

    // --- PUBLIC METHODS ---

    public void placeBid(PlaceBidRequest placeBidRequest, String bidderId, String bidderUsername)
            throws ServiceException, Exception {
        try {
            logger.log(Level.INFO,
                    "Initiating manual bid placement for auctionId: "
                            + (placeBidRequest != null ? placeBidRequest.getAuctionId() : "null") + " by bidderId: "
                            + bidderId);

            validatePlaceBidRequest(placeBidRequest, bidderId);

            Auction auction = AuctionRegistry.getInstance().retrieveAndValidateAuction(placeBidRequest.getAuctionId());
            validatePlaceBidTerms(auction, placeBidRequest, bidderId);
            User bidder = userDAO.findById(bidderId);
            validateBidderAccount(bidder, placeBidRequest.getBidAmount());
            if (!userDAO.lockBidderBalance(bidderId, placeBidRequest.getBidAmount())) {
                throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE,
                        "Insufficient available balance to lock this bid.");
            }
            SessionRegistry.getInstance().addUnsettledBalance(bidderId, placeBidRequest.getBidAmount());
            try {
                ConcurrentBidManager.getInstance().submitBid(auction, bidderId, bidderUsername,
                    placeBidRequest.getBidAmount(), placeBidRequest.getBidAmount(), BidType.MANUAL);
            } catch (Exception submitException) {
                userDAO.unlockBidderBalance(bidderId, placeBidRequest.getBidAmount());
                SessionRegistry.getInstance().addUnsettledBalance(bidderId, -placeBidRequest.getBidAmount());
                throw submitException;
            }
        } catch (ServiceException serviceException) {
            logger.log(Level.WARNING, "Service exception during manual bid placement for auctionId: "
                    + (placeBidRequest != null ? placeBidRequest.getAuctionId() : "null"), serviceException);
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during manual bid placement for bidderId: " + bidderId,
                    exception);
            throw exception;
        }
    }

    // --- PRIVATE METHODS ---

    private void validatePlaceBidRequest(PlaceBidRequest placeBidRequest, String bidderId)
            throws ServiceException, Exception {
        if (placeBidRequest == null) {
            throw new ServiceException(ErrorCode.INVALID_BID_REQUEST, "The bid request object cannot be null.");
        }
        if (placeBidRequest.getAuctionId() == null || placeBidRequest.getAuctionId().isBlank()) {
            throw new ServiceException(ErrorCode.MISSING_AUCTION_ID, "The auction identifier is required.");
        }
        if (bidderId == null || bidderId.isBlank()) {
            throw new ServiceException(ErrorCode.MISSING_BIDDER_ID,
                    "The bidder identifier is missing from the current session.");
        }
        if (!BidValidator.isPositiveBid(placeBidRequest.getBidAmount())) {
            throw new ServiceException(ErrorCode.INVALID_BID_AMOUNT,
                    "The bid amount must be a positive value greater than zero.");
        }
    }

    private void validatePlaceBidTerms(Auction auction, PlaceBidRequest placeBidRequest, String bidderId)
            throws ServiceException, Exception {

        long bidAmount = placeBidRequest.getBidAmount();
        long minIncrement = auction.getAuctionConfig().getMinIncrement();
        BidTransaction lastBid = auction.getLastBidTransaction();

        if (lastBid == null) {
            // Chưa có bid — chỉ cần >= startPrice
            if (bidAmount < auction.getAuctionConfig().getStartPrice()) {
                throw new ServiceException(ErrorCode.INCREMENT_TOO_LOW,
                        "Bid must be at least the starting price of " + auction.getAuctionConfig().getStartPrice());
            }
        } else {
            // Đã có bid — phải vượt currentPrice + minIncrement
            if (bidAmount - auction.getCurrentPrice() < minIncrement) {
                throw new ServiceException(ErrorCode.INCREMENT_TOO_LOW,
                        "The bid increment is lower than the required minimum of " + minIncrement);
            }
        }
    }

    private void validateBidderAccount(User bidder, long bidAmountValue) throws ServiceException, Exception {
        try {
            if (!(bidder instanceof Bidder bidderAccount)) {
                throw new ServiceException(ErrorCode.NOT_BIDDER, "Only users with the 'Bidder' role are authorized to place bids.");
            }
            if (bidderAccount.getAvailableBalance() < bidAmountValue) {
                throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE, "The account balance is insufficient to place this bid.");
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during bidder account validation", exception);
            throw exception;
        }
    }

}

package com.ssscloud.auction.server.service;
import java.lang.ModuleLayer.Controller;

import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.ServiceExceptions;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;

public class BidService {
    private ConcurrentBidManager bidManager;
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    public BidService (AuctionDAO auctionDAO, UserDAO userDAO)
    {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
    }

    public void placeBid(PlaceBidRequest req, String bidderId, String bidderUsername) {
        if (req == null)
            throw new Controller("INVALID_REQUEST", "request không được null");
        if (req.getAuctionId() == null || req.getAuctionId().isBlank())
            throw new InvalidBidException("Thiếu auctionId");
        if (bidderId == null || bidderId.isBlank())
            throw new InvalidBidException("Thiếu bidderId");
        if (!BidValidator.isPositiveBid(req.getBidAmount()))
            throw new InvalidBidException("Bid amount phải dương");

        Auction auction = AuctionRegistry.getInstance().get(req.getAuctionId());
        if (auction == null) {
            auction = auctionDAO.findByAuctionId(req.getAuctionId());
            if (auction == null)
                throw new AuctionNotFoundException("Phiên đấu giá không tồn tại: " + req.getAuctionId());
            if (auction.getStatus().isEnded() || auction.isExpired())
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc");
            AuctionRegistry.getInstance().registerIfAbsent(auction);
            auction = AuctionRegistry.getInstance().get(req.getAuctionId());
        }
        if (bidderId.equals(auction.getSellerId()))
            throw new InvalidBidException("Người bán không thể đấu giá sản phẩm của mình");

        User bidder = userDAO.findById(bidderId);
        if (!(bidder instanceof Bidder b))
            throw new IllegalArgumentException("Người dùng không phải bidder");
        if (b.getAccountBalance() < req.getBidAmount())
            throw new InvalidBidException("Số dư tài khoản không đủ để đặt giá");
        bidManager = ConcurrentBidManager.getInstance();
        bidManager.submitBid(auction, bidderId, bidderUsername, req.getBidAmount(), BidType.MANUAL);
    }
}

package com.ssscloud.auction.server.service;


import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.AuctionDAO;

public class BidService {
    private final ConcurrentBidManager bidManager = ConcurrentBidManager.getInstance();
    private final AuctionDAO auctionDAO;

    public BidService (AuctionDAO auctionDAO)
    {
        this.auctionDAO = auctionDAO;
    }

    public void placeBid(PlaceBidRequest req, String bidderId, String bidderUsername) {
        if (req == null)
            throw new InvalidBidException("Request không được null");
        if (req.getAuctionId() == null || req.getAuctionId().isBlank())
            throw new InvalidBidException("Thiếu auctionId");
        if (bidderId == null || bidderId.isBlank())
            throw new InvalidBidException("Thiếu bidderId");
        if (!BidValidator.isPositiveBid(req.getBidAmount()))
            throw new InvalidBidException("Bid amount phải dương");

        Auction auction = auctionDAO.findByAuctionId(req.getAuctionId());
        if (auction == null)
            throw new InvalidBidException("Phiên đấu giá không tồn tại: " + req.getAuctionId());
        if (bidderId.equals(auction.getSellerId()))
            throw new InvalidBidException("Người bán không thể đấu giá sản phẩm của mình");

        bidManager.submitBid(auction, bidderId, bidderUsername, req.getBidAmount(), BidType.MANUAL);
    }
}

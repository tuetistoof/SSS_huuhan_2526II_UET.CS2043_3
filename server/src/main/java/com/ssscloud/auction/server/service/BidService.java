package com.ssscloud.auction.server.service;

import java.util.List;

import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.BidTransactionDAO;

public class BidService {
    private final ConcurrentBidManager bidManager = ConcurrentBidManager.getInstance();
    private final AuctionService auctionService;
    private final BidTransactionDAO bidTransactionDAO;
    private final AntiSnipingService antiSnipingService;
    private final AutoBidService autoBidService;

    public BidService(AuctionService auctionService, BidTransactionDAO bidTransactionDAO, AntiSnipingService antiSnipingService, AutoBidService autoBidService){
        this.auctionService = auctionService;
        this.bidTransactionDAO = bidTransactionDAO;
        this.antiSnipingService = antiSnipingService;
        this.autoBidService = autoBidService;
    }


    public BidDTO placeBid(PlaceBidRequest req){
        if (req == null)
            throw new InvalidBidException("Request không được null");
        if (req.getAuctionId() == null || req.getAuctionId().isBlank())
            throw new InvalidBidException("Thiếu auctionId");
        if (req.getBidderId() == null || req.getBidderId().isBlank())
            throw new InvalidBidException("Thiếu bidderId");
        if (BidValidator.isPositiveBid(req.getBidAmount()))
            throw new InvalidBidException("Bid amount phải dương");

        Auction auction = auctionService.getActiveAuctions(req.getAuctionId());
        if (auction == null)
            throw new InvalidBidException("Phiên đấu giá không tồn tại hoặc đã kết thúc: " + req.getAuctionId());
        if (req.getBidderId().equals(auction.getSellerId()))
            throw new InvalidBidException("Người bán không thể đấu giá sản phẩm của mình");


        BidTransaction bid = bidManager.placeBid(auction, req.getBidderId(), req.getBidderUsername(), req.getBidAmount(),   BidType.MANUAL, antiSnipingService);

        try {
            boolean saved = bidTransactionDAO.saveBidTransaction(bid);
            if (!saved) {
                System.err.println("[BidService] WARN: Lưu DB thất bại — "
                        + "bidder=" + bid.getBidderId()
                        + " | auction=" + bid.getAuctionId()
                        + " | amount=" + bid.getBidAmount()
                        + " — bid vẫn hợp lệ trong memory");
            }
        } catch (Exception e) {
            System.err.println("[BidService] WARN: Exception khi lưu DB: " + e.getMessage()
                    + " — bid vẫn hợp lệ trong memory");
        }

        try {
            autoBidService.trigger(auction);
        } catch (Exception e) {
            System.err.println("[BidService] WARN: Auto-bid trigger lỗi: " + e.getMessage()
                    + " — bid MANUAL vẫn hợp lệ");
        }

        List<BidTransaction> history = auction.getBidTransaction();
        BidTransaction finalBid = history.isEmpty() ? bid : history.get(history.size() - 1);
 
        return toDTO(finalBid, auction.getCurrentPrice());
    }

     private BidDTO toDTO(BidTransaction bid, long currentPrice) {
        BidDTO dto = new BidDTO();
        dto.setAuctionId(bid.getAuctionId());
        dto.setBidderUsername(bid.getBidderUsername());
        dto.setBidAmount(bid.getBidAmount());
        dto.setCurrentPrice(currentPrice);
        dto.setBidTime(bid.getBidTime());
        dto.setBidType(bid.getType().name());
        return dto;
     }
}

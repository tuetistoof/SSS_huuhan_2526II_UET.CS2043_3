package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.server.dao.AuctionDAO;
<<<<<<< HEAD

=======
>>>>>>> 52b90662c489626ee281af0959865dd34783bc6c

import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.exception.*;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.server.dao.BidTransactionDAO;

/**
 * điều phối logic luồng đấu giá
 * luồng: controller gọi BidController.placebid(dto)
 * -> BidService.placebid(request, bidderID) : validate cơ bản, tìm auction trong memory
 * -> xử lí concurrency
 * -> thông báo observer
 * bidDAO save
 */




public class BidService {
    private final ConcurrentBidManager bidManager = ConcurrentBidManager.getInstance();
    //làm observer sau 
    private final AuctionDAO auctionDAO;
<<<<<<< HEAD
    private final BidTransaction bidDAO;
    private final AntiSnipingService antiSnipingService;
    private final AutoBidService autoBidService;
 
    public BidService(AuctionDAO auctionDAO, BidTransaction bidDAO, AntiSnipingService antiSnipingService, AutoBidService autoBidService) {
=======
    private final BidTransactionDAO bidTransactionDAO;
    private final AntiSnipingService antiSnipingService;
    private final AutoBidService autoBidService;
 
    public BidService(AuctionDAO auctionDAO, BidTransactionDAO bidTransactionDAO, AntiSnipingService antiSnipingService, AutoBidService autoBidService) {
>>>>>>> 52b90662c489626ee281af0959865dd34783bc6c
        this.auctionDAO     = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;
        this.antiSnipingService = antiSnipingService;
        this.autoBidService = autoBidService;
    }

    public BidDTO placeBid(PlaceBidRequest request){  //handle req từ bid controller chuyển thành dto response chuyển lại client
        //validate cơ bản
        if (!BidValidator.isPositiveBid(request.getBidAmount())) {
            throw new InvalidBidException("Số tiền đặt phải lớn hơn 0");
        }
        if (request.getAuctionId() == null || request.getAuctionId().isBlank()) {
            throw new InvalidBidException("Thiếu auctionId");
        }
        if (request.getBidderId() == null || request.getBidderId().isBlank()) {
            throw new InvalidBidException("Thiếu bidderId");
        }
        Auction auction = new Auction();
        //Auction auction = auctionDAO.findById(request.getAuctionId());   nao có database thì xóa dòng trên, dữ dòng này
        
        String auctionId = auction.getAuctionConfig().getId();

        BidTransaction bid = bidManager.placeBid(
            auction,
            request.getBidderId(),
            request.getBidderUsername(),
            request.getBidAmount(),
            BidType.MANUAL
        );

        //lưu vào dao
        //antisnipping
        return toDTO(bid, auction.getCurrentPrice());
    }
    private BidDTO toDTO(BidTransaction bid, long currentPrice) {
        BidDTO dto = new BidDTO();
        dto.setAuctionId(bid.getAuctionId());
        dto.setBidderUsername(bid.getBidderUsername());
        dto.setBidAmount(bid.getBidAmount());
        dto.setCurrentPrice(currentPrice);
        dto.setBidTime(bid.getBidTime());
        return dto;
    }

}

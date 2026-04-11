package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.util.BidValidator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.exception.*;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.enums.BidType;

/**
 * điều phối logic luồng đấu giá
 * luồng: controller gọi BidController.placebid(json)
 * -> BidServer.placebid(request, bidderID) : validate cơ bản, tìm auction trong memory
 * -> xử lí concurrency
 * -> thông báo observer
 * bidDAO save
 */




public class BidService {
    private final ConcurrentBidManager bidManager = ConcurrentBidManager.getInstance();
    private static final Map<String, Auction> auctionStore = new ConcurrentHashMap<>(); //này dùng tạm cho thiếu DATABASE 

    //làm observer sau 








    public BidDTO placeBid(PlaceBidRequest request, String bidderID, String bidderUsername){
        

        //SỬ DỤNG BidValidator
        if (!BidValidator.isPositiveBid(request.getBidAmount())){
            throw new InvalidBidException("Bid amount must be positive");
        }
        // viết thêm validate nữa

        String auctionId = request.getAuctionId().toString();
        Auction auction = auctionStore.get(auctionId); //tìm auction theo id


        BidTransaction bid;
        try { 
            bid = bidManager.placeBid(auction, bidderID, bidderUsername, request.getBidAmount(), BidType.MANUAL);
    
        } catch (Exception e){ // đoạn này viết láo, chưa viết try catch hết
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi đặt giá: " + e.getMessage(), e);
        }
        return toDTO(bid);
    }

    private BidDTO toDTO(BidTransaction bid){
        BidDTO dto = new BidDTO();
        dto.setBidderUsername(bid.getBidderUsername());
        dto.setBidAmount(bid.getBidAmount());
        dto.setBidTime(bid.getBidTime());
        return dto;
    }
}

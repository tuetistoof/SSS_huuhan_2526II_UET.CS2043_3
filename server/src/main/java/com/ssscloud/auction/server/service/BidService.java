package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.util.BidValidator;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.exception.*;
import com.ssscloud.auction.common.dto.response.BidDTO;

public class BidService {
    








    public BidDTO placeBid(PlaceBidRequest request, String bidderID){
        //thiếu khởi tạo auction, viết sau nhớ
        //kiểm tra request bid

        //== SỬ DỤNG BidValidator==
        if (!BidValidator.isPositiveBid(request.getBidAmount())){
            throw new InvalidBidException("Bid amount must be positive");
        }
   
        //chưa có dao để viết nốt


    }
}

package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.model.base.AuctionConfig;

import java.time.LocalDateTime;
import java.time.Duration;

public class AntiSnipingService {
    public stati void processAntiSniping(AuctionConfig auction) {
        if (auction == null || auction.getEndTime() == null)
            return;

        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime endTime = auction.getEndTime();

        long secondsLeft = Duration.between(currentTime, endTime).toSeconds();


        if (secondsLeft > 0 && secondsLeft <= 36){
            LocalDateTime newEndTime = endTime.plusSeconds(auction.getExtendSecond());
            auction.setEndTime(newEndTime);
            System.out.println("[Anti - Snipping] Auction " + auction.getName() + " đã được kéo dài thêm " + auction.getExtendSecond() + "s . Thời gian kết thúc: " + newEndTime);
        }
    }
}

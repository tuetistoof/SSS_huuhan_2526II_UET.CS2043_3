package com.ssscloud.auction.common.observer;

import java.time.LocalDateTime;

import com.ssscloud.auction.common.dto.BidNotificationDTO;
import com.ssscloud.auction.common.model.Auction;

/**
 * là concrete observer implements observer
 * có method update
 */


public class ClientObserver implements Observer{
    @Override
    public void update(Subject subject) {
        // Pull: cast Subject → Auction để đọc data
        if (!(subject instanceof Auction)) return;
        Auction auction = (Auction) subject;
 
        // Tạo notification từ data auction
        BidNotificationDTO notification = new BidNotificationDTO();
        notification.setNewPrice(auction.getCurrentPrice());
        notification.setHighestBidder(auction.getHighestBidderId());
        notification.setTimestamp(LocalDateTime.now());


        //có gì bổ sung sau
    }

    
}

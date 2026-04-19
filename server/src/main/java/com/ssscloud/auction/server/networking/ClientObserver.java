package com.ssscloud.auction.server.networking;

import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.model.Auction;

import java.io.PrintWriter;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.observer.Observer;
import com.ssscloud.auction.common.observer.Subject;
import com.ssscloud.auction.common.util.JsonUtils;

/**
 * ClientObserver - là concrete observer
 * Nhận thông báo từ ChangeManager và push JSON về Client
 */
public class ClientObserver implements Observer {

    private final String clientId;
    private final PrintWriter writer;

    public ClientObserver(String clientId, PrintWriter writer) {
        this.clientId = clientId;
        this.writer = writer;
    }

    @Override
    public void update(Subject subject) {       
        try {
            if (!(subject instanceof Auction)) return;
            Auction auction = (Auction) subject;
                
                // Tạo BidDTO từ auction (hoặc lấy bid mới nhất)
                BidDTO bidDTO = new BidDTO();
                //bidDTO.setAuctionId(auction.getId());
                bidDTO.setBidAmount(auction.getCurrentPrice());
                bidDTO.setBidderUsername(auction.getHighestBidderName());

                // Tạo message push
                ClientMessage pushMsg = new ClientMessage("BID_UPDATE", bidDTO);
                String json = JsonUtils.toJson(pushMsg);
                if (writer != null) {
                    writer.println(json);
                    writer.flush(); //push về client
                }
        } catch (Exception e) {
            System.err.println("Lỗi push đến client " + clientId + ": " + e.getMessage());
        }
    }

    public String getClientId() {
        return clientId;
    }
}
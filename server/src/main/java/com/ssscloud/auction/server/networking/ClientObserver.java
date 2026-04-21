package com.ssscloud.auction.server.networking;

import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;

import java.io.PrintWriter;
import java.util.List;

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

    public ClientObserver(PrintWriter writer, String clientId) {
        this.clientId = clientId;
        this.writer = writer;
    }

    @Override
    public void update(Subject subject) {       
        try {
            if (!(subject instanceof Auction)) return;
            Auction auction = (Auction) subject;
                
                // Tạo BidDTO từ auction (hoặc lấy bid mới nhất)
            BidDTO dto = new BidDTO();
            dto.setAuctionId(auction.getAuctionConfig().getId());
            dto.setCurrentPrice(auction.getCurrentPrice());
            dto.setBidderUsername(auction.getHighestBidderName());  

            List<BidTransaction> history = auction.getBidTransaction();
            if (!history.isEmpty()) {
                BidTransaction latest = history.get(history.size() - 1);
                dto.setBidAmount(latest.getBidAmount());
                dto.setBidTime(latest.getBidTime());
                dto.setBidType(latest.getType().name());
            }

            ClientMessage pushMsg = new ClientMessage("BID_UPDATE", dto);
            synchronized (writer) {
                writer.println(JsonUtils.toJson(pushMsg));
            }

        } catch (Exception e) {
            System.err.println("Lỗi push đến client " + clientId + ": " + e.getMessage());
        }
    }

    public String getClientId() {
        return clientId;
    }
}
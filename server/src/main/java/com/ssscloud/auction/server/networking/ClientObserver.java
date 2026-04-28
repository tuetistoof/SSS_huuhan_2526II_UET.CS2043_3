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

            if (auction.getStatus() == com.ssscloud.auction.common.enums.AuctionStatus.FINISHED) {
                pushAuctionEnded(auction);
            } else {
                pushBidUpdate(auction);
            }
        } catch (Exception e) {
            System.err.println("Lỗi push đến client " + clientId + ": " + e.getMessage());
        }
    }

    private void pushBidUpdate(Auction auction) {
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

        synchronized (writer) {
            writer.println(JsonUtils.toJson(ClientMessage.push("BID_UPDATE", dto)));
        }
    }

    private void pushAuctionEnded(Auction auction) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("auctionId",  auction.getAuctionConfig().getId());
        payload.put("finalPrice", auction.getCurrentPrice());
        payload.put("winner",     auction.getHighestBidderName() != null
                                  ? auction.getHighestBidderName() : "Không có người đặt giá");

        synchronized (writer) {
            writer.println(JsonUtils.toJson(ClientMessage.push("AUCTION_ENDED", payload)));
        }
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public String getObserverId() {
        return clientId;
    }
}
package com.ssscloud.auction.server.networking;

import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;

import java.io.PrintWriter;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.observer.Observer;
import com.ssscloud.auction.common.observer.Subject;
import com.ssscloud.auction.common.util.JsonUtils;

/**
 * ClientObserver is a concrete observer implementation.
 * It receives updates from ChangeManager and pushes JSON notifications to the connected client.
 */
public class ClientObserver implements Observer {
    private static final Logger logger = Logger.getLogger(ClientObserver.class.getName()); // Logging Standards: Declared first

    private final String clientId;
    private final PrintWriter writer;

    public ClientObserver(PrintWriter writer, String clientId) {
        this.clientId = clientId;
        this.writer = writer;
    }

    // --- PUBLIC METHODS ---

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
            logger.log(Level.SEVERE, "Transmitting update failure for clientId: " + clientId, e);
        }
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public String getObserverId() {
        return clientId;
    }

    // --- PRIVATE METHODS ---

    private void pushBidUpdate(Auction auction) {
        BidDTO bidDto = new BidDTO(); // DTO suffix
        bidDto.setAuctionId(auction.getAuctionConfig().getId());
        bidDto.setCurrentPrice(auction.getCurrentPrice());
        bidDto.setBidderUsername(auction.getHighestBidderName());
        bidDto.setHighestBidderId(auction.getHighestBidderId()); 
        bidDto.setNewEndTime(auction.getAuctionConfig().getEndTime());

        List<BidTransaction> bidHistoryList = auction.getBidTransaction(); // List suffix
        if (!bidHistoryList.isEmpty()) {
            BidTransaction latestBidTransaction = bidHistoryList.get(bidHistoryList.size() - 1);
            bidDto.setBidAmount(latestBidTransaction.getBidAmount());
            bidDto.setBidTime(latestBidTransaction.getBidTime());
            bidDto.setBidType(latestBidTransaction.getType().name());
        }

        synchronized (writer) {
            writer.println(JsonUtils.toJson(ClientMessage.push("BID_UPDATE", bidDto)));
        }
    }

    private void pushAuctionEnded(Auction auction) {
        java.util.Map<String, Object> auctionEndedPayload = new java.util.HashMap<>(); // Descriptive internal logic name
        auctionEndedPayload.put("auctionId",  auction.getAuctionConfig().getId());
        auctionEndedPayload.put("finalPrice", auction.getCurrentPrice());
        auctionEndedPayload.put("winner",     auction.getHighestBidderName() != null
                                  ? auction.getHighestBidderName() : "No bids placed"); // Language Policy: Technical English

        synchronized (writer) {
            writer.println(JsonUtils.toJson(ClientMessage.push("AUCTION_ENDED", auctionEndedPayload)));
        }
    }
}
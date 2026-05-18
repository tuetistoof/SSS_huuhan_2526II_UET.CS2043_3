package com.ssscloud.auction.server.networking;

import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.observer.Observer;
import com.ssscloud.auction.common.observer.Subject;
import com.ssscloud.auction.common.util.JsonUtils;

/**
 * ClientObserver is a concrete observer implementation.
 * It receives updates from ChangeManager and pushes JSON notifications to the connected client.
 *
 * Threading model:
 * - update() is called on the worker thread (via ChangeManager.notify).
 * - BidDTO/payload is snapshot-built synchronously on the worker thread to capture
 *   the exact auction state at the moment of notification.
 * - The actual I/O write is offloaded to a per-client SingleThreadExecutor so that
 *   a slow or blocked client never stalls the worker thread or other clients.
 * - SingleThreadExecutor guarantees packets are delivered to each client in order.
 */
public class ClientObserver implements Observer {
    private static final Logger logger = Logger.getLogger(ClientObserver.class.getName()); // Logging Standards: Declared first

    private final String clientId;
    private final PrintWriter writer;
    private final ExecutorService notifyExecutor;

    public ClientObserver(PrintWriter writer, String clientId) {
        this.clientId = clientId;
        this.writer = writer;
        this.notifyExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("notify-" + clientId); // ← clientId đã sẵn sàng ở đây
            return thread;
        });
}

    // --- PUBLIC METHODS ---

    @Override
    public void update(Subject subject) {
        try {
            if (!(subject instanceof Auction auction)) return;

            if (auction.getStatus() == AuctionStatus.FINISHED) {
                // Snapshot payload on worker thread before submitting
                Map<String, Object> payload = buildAuctionEndedPayload(auction);
                notifyExecutor.submit(() -> push("AUCTION_ENDED", payload));
            } else {
                // Snapshot DTO on worker thread before submitting
                BidDTO bidDto = buildBidDto(auction);
                notifyExecutor.submit(() -> push("BID_UPDATE", bidDto));
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

    /**
     * Must be called when the client disconnects to release the executor thread.
     */
    public void shutdown() {
        notifyExecutor.shutdownNow();
        logger.log(Level.INFO, "Notify executor shut down for clientId: " + clientId);
    }

    // --- PRIVATE METHODS ---

    /**
     * Snapshot BidDTO from auction state. Called synchronously on the worker thread.
     */
    private BidDTO buildBidDto(Auction auction) {
        BidDTO bidDto = new BidDTO();
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
        return bidDto;
    }

    /**
     * Snapshot auction-ended payload. Called synchronously on the worker thread.
     */
    private Map<String, Object> buildAuctionEndedPayload(Auction auction) {
        Map<String, Object> auctionEndedPayload = new HashMap<>(); // Descriptive internal logic name
        auctionEndedPayload.put("auctionId",  auction.getAuctionConfig().getId());
        auctionEndedPayload.put("finalPrice", auction.getCurrentPrice());
        auctionEndedPayload.put("winnerId", auction.getHighestBidderId() != null ? auction.getHighestBidderId() : "");
        auctionEndedPayload.put("winner",     auction.getHighestBidderName() != null
                                  ? auction.getHighestBidderName() : "No bids placed"); // Language Policy: Technical English
        return auctionEndedPayload;
    }

    /**
     * Write a JSON message to the client. Runs on the per-client notifyExecutor thread.
     * No synchronized needed: only one thread (notifyExecutor) ever writes to this writer.
     */
    private void push(String eventType, Object payload) {
        try {
            synchronized (writer) {   // ← cần giữ vì ConcurrentBidManager cũng ghi vào writer này
                writer.println(JsonUtils.toJson(ClientMessage.push(eventType, payload)));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Push failure for clientId: " + clientId + ", event: " + eventType, e);
        }
    }
}
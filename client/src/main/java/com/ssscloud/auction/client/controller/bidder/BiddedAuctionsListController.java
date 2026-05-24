package com.ssscloud.auction.client.controller.bidder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.google.gson.reflect.TypeToken;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.response.DTO.BidderDisplayDTO;
import com.ssscloud.auction.common.payload.response.request.ApiResponse;
import com.ssscloud.auction.common.payload.response.request.ListResponse;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.networking.MessageListener;
import com.ssscloud.auction.common.payload.response.DTO.BidDTO;
import com.ssscloud.auction.client.util.SessionManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class BiddedAuctionsListController implements MessageListener {

    @FXML private VBox emptyState, listContainer, spinnerPane;
    @FXML private Label lblSubtitle, lblTotalCount;
    @FXML private ScrollPane scrollPane;

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();
    private List<BidderDisplayDTO> masterList = new ArrayList<>();

    /** Map auctionId → row controller, dùng để refreshRow không cần rebuild toàn list */
    private final Map<String, BiddedAuctionsListRowController> rowControllerMap = new HashMap<>();

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private Consumer<BidderDisplayDTO> onOpenAuction;

    public void setOnOpenAuction(Consumer<BidderDisplayDTO> onOpenAction) {
        this.onOpenAuction = onOpenAction;
    }

    @FXML
    public void initialize() {
        socket.addListener(this);
        loadWatchlist();
    }

    public void cleanup() {
        socket.removeListener(this);
    }

    // ------------------------------------------------------------------ //
    //  Data loading                                                        //
    // ------------------------------------------------------------------ //

    public void loadWatchlist() {
        try {
            String requestJson = JsonUtils.toJson(ClientMessage.request("GET_BIDDED_AUCTIONS", null));

            dispatcher.request(requestJson, raw -> {
                try {
                    if (raw == null) return;

                    ClientMessage serverMsg = JsonUtils.fromJson(raw, ClientMessage.class);
                    if (serverMsg == null || serverMsg.getData() == null) return;

                    String innerJson = JsonUtils.toJson(serverMsg.getData());
                    ApiResponse<?> apiResp = JsonUtils.fromJson(innerJson, ApiResponse.class);
                    if (apiResp == null || !apiResp.isSuccess()) {
                        renderUI(new ArrayList<>());
                        return;
                    }

                    String listJson = JsonUtils.toJson(apiResp.getData());
                    Type listRespType = new TypeToken<ListResponse<BidderDisplayDTO>>(){}.getType();
                    ListResponse<BidderDisplayDTO> listResp = JsonUtils.fromJsonGeneric(listJson, listRespType);

                    List<BidderDisplayDTO> auctions =
                            (listResp != null && listResp.getData() != null)
                            ? listResp.getData() : new ArrayList<>();

                    renderUI(auctions);

                } catch (Exception e) {
                    System.err.println("[Bidded Auctions List] Lỗi parse dữ liệu: " + e.getMessage());
                    e.printStackTrace();
                    renderUI(new ArrayList<>());
                }
            });

        } catch (Exception e) {
            System.err.println("[Bidded Auctions List] Lỗi tạo/gửi request: " + e.getMessage());
            e.printStackTrace();
            renderUI(new ArrayList<>());
        }
    }

    // ------------------------------------------------------------------ //
    //  UI rendering                                                        //
    // ------------------------------------------------------------------ //

    private void renderUI(List<BidderDisplayDTO> auctions) {
        this.masterList = auctions;
        Platform.runLater(() -> {
            listContainer.getChildren().clear();
            rowControllerMap.clear();   // reset map mỗi lần render lại toàn bộ
            lblTotalCount.setText(String.valueOf(auctions.size()));

            if (auctions.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                scrollPane.setVisible(false);
                scrollPane.setManaged(false);
            } else {
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                scrollPane.setVisible(true);
                scrollPane.setManaged(true);

                for (BidderDisplayDTO auction : auctions) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bidded-auction-list-row.fxml"));
                        Parent rowNode = loader.load();

                        BiddedAuctionsListRowController rowCtrl = loader.getController();
                        rowCtrl.setData(auction);
                        rowCtrl.setOnViewRoom(() -> {
                            if (onOpenAuction != null) onOpenAuction.accept(auction);
                        });

                        // Lưu vào map để refreshRow tìm lại được sau này
                        rowControllerMap.put(auction.getId(), rowCtrl);

                        listContainer.getChildren().add(rowNode);
                        System.out.println("[Bidded Auctions List] Successfully added row: " + auction.getAuctionName());

                    } catch (IOException e) {
                        System.err.println("Lỗi nạp hàng cho ID: " + auction.getId());
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Cập nhật UI của một row đã có — không rebuild toàn bộ list.
     * Phải gọi trên FX thread (đã được đảm bảo bởi Platform.runLater ở caller).
     */
    private void refreshRow(BidderDisplayDTO dto) {
        BiddedAuctionsListRowController rowCtrl = rowControllerMap.get(dto.getId());
        if (rowCtrl != null) {
            rowCtrl.setData(dto);
        }
    }

    // ------------------------------------------------------------------ //
    //  Real-time socket handler                                            //
    // ------------------------------------------------------------------ //

    @Override
    public void onMessageReceived(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String action = root.has("action") ? root.get("action").getAsString() : "";
            switch (action) {
                case "BID_UPDATE" -> {
                    BidDTO bid = JsonUtils.fromJson(JsonUtils.toJson(root.get("data")), BidDTO.class);
                    if (bid == null) return;
                    Platform.runLater(() -> {
                        for (BidderDisplayDTO dto : masterList) {
                            if (dto.getId().equals(bid.getAuctionId())) {
                                dto.setCurrentPrice(bid.getBidAmount());
                                // Cập nhật leading: user đang leading nếu chính họ vừa bid
                                String currentUserId = SessionManager.getInstance().getCurrentUser() != null
                                        ? SessionManager.getInstance().getCurrentUser().getId() : null;
                                if (currentUserId != null) {
                                    dto.setLeading(currentUserId.equals(bid.getBidderId()));
                                }
                                refreshRow(dto);
                                break;
                            }
                        }
                    });
                }
                case "AUCTION_ENDED", "AUCTION_CANCELED" -> {
                    // re-fetch để list phản ánh đúng trạng thái
                    Platform.runLater(this::loadWatchlist);
                }
            }
        } catch (Exception e) {
            System.err.println("[BiddedAuctions] onMessageReceived error: " + e.getMessage());
        }
    }
}
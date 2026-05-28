package com.ssscloud.auction.client.controller.bidder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.ssscloud.auction.client.networking.MessageListener;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.response.DTO.BidderDisplayDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.networking.MessageListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Controller cho màn hình Won Items.
 *
 * Flow:
 *  - Server trả danh sách auction đã kết thúc mà bidder này thắng
 *    (action: GET_WON_ITEMS — hoặc tuỳ server đặt tên)
 *  - Filter client-side theo itemType: All / ART / VEHICLE / ELECTRONIC
 *  - Mỗi row load từ won-item-row.fxml, điều khiển bởi ItemsWonRowController
 *  - Callback onOpenAuction truyền ra ngoài (MainLayout) để mở BiddingRoom nếu cần
 */
public class ItemsWonController implements MessageListener {

    @FXML private VBox        listContainer;
    @FXML private VBox        emptyState;
    @FXML private ScrollPane  scrollPane;
    @FXML private Label       lblMetricRunning;

    @FXML private Button      aFilterAll;
    @FXML private Button      aFilterArts;
    @FXML private Button      aFilterVehicles;
    @FXML private Button      aFilterElectronics;

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();
    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();

    private List<BidderDisplayDTO> masterList = new ArrayList<>();
    private String activeFilter = null;
    private Consumer<BidderDisplayDTO> onOpenAuction;


    @FXML
    public void initialize() {
        setActiveFilter(aFilterAll, null);
        socket.addListener(this);
        loadWonItems();
    }

    public void setOnOpenAuction(Consumer<BidderDisplayDTO> callback) {
        this.onOpenAuction = callback;
    }

    public void loadWonItems() {
        String json = JsonUtils.toJson(ClientMessage.request("GET_WON_ITEMS", null));

        dispatcher.request(json, raw -> {
            List<BidderDisplayDTO> items = ServerResponse.unwrapList(raw, null, BidderDisplayDTO.class);
            if (items == null) items = new ArrayList<>();
            masterList = items;
            renderUI(applyFilter(masterList));
        });
    }

    @FXML
    private void filterAuctionAll() {
        setActiveFilter(aFilterAll, null);
        renderUI(applyFilter(masterList));
    }

    @FXML
    private void filterAuctionRunning() {        // "Arts" button
        setActiveFilter(aFilterArts, "ART");
        renderUI(applyFilter(masterList));
    }

    @FXML
    private void filterAuctionOpen() {           // "Vehicles" button
        setActiveFilter(aFilterVehicles, "VEHICLE");
        renderUI(applyFilter(masterList));
    }

    @FXML
    private void filterAuctionDone() {           // "Electronics" button
        setActiveFilter(aFilterElectronics, "ELECTRONIC");
        renderUI(applyFilter(masterList));
    }

    private void renderUI(List<BidderDisplayDTO> items) {
        Platform.runLater(() -> {
            listContainer.getChildren().clear();
            lblMetricRunning.setText(String.valueOf(masterList.size()));

            if (items.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                scrollPane.setVisible(false);
                scrollPane.setManaged(false);
            } else {
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                scrollPane.setVisible(true);
                scrollPane.setManaged(true);

                for (BidderDisplayDTO dto : items) {
                    try {
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/fxml/won-item-row.fxml"));
                        Parent rowNode = loader.load();

                        ItemsWonRowController rowCtrl = loader.getController();
                        rowCtrl.setData(dto);
                        rowCtrl.setOnViewDetails(() -> {
                            if (onOpenAuction != null) onOpenAuction.accept(dto);
                        });

                        listContainer.getChildren().add(rowNode);

                    } catch (IOException e) {
                        System.err.println("[ItemsWon] Error loading row for ID: " + dto.getId());
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private List<BidderDisplayDTO> applyFilter(List<BidderDisplayDTO> source) {
        if (activeFilter == null) return source;
        List<BidderDisplayDTO> result = new ArrayList<>();
        for (BidderDisplayDTO dto : source) {
            if (activeFilter.equalsIgnoreCase(dto.getItemType())) {
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * Cập nhật trạng thái active cho filter buttons.
     * Dùng StyleClass "wi-filter-btn-selected" — đúng như CSS đã định nghĩa.
     */
    private void setActiveFilter(Button activeBtn, String typeFilter) {
        this.activeFilter = typeFilter;
        for (Button btn : new Button[]{aFilterAll, aFilterArts, aFilterVehicles, aFilterElectronics}) {
            btn.getStyleClass().remove("wi-filter-btn-selected");
        }
        activeBtn.getStyleClass().add("wi-filter-btn-selected");
    }

    public void cleanup() {
        socket.removeListener(this);
    }

    @Override
    public void onMessageReceived(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String action = root.has("action") ? root.get("action").getAsString() : "";
            if ("AUCTION_CANCELED".equals(action)) {
                Platform.runLater(this::loadWonItems);
            }
        } catch (Exception e) {
            System.err.println("[ItemsWon] onMessageReceived error: " + e.getMessage());
        }
    }
}
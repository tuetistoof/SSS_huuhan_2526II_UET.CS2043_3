package com.ssscloud.auction.client.controller;

import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.networking.MessageListener;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionListResponse;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * AuctionListController — hiển thị danh sách phiên đấu giá đang mở.
 *
 * Flow:
 *   initialize() → loadAuctions()
 *   → send GET_AUCTIONS qua socket
 *   → nhận GET_AUCTIONS_RESPONSE
 *   → render vào TableView
 */
public class AuctionListController implements MessageListener {

    @FXML private TableView<AuctionDTO>              tblAuctions;
    @FXML private TableColumn<AuctionDTO, String>    colName;
    @FXML private TableColumn<AuctionDTO, Long>      colCurrentPrice;
    @FXML private TableColumn<AuctionDTO, Long>      colMinIncrement;
    @FXML private TableColumn<AuctionDTO, String>    colStatus;
    @FXML private TableColumn<AuctionDTO, String>    colEndTime;
    @FXML private Button                             btnRefresh;
    @FXML private Label                              lblStatus;
    @FXML private TextField                          txtSearch;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private final ObservableList<AuctionDTO> masterList  = FXCollections.observableArrayList();
    private final ObservableList<AuctionDTO> displayList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupSearch();
        socket.addListener(this);
        loadAuctions();
    }

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colMinIncrement.setCellValueFactory(new PropertyValueFactory<>("minIncrement"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        tblAuctions.setItems(displayList);
        tblAuctions.setPlaceholder(new Label("Chưa có phiên đấu giá nào đang mở."));
    }

    private void setupSearch() {
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, old, newVal) -> filterList(newVal));
        }
    }

    // ------------------------------------------------------------------
    // Load dữ liệu
    // ------------------------------------------------------------------

    private void loadAuctions() {
        if (lblStatus != null) lblStatus.setText("Đang tải...");

        ClientMessage msg = new ClientMessage("GET_AUCTIONS", null);
        String jsonRequest = JsonUtils.toJson(msg);

        new Thread(() -> {
            List<AuctionDTO> auctions = null;
            String error = null;
            try {
                String jsonResponse = socket.sendAndReceive(jsonRequest);
                if (jsonResponse != null) {
                    ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);
                    if ("GET_AUCTIONS_RESPONSE".equals(serverMsg.getAction())) {
                        String rawData = JsonUtils.toJson(serverMsg.getData());
                        ApiResponse<?> resp = JsonUtils.fromJson(rawData, ApiResponse.class);
                        if (resp.isSuccess()) {
                            String listJson = JsonUtils.toJson(resp.getData());
                            AuctionListResponse listResp = JsonUtils.fromJson(listJson, AuctionListResponse.class);
                            auctions = listResp.getAuctions();
                        } else {
                            error = resp.getMessage();
                        }
                    }
                }
            } catch (Exception e) {
                error = "Lỗi kết nối: " + e.getMessage();
                e.printStackTrace();
            }

            final List<AuctionDTO> finalAuctions = auctions;
            final String finalError = error;
            Platform.runLater(() -> updateUI(finalAuctions, finalError));
        }).start();
    }

    private void updateUI(List<AuctionDTO> auctions, String error) {
        if (auctions != null) {
            masterList.setAll(auctions);
            filterList(txtSearch != null ? txtSearch.getText() : "");
            if (lblStatus != null) {
                lblStatus.setText("Tổng: " + auctions.size() + " phiên");
            }
        } else {
            if (lblStatus != null) {
                lblStatus.setText("Lỗi: " + (error != null ? error : "Không xác định"));
            }
        }
    }

    private void filterList(String keyword) {
        displayList.clear();
        if (keyword == null || keyword.isBlank()) {
            displayList.addAll(masterList);
        } else {
            String lower = keyword.toLowerCase();
            masterList.stream()
                    .filter(a -> a.getName() != null && a.getName().toLowerCase().contains(lower))
                    .forEach(displayList::add);
        }
    }

    // ------------------------------------------------------------------
    // MessageListener — nhận realtime update từ server (Bid events)
    // ------------------------------------------------------------------

    @Override
    public void onMessageReceived(String json) {
        try {
            ClientMessage msg = JsonUtils.fromJson(json, ClientMessage.class);
            if (msg == null) return;

            // Khi có bid mới → refresh list để cập nhật currentPrice
            if ("PLACE_BID_RESPONSE".equals(msg.getAction())
                    || "BID_UPDATE".equals(msg.getAction())) {
                loadAuctions();
            }
        } catch (Exception e) {
            // Bỏ qua lỗi parse để không crash UI
        }
    }

    // ------------------------------------------------------------------
    // FXML Actions
    // ------------------------------------------------------------------

    @FXML
    private void handleRefresh() {
        loadAuctions();
    }
}
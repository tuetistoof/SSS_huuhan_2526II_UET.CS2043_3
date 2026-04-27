package com.ssscloud.auction.client.controller;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.util.JsonUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.client.networking.*;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class BiddingRoomController implements MessageListener {

    @FXML private Button    btnBack;
    @FXML private Label     lblUserName;
    @FXML private Label     lblCurrentPrice;
    @FXML private Label     lblBidderName;

    // manual bidding
    @FXML private TextField txtBidAmount;
    @FXML private Button    btnPlaceBid;
    @FXML private Label     lblMinIncrementHint;

    // auto bidding
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Button    btnStartAutoBid;

    private boolean    isAutoBidding  = false;
    private AuctionDTO currentAuction;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();

    public void initialize() {
        socket.addListener(this);
    }

    // ------------------------------------------------------------------
    // Manual bid
    // ------------------------------------------------------------------

    @FXML
    private void handlePlaceBid() {
        String amountText = txtBidAmount.getText().trim();
        if (amountText.isEmpty()) {
            showError("Vui lòng nhập số tiền muốn đặt.");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ.");
            return;
        }
        if (amount <= 0) {
            showError("Số tiền đặt phải lớn hơn 0.");
            return;
        }
        if (amount <= currentAuction.getCurrentPrice()) {
            showError("Giá phải cao hơn giá hiện tại.");
            return;
        }
        if (amount < currentAuction.getCurrentPrice() + currentAuction.getMinIncrement()) {
            showError("Giá phải cao hơn ít nhất bước giá tối thiểu.");
            return;
        }

        txtBidAmount.clear();
        PlaceBidRequest req = new PlaceBidRequest(currentAuction.getId(), amount);
        socket.send(JsonUtils.toJson(ClientMessage.request("PLACE_BID", req)));
        // Không chờ response — nếu thành công server push BID_UPDATE đến tất cả client trong phiên
        // Nếu thất bại server push BID_ERROR về riêng client này → handleServerPush() xử lý
    }

    // ------------------------------------------------------------------
    // Auto bid
    // ------------------------------------------------------------------

    @FXML
    private void handleStartAutoBid() {
        if (txtMaxBid.getText().isEmpty() || txtIncrement.getText().isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin Auto Bidding.");
            return;
        }
        long maxBid, increment;
        try {
            maxBid    = Long.parseLong(txtMaxBid.getText().trim());
            increment = Long.parseLong(txtIncrement.getText().trim());
        } catch (NumberFormatException e) {
            showError("Giá tối đa hoặc bước giá không hợp lệ.");
            return;
        }
        if (maxBid <= currentAuction.getCurrentPrice()) {
            showError("Giá tối đa phải cao hơn giá hiện tại.");
            return;
        }

        btnStartAutoBid.setDisable(true);
        btnStartAutoBid.setText("Đang đăng ký...");

        new Thread(() -> {
            try {
                AutoBidRequest req = new AutoBidRequest(currentAuction.getId(), maxBid, increment);
                String responseJson = socket.sendAndReceive(
                        JsonUtils.toJson(ClientMessage.request("AUTO_BID", req)));

                Platform.runLater(() -> {
                    if (responseJson == null) {
                        showError("Không nhận được phản hồi từ server.");
                        resetAutoBidButton();
                        return;
                    }
                    ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                    String dataJson = JsonUtils.toJson(serverMsg.getData());
                    ApiResponse<?> response = JsonUtils.fromJson(dataJson, ApiResponse.class);

                    if (response != null && response.isSuccess()) {
                        isAutoBidding = true;
                        btnStartAutoBid.setText("Auto Bidding...");
                        // Nút giữ disable — AUTO_BID_STOPPED push sẽ reset lại
                    } else {
                        showError(response != null ? response.getMessage() : "Đăng ký Auto Bid thất bại.");
                        resetAutoBidButton();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi kết nối Server.");
                    resetAutoBidButton();
                });
            }
        }).start();
    }

    // ------------------------------------------------------------------
    // Server push
    // ------------------------------------------------------------------

    @Override
    public void onMessageReceived(String jsonMessage) {
        Platform.runLater(() -> handleServerPush(jsonMessage));
    }

    private void handleServerPush(String json) {
        try {
            JsonObject root   = JsonParser.parseString(json).getAsJsonObject();
            String     action = root.has("action") ? root.get("action").getAsString() : "";

            switch (action.toUpperCase()) {
                case "BID_UPDATE":       handleBidUpdate(root);      break;
                case "BID_ERROR":        handleBidError(root);       break;
                case "AUCTION_ENDED":    handleAuctionEnded(root);   break;
                case "AUTO_BID_STOPPED": handleAutoBidStopped(root); break;
                default: break;
            }
        } catch (Exception e) {
            System.err.println("Lỗi xử lý server push: " + e.getMessage());
        }
    }

    private void handleBidUpdate(JsonObject root) {
        if (!root.has("data")) return;
        JsonObject data = root.get("data").getAsJsonObject();

        long currentPrice = data.has("currentPrice") ? data.get("currentPrice").getAsLong() : 0;
        String bidderUsername = data.has("bidderUsername") ? data.get("bidderUsername").getAsString() : "";

        lblCurrentPrice.setText(String.format("%,d VND", currentPrice));
        lblBidderName.setText(bidderUsername);
        currentAuction.setCurrentPrice(currentPrice);
        resetPlaceBidButton();
    }

    private void handleBidError(JsonObject root) {
        String message = "Đặt giá thất bại.";
        if (root.has("data") && root.get("data").isJsonObject()) {
            JsonObject data = root.get("data").getAsJsonObject();
            if (data.has("message")) {
                message = data.get("message").getAsString();
            }
        }
        showError(message);
        resetPlaceBidButton();
    }

    private void handleAuctionEnded(JsonObject root) {
        btnPlaceBid.setDisable(true);
        btnStartAutoBid.setDisable(true);
        txtBidAmount.setDisable(true);

        String winner = root.has("data") && root.get("data").getAsJsonObject().has("winner")
                ? root.get("data").getAsJsonObject().get("winner").getAsString()
                : "Không xác định";

        showInfo("Phiên đấu giá đã kết thúc. Người thắng: " + winner);
    }

    private void handleAutoBidStopped(JsonObject root) {
        isAutoBidding = false;
        resetAutoBidButton();
        showInfo("Auto Bidding đã dừng (đã đạt giá tối đa).");
    }

    // ------------------------------------------------------------------
    // Setters — màn hình trước inject context
    // ------------------------------------------------------------------

    public void setAuction(AuctionDTO auction) {
        this.currentAuction = auction;
        subscribeToAuction();
    }

    private void subscribeToAuction() {
        new Thread(() -> {
            try {
                String json = JsonUtils.toJson(
                        ClientMessage.request("SUBSCRIBE_AUCTION", currentAuction.getId()));
                socket.send(json);
            } catch (Exception e) {
                System.err.println("[BiddingRoom] Lỗi subscribe: " + e.getMessage());
            }
        }).start();
    }

    public void cleanup() {
        socket.removeListener(this);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void resetPlaceBidButton() {
        btnPlaceBid.setDisable(false);
        btnPlaceBid.setText("Place Bid");
    }

    private void resetAutoBidButton() {
        btnStartAutoBid.setDisable(false);
        btnStartAutoBid.setText("Start Auto Bid");
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
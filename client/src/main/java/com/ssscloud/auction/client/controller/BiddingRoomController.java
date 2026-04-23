package com.ssscloud.auction.client.controller;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;


import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.util.JsonUtils;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import  com.ssscloud.auction.client.networking.*;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;       
import javafx.scene.control.TextField;

/**
 * BiddingRoomController — màn hình đấu giá trực tiếp.
 *
 * Implement MessageListener vì đây là màn hình duy nhất cần nhận push từ server.
 * Các action nhận qua push: BID_UPDATE, AUCTION_ENDED, AUTO_BID_STOPPED.
 *
 * Luồng PLACE_BID dùng send() — fire-and-forget, không cần chờ response.
 * Server sau đó push BID_UPDATE tới tất cả client → UI update từ push đó,
 * không update từ response riêng.
 *
 * Luồng AUTO_BID dùng sendAndReceive() — cần biết ngay có đăng ký thành công
 * không để hiển thị trạng thái cho user.
 */
public class BiddingRoomController implements MessageListener{
    @FXML private Button btnBack;
    @FXML private Label lblUserName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblBidderName;

    //manual bidding
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Label lblMinIncrementHint;
    //auto
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Button btnStartAutoBid;
    
  
    private boolean isAutoBidding = false;      
    //inject từ màn hình trước
    private AuctionDTO currentAuction;
    private String currentUserId;
    private String currentUserName;
    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();

    public void initialize() {
        socket.addListener(this);
    }

    @FXML
    private void handlePlaceBid() {
        String amountText = txtBidAmount.getText();
        long amount = Long.parseLong(amountText);
        if (amountText.isEmpty()) {
            showError("Vui lòng nhập số tiền muốn đặt.");
            return;
        }
        if (amount <= 0){
            showError("Số tiền đặt phải lớn hơn 0");
            return;
        }
        if (amount <= currentAuction.getCurrentPrice()) {
            showError("Giá phải cao hơn giá hiện tại");
            return;
        }

        if (amount < currentAuction.getCurrentPrice() + currentAuction.getMinIncrement()) {
            showError("Giá phải cao hơn ít nhất bước giá tối thiểu");
            return;
        }

        txtBidAmount.clear();
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("Đang xử lý..."); 
        
        new Thread(()-> {
            try{
                PlaceBidRequest req = new PlaceBidRequest(currentAuction.getId(), amount);
                String jsonResponse = socket.sendAndReceive(JsonUtils.toJson(ClientMessage.request("PLACE_BID", req)));
                
                if (jsonResponse != null && !jsonResponse.isEmpty()) {
                    ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);

                    if ("PLACE_BID_RESPONSE".equals(serverMsg.getAction())) {
                        String responseRawData = JsonUtils.toJson(serverMsg.getData());
                        Type type = new TypeToken<ApiResponse<BidDTO>>() {}.getType();
                        ApiResponse<BidDTO> response = JsonUtils.fromJsonGeneric(responseRawData, type);

                        if (response != null && response.isSuccess()) {
                            // Cập nhật UI thành công tại đây
                            Platform.runLater(() -> {
                                BidDTO bidResult = response.getData();
                                // Logic cập nhật giao diện
                            });

                    } else {
                        Platform.runLater(() -> {
                            showError(response != null ? response.getMessage() : "Lỗi không xác định");
                            resetPlaceBidButton();
                        });
                    }
                }
            }
    
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi kết nối Server.");
                    resetPlaceBidButton();
                });
            }
        }).start();
    }

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
                String json = JsonUtils.toJson(ClientMessage.request("AUTO_BID", req));
                String responseJson = socket.sendAndReceive(json); // cần biết có thành công không
 
                Platform.runLater(() -> {
                    if (responseJson == null) {
                        showError("Không nhận được phản hồi từ server.");
                        resetAutoBidButton();
                        return;
                    }
                    // Unwrap ClientMessage wrapper
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


    
    public void onMessageReceived(String jsonMessage){
        Platform.runLater(()-> handleServerPush(jsonMessage));
   }

    private void handleServerPush(String json){
        try {
            JsonObject root   = JsonParser.parseString(json).getAsJsonObject();
            String     action = root.has("action") ? root.get("action").getAsString() : "";
 
            switch (action.toUpperCase()) {
                case "BID_UPDATE":       handleBidUpdate(root);       break;
                case "AUCTION_ENDED":    handleAuctionEnded(root);    break;
                case "AUTO_BID_STOPPED": handleAutoBidStopped(root);  break;
                default:
                    // Action khác không liên quan đến màn hình này — bỏ qua
                    break;
            }
        } catch (Exception e) {
            System.err.println("Lỗi xử lý server push: " + e.getMessage());
        }
    }
    private void handleBidUpdate(JsonObject root) {
        BidDTO bid = JsonUtils.fromJson(JsonUtils.toJson(root.get("data")), BidDTO.class);
        if (bid == null) return;
 
        lblCurrentPrice.setText(String.format("%,d VND", bid.getCurrentPrice()));
        lblBidderName.setText(bid.getBidderUsername());
 
        // Đồng bộ local state
        currentAuction.setCurrentPrice(bid.getCurrentPrice());
        resetPlaceBidButton();
    }
    private void handleAuctionEnded(JsonObject root) {
        // Khóa toàn bộ UI đấu giá
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


    // Setters — màn hình trước inject context 
    public void setAuction(AuctionDTO auction)  { this.currentAuction  = auction; }
    public void setUserId(String userId)         { this.currentUserId   = userId; }
    public void setUserName(String userName)     { this.currentUserName = userName; }
 
    // Cleanup khi rời phòng
    public void cleanup() {
        socket.removeListener(this);
    }
 
    //Helpers
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
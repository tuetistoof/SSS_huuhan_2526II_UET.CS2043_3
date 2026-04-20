package com.ssscloud.auction.client.controller;


import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import  com.ssscloud.auction.client.networking.*;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;       
import javafx.scene.control.TextField;


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

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
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

        new Thread(()-> {
            try{
                PlaceBidRequest bidDto = new PlaceBidRequest(currentAuction.getId(), amount);
                String json = JsonUtils.toJson(bidDto);
                AuctionClientSocket.getInstance().send(json);  //gửi qua socket
            } catch(Exception e){
                Platform.runLater(() -> showError("Lỗi kết nối Server"));
            }
        }).start();

        txtBidAmount.clear();
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("Đang xử lý...");    
    }

    // @FXML
    // private void handleStartAutoBid(){
    //     if (txtMaxBid.getText().isEmpty() || txtIncrement.getText().isEmpty()) {
    //         showError("Vui lòng nhập đầy đủ thông tin cho Auto Bidding");
    //         return;
    //     }
    //     long maxBid;
    //     long increment;
    //     try {
    //         maxBid = Long.parseLong(txtMaxBid.getText().trim());
    //         increment = Long.parseLong(txtIncrement.getText().trim());
    //     } catch (NumberFormatException e) {
    //         showError("Giá tối đa hoặc bước giá không hợp lệ.");
    //         return;
    //     }

    //     if (maxBid <= currentAuction.getCurrentPrice()) {
    //         showError("Giá tối đa phải cao hơn giá hiện tại.");
    //         return;
    //     }
    //     isAutoBidding = true;
    //     btnStartAutoBid.setDisable(true);
    //     btnStartAutoBid.setText("Auto Bidding...");

    //     // Gửi yêu cầu Auto Bid lên Server
    //     new Thread(() -> {
    //         try {
    //             AutoBidRequest req = new AutoBidRequest(currentAuction.getId(), maxBid, increment, currentUserId);
    //             ClientMessage msg = new ClientMessage("START_AUTO_BID", req);
    //             socket.send(JsonUtils.toJson(msg));
    //         } catch (Exception e) {
    //             Platform.runLater(() -> showError("Lỗi khởi động Auto Bid"));
    //         }
    //     }).start();


    //}
    
    public void onMessageReceived(String jsonMessage){
        Platform.runLater(()-> handleServerMessage(jsonMessage));
   }

    private void handleServerMessage(String jsonMessage){
        try {
            ClientMessage msg = JsonUtils.fromJson(jsonMessage, ClientMessage.class); //là dto wrap 1 messgae từ server gửi lên client
            if (msg == null) return;

            if ("BID_UPDATE".equalsIgnoreCase(msg.getAction())) {
                    BidDTO bid = JsonUtils.fromJson(String.valueOf(msg.getData()), BidDTO.class);
                    if (bid != null) {
                        Platform.runLater(()-> {
                            // Cập nhật UI
                            lblCurrentPrice.setText(bid.getBidAmount() + " VND");
                            //lblHighestBidder.setText("Leading: " + bid.getBidderUsername());
                        });
                    }
                }
        } catch (Exception e) {
            System.err.println("Lỗi parse BID_UPDATE: " + e.getMessage());
        }
    }

    public void cleanup() { //sau khi rời phòng nên remove listener
        socket.removeListener(this);
    }
}



    

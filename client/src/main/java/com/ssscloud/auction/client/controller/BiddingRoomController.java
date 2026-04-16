package com.ssscloud.auction.client.controller;

import java.lang.classfile.Label;

import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.util.JsonUtils;
import  com.ssscloud.auction.client.networking.*;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;


public class BiddingRoomController {
    @FXML private Button btnBack;
    @FXML private Label lblUserName;

    // Thông tin giá hiện tại
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblBidderName;

    //manual bidding
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Label lblMinIncrementHint;

    //auto biddinf
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Button btnStartAutoBid;
    
  
    private boolean isAutoBidding = false;          // giả sử
    private String currentAuctionId = "AUCTION_01"; // Giả sử id phiên

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        String amountText = txtBidAmount.getText();
        //if (amountText.isEmpty()) {
  

        
        long amount = Long.parseLong(amountText);
        

        PlaceBidRequest bidDto = new PlaceBidRequest(currentAuctionId, amount);

        String json = JsonUtils.toJson(bidDto);
        //AuctionClientNetwork.getInstance().send(json);  gửi qua socket
        txtBidAmount.clear();
    }
}
    

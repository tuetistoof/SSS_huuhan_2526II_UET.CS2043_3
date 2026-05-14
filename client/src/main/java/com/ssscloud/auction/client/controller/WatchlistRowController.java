package com.ssscloud.auction.client.controller;

import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.time.format.DateTimeFormatter;

public class WatchlistRowController {
    @FXML private Label lblAuctionName, lblItemType, lblSeller, lblCurrentPrice, lblEndTime;

    private AuctionDisplayInfoDTO data;
    private Runnable onUnfollowSuccess;
    private Runnable onViewRoom;

    public void setData(AuctionDisplayInfoDTO data) {
        this.data = data;
        lblAuctionName.setText(data.getAuctionName());
        lblItemType.setText(data.getItemType());
        lblSeller.setText("Người bán: " + data.getSellerUsername());

        lblCurrentPrice.setText(String.format("%,d ₫", data.getCurrentPrice()));
        if (data.getEndTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            lblEndTime.setText(data.getEndTime().format(formatter));
        }
    }

    public void setOnUnfollowSuccess(Runnable callback) { this.onUnfollowSuccess = callback; }
    public void setOnViewRoom(Runnable callback) { this.onViewRoom = callback; }

    @FXML
    private void handleEnter() {
        if (onViewRoom != null) onViewRoom.run();
    }

    @FXML
    private void handleUnfollow() {
        new Thread(() -> {
            try {
                String json = JsonUtils.toJson(ClientMessage.request("UNFOLLOW_AUCTION", data.getId()));
                String resp = AuctionClientSocket.getInstance().sendAndReceive(json);
                
                if (resp != null) {
                    Platform.runLater(() -> {
                        if (onUnfollowSuccess != null) onUnfollowSuccess.run();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
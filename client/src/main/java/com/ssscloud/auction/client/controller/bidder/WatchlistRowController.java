package com.ssscloud.auction.client.controller.bidder;

import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.response.DTO.BidderDisplayDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.time.format.DateTimeFormatter;


public class WatchlistRowController {
    @FXML private Label lblAuctionName, lblItemType, lblSeller, lblCurrentPrice, lblEndTime;
    @FXML private StackPane imgThumb;

    private BidderDisplayDTO data;
    private Runnable onUnfollowSuccess;
    private Runnable onViewRoom;
    private String finalImageURL;

    public void setData(BidderDisplayDTO data) {
        this.data = data;
        lblAuctionName.setText(data.getAuctionName());
        lblItemType.setText(data.getItemType());
        lblSeller.setText("Người bán: " + data.getSellerUsername());

        lblCurrentPrice.setText(String.format("%,d ₫", data.getCurrentPrice()));
        if (data.getEndTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            lblEndTime.setText(data.getEndTime().format(formatter));
        }
          if (data.getImageUrl() != null && !data.getImageUrl().isEmpty()) {
            finalImageURL = data.getImageUrl().get(0);
        } else {
            finalImageURL = "https://i.pinimg.com/736x/14/dd/b1/14ddb197526f8ca30d420c750f32d36c.jpg";
            System.out.println("Cảnh báo: Phòng đấu giá " + data.getId() + " đang bị NULL dữ liệu hàng hóa!");
        }
        setRowImage(finalImageURL);
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

    private void setRowImage(String url) {
        String cssLayout = "-fx-background-image: url('" + finalImageURL + "'); " +
                       "-fx-background-color: #f0f0f0; " +
                       "-fx-background-position: center center; " +
                       "-fx-background-radius: 8px; " +
                       "-fx-background-size: cover; ";
                       
        imgThumb.setStyle(cssLayout);
    }
}
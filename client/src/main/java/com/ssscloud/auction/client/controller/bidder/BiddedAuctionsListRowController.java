package com.ssscloud.auction.client.controller.bidder;

import java.time.format.DateTimeFormatter;

import com.ssscloud.auction.common.payload.response.DTO.BidderDisplayDTO;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class BiddedAuctionsListRowController {

    @FXML private Button btnEnter;
    @FXML private StackPane imgThumb;
    @FXML private Label lblAuctionName, lblCurrentPrice, lblEndTime, lblItemType, lblMyBid, lblSeller, lblStatus;
    @FXML private HBox rowRoot;

    private BidderDisplayDTO data;
    private Runnable onViewRoom;
    private String finalImageURL;

    public void setData(BidderDisplayDTO data) {
        this.data = data;
        lblAuctionName.setText(data.getAuctionName());
        lblItemType.setText(data.getItemType());
        lblSeller.setText("Seller: " + data.getSellerUsername());
        if (data.getMyLastBid() > 0) {
            lblMyBid.setText("Your bid: " + String.format("%,d ₫", data.getMyLastBid()));
            lblStatus.setText(data.isLeading() ? "▲ Leading" : "▼ Outbid");
            lblStatus.getStyleClass().removeAll("bda-leading-status", "bda-outbid-status");
            if (data.isLeading()) {
                lblStatus.getStyleClass().add("bda-leading-status");
            } else {
                lblStatus.getStyleClass().add("bda-outbid-status");
            }
            lblMyBid.setVisible(true);
            lblStatus.setVisible(true);
        } else {
            lblMyBid.setVisible(false);
            lblStatus.setVisible(false);
        }

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

    public void setOnViewRoom(Runnable callback) { 
        this.onViewRoom = callback; 
    }
    
    @FXML
    private void handleEnter() {
        if (onViewRoom != null) onViewRoom.run();
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

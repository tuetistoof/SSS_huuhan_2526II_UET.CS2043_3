package com.ssscloud.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;

import com.ssscloud.auction.common.dto.response.AuctionDTO;

public class AuctionCardController {


    @FXML private ImageView imgItem;
    @FXML private Label lblAuctionName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblItemName;
    @FXML private Label lblSellerName;

    public void setAuctionData(AuctionDTO auction) {
        String finalImageURL = "https://i.pinimg.com/736x/14/dd/b1/14ddb197526f8ca30d420c750f32d36c.jpg";
        String itemName = "Không rõ tên hàng";
        if (auction.getItemData() != null) {
            itemName = auction.getItemData().getName();

            if (auction.getItemData().getImageUrls() != null && !auction.getItemData().getImageUrls().isEmpty()) {
                finalImageURL = auction.getItemData().getImageUrls().get(0);
            } else {
                System.out.println("Cảnh báo: Phòng đấu giá " + auction.getId() + " đang bị NULL dữ liệu hàng hóa!");
            }
        }

        lblAuctionName.setText(auction.getName());
        lblCurrentPrice.setText(auction.getCurrentPrice() + " VND");
        lblItemName.setText(itemName);
        lblSellerName.setText("By " + auction.getSellerName());
        
        Image image = new Image(finalImageURL, true);
        imgItem.setImage(image);
        double width = 200;
        double height = 160;

        imgItem.setFitWidth(width);
        imgItem.setFitHeight(height);
        imgItem.setPreserveRatio(false);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(width, height);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imgItem.setClip(clip);
    }
}

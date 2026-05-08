package com.ssscloud.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import com.ssscloud.auction.common.dto.response.AuctionDTO;

public class AuctionCardController {

    @FXML private ImageView imgItem;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblItemName;
    @FXML private Label lblEndTime;
    @FXML private Label lblSellerUsername;
    @FXML private VBox auctionCard;

    public void setAuctionData(AuctionDTO auction, Consumer<AuctionDTO> onClickListener) {
        String finalImageURL = "https://i.pinimg.com/736x/14/dd/b1/14ddb197526f8ca30d420c750f32d36c.jpg";
        String itemName = "Không rõ tên hàng";
        String SellerUsername = "Không rõ người bán";
        if (auction.getItemData() != null) {
            itemName = auction.getItemData().getName();

            if (auction.getItemData().getImageUrls() != null && !auction.getItemData().getImageUrls().isEmpty()) {
                finalImageURL = auction.getItemData().getImageUrls().get(0);
            } else {
                System.out.println("Cảnh báo: Phòng đấu giá " + auction.getId() + " đang bị NULL dữ liệu hàng hóa!");
            }
        }

        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
        lblCurrentPrice.setText(formatter.format(auction.getCurrentPrice()) + " VND");

        lblItemName.setText(auction.getName() + " " + itemName);
        lblEndTime.setText(formatTimeLeft(auction.getEndTime()));
        if (auction.getSellerName() != null) {
            SellerUsername = auction.getSellerName();
        }
        lblSellerUsername.setText(SellerUsername);

        Image image = new Image(finalImageURL, true);
        imgItem.setImage(image);
        double width = 300;
        double height = 250;

        imgItem.setFitWidth(width);
        imgItem.setFitHeight(height);
        imgItem.setPreserveRatio(false);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(width, height);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imgItem.setClip(clip);

        auctionCard.setOnMouseClicked(event -> {
            if (onClickListener != null) {
                onClickListener.accept(auction);
            }
        });
    }

    private String formatTimeLeft(LocalDateTime endTime) {
        if (endTime == null) return "Không rõ thời gian";
        Duration duration = Duration.between(LocalDateTime.now(), endTime);
        if (duration.isNegative() || duration.isZero()) {
            return "Đã kết thúc";
        }

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            lblEndTime.setStyle("-fx-text-fill: #888888;");
            return "Còn " + days + " ngày " + hours + " giờ";
        } else if (hours > 0) {
            // Dưới 1 ngày -> set màu cam ở đây
            lblEndTime.setStyle("-fx-text-fill: orange;"); 
            return "Còn " + hours + " giờ " + minutes + " phút";
        } else {
            // Dưới 1 giờ -> Set màu đỏ
            lblEndTime.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return "Sắp kết thúc (" + minutes + " phút)";
        }
    }
}

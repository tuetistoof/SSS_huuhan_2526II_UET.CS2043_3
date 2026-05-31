package com.ssscloud.auction.client.controller.bidder;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import com.ssscloud.auction.common.payload.response.DTO.BidderDisplayDTO;

public class AuctionCardController {

    @FXML private StackPane imgItem;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblItemName;
    @FXML private Label lblEndTime;
    @FXML private Label lblSellerUsername;
    @FXML private VBox auctionCard;

    public void setAuctionDisplayData(BidderDisplayDTO auctionInfo, Consumer<BidderDisplayDTO> onClickListener) {
        String finalImageURL = "https://i.pinimg.com/736x/14/dd/b1/14ddb197526f8ca30d420c750f32d36c.jpg";
        String auctionName = "Không rõ tên";
        String itemName = "Không rõ tên hàng";
        String SellerUsername = "Không rõ người bán";
        if (auctionInfo.getAuctionName() != null) {
            auctionName = auctionInfo.getAuctionName();
        }
        if (auctionInfo.getSellerUsername() != null) {
            SellerUsername = auctionInfo.getSellerUsername();
        }
        if (auctionInfo.getItemName() != null) {
            itemName = auctionInfo.getItemName();

            if (auctionInfo.getImageUrl() != null && !auctionInfo.getImageUrl().isEmpty()) {
                finalImageURL = auctionInfo.getImageUrl().get(0);
            } else {
                System.out.println("Cảnh báo: Phòng đấu giá " + auctionInfo.getId() + " đang bị NULL dữ liệu hàng hóa!");
            }
        }

        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
        lblCurrentPrice.setText(formatter.format(auctionInfo.getCurrentPrice()) + " VND");

        lblItemName.setText(auctionName + " " + itemName);
        lblEndTime.setText(formatTimeLeft(auctionInfo.getEndTime()));
       
        lblSellerUsername.setText(SellerUsername);

        String cssLayout = "-fx-background-image: url('" + finalImageURL + "'); " +
                       "-fx-background-size: cover; " +
                       "-fx-background-position: center center; " +
                       "-fx-background-radius: 8 8 0 0;";
                       
        imgItem.setStyle(cssLayout);

        auctionCard.setOnMouseClicked(event -> {
            if (onClickListener != null) {
                onClickListener.accept(auctionInfo);
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
            return "Còn " + days + " ngày ";
        } else if (hours > 0) {
            lblEndTime.setStyle("-fx-text-fill: orange;"); 
            return "Còn " + hours + " giờ ";
        } else {
            lblEndTime.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return "Sắp kết thúc (" + minutes + " phút)";
        }
    }
}

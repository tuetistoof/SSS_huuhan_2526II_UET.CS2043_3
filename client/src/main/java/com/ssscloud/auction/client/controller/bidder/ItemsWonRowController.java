package com.ssscloud.auction.client.controller.bidder;

import java.time.format.DateTimeFormatter;

import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * Controller cho từng row trong Won Items list.
 * Pattern giống BiddedAuctionsListRowController — setData() + callback.
 */
public class ItemsWonRowController {

    @FXML private HBox      rowRoot;
    @FXML private StackPane imgThumb;
    @FXML private Button    btnView;

    @FXML private Label lblAuctionName;
    @FXML private Label lblItemType;
    @FXML private Label lblSeller;
    @FXML private Label lblWinningPrice;
    @FXML private Label lblWonDate;

    private BidderDisplayDTO data;
    private Runnable onViewDetails;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // ── Public API ────────────────────────────────────────────────────────────

    public void setData(BidderDisplayDTO data) {
        this.data = data;

        lblAuctionName.setText(data.getAuctionName());
        lblItemType.setText(data.getItemType());
        lblSeller.setText("Seller: " + data.getSellerUsername());
        lblWinningPrice.setText(String.format("%,d ₫", data.getMyLastBid() > 0
                ? data.getMyLastBid()
                : data.getCurrentPrice()));

        if (data.getEndTime() != null) {
            lblWonDate.setText("Ended: " + data.getEndTime().format(DATE_FMT));
        } else {
            lblWonDate.setText("—");
        }

        // Thumbnail — cùng pattern với WatchlistRowController
        String imageUrl;
        if (data.getImageUrl() != null && !data.getImageUrl().isEmpty()) {
            imageUrl = data.getImageUrl().get(0);
        } else {
            imageUrl = "https://i.pinimg.com/736x/14/dd/b1/14ddb197526f8ca30d420c750f32d36c.jpg";
            System.out.println("Cảnh báo: Won item " + data.getId() + " không có ảnh!");
        }
        setRowImage(imageUrl);
    }

    public void setOnViewDetails(Runnable callback) {
        this.onViewDetails = callback;
    }

    // ── FXML handler ──────────────────────────────────────────────────────────

    @FXML
    private void handleViewDetails() {
        if (onViewDetails != null) onViewDetails.run();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void setRowImage(String url) {
        // Inline style cho background image — đúng pattern của WatchlistRowController
        String cssLayout = "-fx-background-image: url('" + url + "'); "
                + "-fx-background-color: -cloud-surface-alt; "
                + "-fx-background-position: center center; "
                + "-fx-background-radius: 8px; "
                + "-fx-background-size: cover;";
        imgThumb.setStyle(cssLayout);
    }
}
package com.ssscloud.auction.client.controller.bidder;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;

public class BidSuccessToastController {

    @FXML private VBox        toastCard;
    @FXML private Label       lblAuctionName;
    @FXML private Label       lblBidAmount;
    @FXML private ProgressBar progressBar;
    @FXML private Button      btnClose;

    private static final double AUTO_CLOSE_SECONDS = 4.5;
    private static final String SOUND_PATH         = "/sounds/success.mp3";

   private static final NumberFormat CURRENCY_FMT =
        NumberFormat.getIntegerInstance(Locale.of("vi", "VN"));
    private Timeline  autoCloseTimeline;
    private Popup popup;


    public static void show(String auctionName, long amount, Window owner) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(BidSuccessToastController.class.getResource("/fxml/bid-success-toast.fxml"));
                Parent toastNode = loader.load();

                URL stylesheet = BidSuccessToastController.class.getResource("/css/component-toast.css");
                if (stylesheet != null) {
                    toastNode.getStylesheets().add(stylesheet.toExternalForm());
                }
                BidSuccessToastController controller = loader.getController();
                controller.setup(auctionName, amount);

                Popup popup = new Popup();
                popup.getContent().add(toastNode);
                controller.popup = popup;

                double x = owner.getX() + owner.getWidth() - 350;
                double y = owner.getY() + 80;
                popup.show(owner, x, y);

                controller.animateIn();
                controller.playSound();
                Platform.runLater(controller::startAutoClose);

            } catch (IOException e) {
                System.err.println("[BidSuccessToast] Không load được FXML: " + e.getMessage());
            }
        });
    }

    private void setup(String auctionName, long bidAmount) {
        lblAuctionName.setText(auctionName != null ? auctionName : "Phiên đấu giá");
        lblBidAmount.setText(CURRENCY_FMT.format(bidAmount) + " ₫");
        progressBar.setProgress(1.0);
    }


    private void animateIn() {
        toastCard.setOpacity(0);
        toastCard.setTranslateX(340);
        new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(toastCard.translateXProperty(), 340),
                new KeyValue(toastCard.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(220),
                new KeyValue(toastCard.translateXProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(toastCard.opacityProperty(), 1, Interpolator.EASE_OUT))
        ).play();
    }

    private void animateOut() {
        Timeline slideOut = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(toastCard.translateXProperty(), 0),
                new KeyValue(toastCard.opacityProperty(), 1)),
            new KeyFrame(Duration.millis(180),
                new KeyValue(toastCard.translateXProperty(), 340, Interpolator.EASE_IN),
                new KeyValue(toastCard.opacityProperty(), 0, Interpolator.EASE_IN))
        );
        slideOut.setOnFinished(e -> {
            if (popup != null) popup.hide();
        });
        slideOut.play();
    }

    private void startAutoClose() {
        progressBar.setProgress(1.0);
        autoCloseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(progressBar.progressProperty(), 1.0)),
            new KeyFrame(Duration.seconds(AUTO_CLOSE_SECONDS),
                new KeyValue(progressBar.progressProperty(), 0.0, Interpolator.LINEAR))
        );
        autoCloseTimeline.setOnFinished(e -> animateOut());
        autoCloseTimeline.play();
    }


    private void playSound() {
        try {
            URL url = getClass().getResource(SOUND_PATH);
            if (url == null) {
                System.out.println("[BidSuccessToast] Không tìm thấy: " + SOUND_PATH);
                return;
            }
            AudioClip clip = new AudioClip(url.toExternalForm());
            clip.setVolume(0.7);
            clip.play();
        } catch (Exception e) {
            System.err.println("[BidSuccessToast] Lỗi phát âm thanh: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        if (autoCloseTimeline != null) autoCloseTimeline.stop();
        animateOut();
    }
}
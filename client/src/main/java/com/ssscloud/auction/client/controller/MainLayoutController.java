package com.ssscloud.auction.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class MainLayoutController {

    @FXML
    private Button btnHistory;

    @FXML
    private Button btnManageProduct;

    @FXML
    private StackPane contentArea;

    @FXML
    private Label lblUsername;

    @FXML
    private VBox sidebar;
    private boolean isSidebarExpanded = true;
    private final double SIDEBAR_EXPANDED_WIDTH = 200.0;
    private final double SIDEBAR_COLLAPSED_WIDTH = 60.0;

    @FXML
    void handleNavAuction(ActionEvent event) {

    }

    @FXML
    void handleNavHistory(ActionEvent event) {

    }

    @FXML
    void handleNavHome(ActionEvent event) {

    }

    @FXML
    void toggleSidebar(ActionEvent event) {
        Timeline timeline = new Timeline();
        Duration duration = Duration.millis(250);

        if (isSidebarExpanded) {
            KeyValue kv = new KeyValue(sidebar.prefWidthProperty(), SIDEBAR_COLLAPSED_WIDTH);
            KeyFrame kf = new KeyFrame(duration, kv);
            timeline.getKeyFrames().add(kf);

        } else {

            KeyValue kv = new KeyValue(sidebar.prefWidthProperty(), SIDEBAR_EXPANDED_WIDTH);
            KeyFrame kf = new KeyFrame(duration, kv);
            timeline.getKeyFrames().add(kf);
        }

        timeline.play();
        isSidebarExpanded = !isSidebarExpanded;
    }
}
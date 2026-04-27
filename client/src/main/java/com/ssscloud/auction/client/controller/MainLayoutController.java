package com.ssscloud.auction.client.controller;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.common.dto.response.UserDTO;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;


public class MainLayoutController {

    @FXML
    private StackPane contentArea;
    
    // @FXML
    // private HBox hBoxLogo;

    @FXML
    private Label lblAccountBalance;

    @FXML
    private Label lblUsername;

    @FXML
    private HBox navActiveBids;

    @FXML
    private HBox navDashboard;

    @FXML
    private HBox navHistory;

    @FXML
    private HBox navMyAuctionRooms;

    @FXML
    private HBox navMyItems;

    @FXML
    private HBox navNewAuctionRoom;

    @FXML
    private Circle navUserInfo;

    @FXML
    private HBox navWatchlist;

    @FXML
    private HBox navWonItems;

    @FXML
    private VBox sidebar;
    private boolean isSidebarExpanded = true;
    private final double SIDEBAR_EXPANDED_WIDTH = 200.0;
    private final double SIDEBAR_COLLAPSED_WIDTH = 60.0;

    public void initialize() {
        UserDTO user = SessionManager.getInstance().getCurrentUser();
        lblUsername.setText(user.getUsername());
        applyRole(user.getRole());
    }

    private void applyRole(UserRole role) {
        // Ẩn hết trước
        navWonItems.setVisible(false);
        navWonItems.setManaged(false);
        navHistory.setVisible(false);
        navHistory.setManaged(false);
        navWatchlist.setVisible(false);
        navWatchlist.setManaged(false);
        navMyItems.setVisible(false);
        navMyItems.setManaged(false);
        navNewAuctionRoom.setVisible(false);
        navNewAuctionRoom.setManaged(false);
        lblAccountBalance.setVisible(false);
        lblAccountBalance.setManaged(false);
        navMyAuctionRooms.setVisible(false);
        navMyAuctionRooms.setManaged(false);
        navActiveBids.setVisible(false);
        navActiveBids.setManaged(false);

        // Hiện lại đúng role
        switch (role) {
            case BIDDER -> {
                lblAccountBalance.setVisible(true);
                lblAccountBalance.setManaged(true);
                navWatchlist.setVisible(true);
                navWatchlist.setManaged(true);
                navWonItems.setVisible(true);
                navWonItems.setManaged(true);
                navActiveBids.setVisible(true);
                navActiveBids.setManaged(true);
            }
            case SELLER -> {
                navMyItems.setVisible(true);
                navMyItems.setManaged(true);
                navHistory.setVisible(true);
                navHistory.setManaged(true);
                navMyAuctionRooms.setVisible(true);
                navMyAuctionRooms.setManaged(true);
                navNewAuctionRoom.setVisible(true);
                navNewAuctionRoom.setManaged(true);
            }
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {

    }

    @FXML
    void handleNavActiveBids(MouseEvent event) {

    }

    @FXML
    void handleNavDashboard(MouseEvent event) {
        updateActiveStyle(navDashboard); 

        try {
            // Load file Dashboard.fxml
            Parent dashboardView = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));
            
            // Xóa sạch các màn hình cũ trong phần center đi (nếu có)
            contentArea.getChildren().clear();
            
            // Nhét Dashboard vào
            contentArea.getChildren().add(dashboardView);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    void handleNavHistory(MouseEvent event) {

    }

    @FXML
    void handleNavMyAuctionRooms(MouseEvent event) {

    }

    @FXML
    void handleNavMyItems(MouseEvent event) {

    }

    @FXML
    void handleNavNewAuctionRoom(MouseEvent event) {

    }

    @FXML
    void handleNavUserInfo(MouseEvent event) {
        System.out.println("Đã click vào khu vực User Info!");
    }

    private void updateActiveStyle(HBox activeItem) {

        HBox[] allNavItems = {
            navDashboard, navActiveBids, navWatchlist, 
            navWonItems, navHistory, navMyItems, 
            navNewAuctionRoom, navMyAuctionRooms
        };

        // 2. Đi dọn dẹp: Xóa cái class "active" ở TẤT CẢ các menu
        for (HBox item : allNavItems) {
            if (item != null) {
                item.getStyleClass().remove("active-nav");
            }
        }

        if (activeItem != null) {
            if (!activeItem.getStyleClass().contains("active-nav")) {
                activeItem.getStyleClass().add("active-nav");
            }
        }
    }

    @FXML
    void handleNavWatchlist(MouseEvent event) {

    }

    @FXML
    void handleNavWonItems(MouseEvent event) {

    }

    @FXML
    void handleSearching(MouseEvent event) {

    }

    @FXML
    void toggleSidebar(ActionEvent event) {
        Timeline timeline = new Timeline();
        Duration duration = Duration.millis(150);

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

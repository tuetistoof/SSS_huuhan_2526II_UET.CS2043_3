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
import javafx.scene.control.Button;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;


public class MainLayoutController {

    @FXML private StackPane contentArea;
    
    @FXML private HBox hBoxLogo;

    @FXML private Label lblSidebarTitleAB;
    @FXML private Label lblSidebarTitleDB;
    @FXML private Label lblSidebarTitleH;
    @FXML private Label lblSidebarTitleMAR;
    @FXML private Label lblSidebarTitleMI;
    @FXML private Label lblSidebarTitleNAR;
    @FXML private Label lblSidebarTitleW;
    @FXML private Label lblSidebarTitleWI;
    @FXML private Label lblAccountBalance;
    @FXML private Label lblUsername;
    @FXML private Label lblOverview;
    @FXML private Label lblAuction;

    @FXML private HBox navActiveBids;
    @FXML private HBox navDashboard;
    @FXML private HBox navHistory;
    @FXML private HBox navMyAuctionRooms;
    @FXML private HBox navMyItems;
    @FXML private HBox navNewAuctionRoom;
    @FXML private Circle navUserInfo;
    @FXML private HBox navWatchlist;
    @FXML private HBox navWonItems;
    @FXML private Button btnLogOut;

    @FXML private VBox sidebar;
    private boolean isSidebarExpanded = true;
    private final double SIDEBAR_EXPANDED_WIDTH = 200.0;
    private final double SIDEBAR_COLLAPSED_WIDTH = 60.0;

    public void initialize() {
        UserDTO user = SessionManager.getInstance().getCurrentUser();
        lblUsername.setText(user.getUsername());
        applyRole(user.getRole());
        handleNavDashboard(null);
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
        updateActiveStyle(navNewAuctionRoom); 
        FXMLLoader loader = new FXMLLoader();
        try {
            loader = new FXMLLoader(getClass().getResource("/fxml/create-auction.fxml"));
            Parent createAuctionView = loader.load();
            CreateAuctionController controller = loader.getController();
            controller.setOnSuccessCallback(() -> {
                loadBiddingRoom();
            });

            contentArea.getChildren().clear();
            contentArea.getChildren().add(createAuctionView);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi load file create-auction.fxml");
        }
    }

    private void loadBiddingRoom() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bidding-room.fxml")); 
            Parent biddingRoomView = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(biddingRoomView);
            
            // (Nâng cao) Chỗ này sau lấy controller của BiddingRoom 
            // để bơm ID phòng hoặc Dữ liệu phòng vào
            // BiddingRoomController bidCtrl = loader.getController();
            // bidCtrl.setAuctionData(newAuctionData);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi load file bidding-room.fxml");
        }
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

        double targetWidth = isSidebarExpanded ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;

        KeyValue kvPref = new KeyValue(sidebar.prefWidthProperty(), targetWidth);
        KeyValue kvMin = new KeyValue(sidebar.minWidthProperty(), targetWidth);
        KeyValue kvMax = new KeyValue(sidebar.maxWidthProperty(), targetWidth);

        KeyFrame kf = new KeyFrame(duration, kvPref, kvMin, kvMax);
        timeline.getKeyFrames().add(kf);

        Label[] navLabels = {
            lblSidebarTitleAB, lblSidebarTitleDB, lblSidebarTitleH,
            lblSidebarTitleMAR, lblSidebarTitleMI, lblSidebarTitleNAR,
            lblSidebarTitleW, lblSidebarTitleWI
        };

        if (isSidebarExpanded) {
            for (Label lbl : navLabels) {
                lbl.setVisible(false);
                lbl.setManaged(false);
            }
            lblOverview.setText("");
            lblAuction.setText("");
            btnLogOut.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        } else {
            timeline.setOnFinished(e -> {
                for (Label lbl : navLabels) {
                    lbl.setVisible(true);
                    lbl.setManaged(true);
                }
                lblOverview.setText("OVERVIEW"); 
                lblAuction.setText("AUCTION");
                btnLogOut.setContentDisplay(ContentDisplay.LEFT);
            });
        }

        timeline.play();
        isSidebarExpanded = !isSidebarExpanded;
    }

}

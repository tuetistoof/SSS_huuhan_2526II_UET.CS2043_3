package com.ssscloud.auction.client.controller;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.GetAuctionDetailsRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.dto.response.UserDTO;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Popup;
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

    @FXML private Label lblBellBadge;
    @FXML private Button btnBell;
    private NotificationController notificationController; 
    private Popup notifPopup;
    private Parent notifPopupRoot; // root đã load sẵn từ fxml, tái sử dụng cho mọi lần show/hide

    @FXML private VBox sidebar;
    @FXML private Parent loading; // Giao diện của khung loading
    @FXML private LoadingController loadingController;

    private boolean isSidebarExpanded = true;
    private final double SIDEBAR_EXPANDED_WIDTH = 200.0;
    private final double SIDEBAR_COLLAPSED_WIDTH = 60.0;

    private Object currentController = null;
    private UserDTO user = SessionManager.getInstance().getCurrentUser();
    private long currentBalance = user.getAccountBalance();

    private AuctionClientSocket socket =  AuctionClientSocket.getInstance();

    private Runnable onSuccessCallback;
    
    java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
 
    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }
    public void initialize() {
        lblUsername.setText(user.getUsername());
        lblAccountBalance.setText("Balance: " + formatter.format(Long.valueOf(user.getAccountBalance())));
        applyRole(user.getRole());
        initNotification();
        handleNavDashboard(null);
    }
    private void initNotification() {
        try {
            FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/notification-popup.fxml"));
            notifPopupRoot = loader.load(); 
            notificationController = loader.getController();
            notificationController.init(this::navigateToAuction);
            // Gắn badge listener: mỗi lần badge thay đổi → cập nhật lblBellBadge
            notificationController.setBadgeListener(count -> {
                if (lblBellBadge != null) {
                    lblBellBadge.setText(count > 0 ? String.valueOf(count) : "");
                    lblBellBadge.setVisible(count > 0);
                    lblBellBadge.setManaged(count > 0);
                }
            });
        } catch (IOException e) {
            System.err.println("Không load được notification-popup.fxml: " + e.getMessage());
        }
    }
    // Khi user click vào 1 notification, sẽ gọi callback này với auctionId tương ứng
    private void navigateToAuction(String auctionId) {
        // Tìm AuctionDTO từ danh sách đang hiển thị hoặc tạo dummy để navigate
        AuctionDisplayInfoDTO dummy = new AuctionDisplayInfoDTO();
        dummy.setId(auctionId);
        loadBiddingRoom(dummy);
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
    //__CLEANUP___

    private void cleanupCurrentController() {
        if (currentController == null) return;
        if      (currentController instanceof BiddingRoomController  c) c.cleanup();
        else if (currentController instanceof AuctionListController  c) c.cleanup();
        currentController = null;
    }
 
    private void clearContent() {
        cleanupCurrentController();
        contentArea.getChildren().clear();
    }

    //__NAVIGATION__
    @FXML
    void handleBell(ActionEvent event) {
        if (notificationController == null || notifPopupRoot == null) return;
        if (notifPopup != null && notifPopup.isShowing()) {
            notifPopup.hide();
            return;
        }
         if (notifPopup == null) {
            notifPopup = new javafx.stage.Popup();
            notifPopup.setAutoHide(true);    
            notifPopup.setAutoFix(true);     
            notifPopup.getContent().add(notifPopupRoot);
        }
 
        // Tính tọa độ từ bell.localToScreen() — căn lề phải với nút chuông
        Node bell = (Node) event.getSource();
        Bounds b = bell.localToScreen(bell.getBoundsInLocal());
        double popupWidth = 340;
        double x = b.getMaxX() - popupWidth;   // căn lề phải
        double y = b.getMaxY() + 6;            // sát bên dưới nút
 
        notifPopup.show(bell.getScene().getWindow(), x, y);
    }
    @FXML
    void handleLogout(ActionEvent event) {
        if (notificationController != null) notificationController.destroy();

    }

    @FXML
    void handleNavActiveBids(MouseEvent event) {

    }

    @FXML
    void handleDeposit(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DepositCard.fxml"));
            Parent root = loader.load();

            DepositCardController ctrl = loader.getController();
            ctrl.setMainLayoutController(this);

            Stage depositStage = new Stage();
            depositStage.setTitle("Nạp tiền vào tài khoản");
            depositStage.setScene(new Scene(root));
            depositStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            depositStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateBalance(long newBalance) {
        currentBalance = newBalance;
        lblAccountBalance.setText("Balance: " + formatter.format(currentBalance));
        SessionManager.getInstance().getCurrentUser().setAccountBalance(newBalance);
    }

    @FXML
    void handleNavDashboard(MouseEvent event) {
        updateActiveStyle(navDashboard);
        clearContent();
        try {
            contentArea.getChildren().clear();
            String fxmlPath = (user.getRole() == UserRole.BIDDER) ? "/fxml/BidderDashboard.fxml" : "/fxml/SellerDashboard.fxml";
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent dashboardView = loader.load();

            if (user.getRole() == UserRole.BIDDER) {
                BidderDashboardController ctrl = loader.getController();
                ctrl.setOnOpenBidRoom(this::loadBiddingRoom);
                currentController = ctrl;
            }
            
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
        updateActiveStyle(navMyAuctionRooms);
        clearContent();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-list.fxml"));
            Parent view = loader.load();
            AuctionListController ctrl = loader.getController();
            // ctrl.setOnOpenAuction(this::loadAuctionList);
            currentController = ctrl;
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    void handleNavMyItems(MouseEvent event) {

    }

    @FXML
    void handleNavNewAuctionRoom(MouseEvent event) {
        updateActiveStyle(navNewAuctionRoom); 
        clearContent();

        FXMLLoader loader = new FXMLLoader();
        try {
            loader = new FXMLLoader(getClass().getResource("/fxml/create-auction.fxml"));
            Parent createAuctionView = loader.load();
            CreateAuctionController controller = loader.getController();
            controller.setOnSuccessCallback(newAuction -> {
                updateActiveStyle(null);
                clearContent();
                try {
                    FXMLLoader roomLoader = new FXMLLoader(getClass().getResource("/fxml/bidding-room.fxml"));
                    Parent view = roomLoader.load();

                    BiddingRoomController ctrl = roomLoader.getController();
                    ctrl.setAuction(newAuction); 
                    ctrl.setOnSuccessCallback(() -> handleNavDashboard(null)); 
                    
                    currentController = ctrl;
                    contentArea.getChildren().add(view);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            contentArea.getChildren().clear();
            contentArea.getChildren().add(createAuctionView);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi load file create-auction.fxml");
        }
    }

    public void loadBiddingRoom(AuctionDisplayInfoDTO basicInfo) {
        if (basicInfo == null) { 
            handleNavDashboard(null);
            return;
        }
        if (loading != null && loadingController != null) {
            loading.setVisible(true);
            loadingController.playAnimation();
        } else {
            System.out.println("Chưa sửa chèn thêm fxml vào 1");
            return;
        }
        new Thread(() -> {
            GetAuctionDetailsRequest req = new GetAuctionDetailsRequest(basicInfo.getId());
            String jsonResponse = socket.sendAndReceive(JsonUtils.toJson(ClientMessage.request("GET_AUCTION_DETAILS", req)));
            
            AuctionDTO fullAuctionData = null;

            if (jsonResponse != null && !jsonResponse.isEmpty()) {
                ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);
                if ("GET_AUCTION_DETAILS_RESPONSE".equals(serverMsg.getAction())) {
                    String responseRawData = JsonUtils.toJson(serverMsg.getData());
                    Type type = new TypeToken<ApiResponse<AuctionDTO>>() {}.getType();
                    ApiResponse<AuctionDTO> response = JsonUtils.fromJsonGeneric(responseRawData, type);

                    if (response != null && response.isSuccess()) {
                        fullAuctionData = response.getData();
                    }
                }
            }
            final AuctionDTO finalData = fullAuctionData;
            Platform.runLater(() -> {
                if (finalData != null) {
                    updateActiveStyle(null);
                    clearContent();
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bidding-room.fxml"));
                        Parent view = loader.load();

                        BiddingRoomController ctrl = loader.getController();
                        ctrl.setAuction(finalData);                          // inject dữ liệu phòng
                        ctrl.setOnSuccessCallback(() -> handleNavDashboard(null)); // Back → dashboard
                        
                        currentController = ctrl;
                        contentArea.getChildren().add(view);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Không lấy được Data phòng");
                }
                if (loading != null) loading.setVisible(false);
            });
        }).start();
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

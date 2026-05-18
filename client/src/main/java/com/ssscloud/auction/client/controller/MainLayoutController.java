package com.ssscloud.auction.client.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Optional;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.networking.MessageListener;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.GetAuctionDetailsRequest;
import com.ssscloud.auction.common.dto.response.AdminDisplayDTO;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;
import com.ssscloud.auction.common.dto.response.SellerDisplayDTO;
import com.ssscloud.auction.common.dto.response.UserDTO;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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


public class MainLayoutController implements MessageListener {

    @FXML private StackPane contentArea;
    
    @FXML private HBox hBoxLogo;

    @FXML private Label lblSidebarTitleAB;
    @FXML private Label lblSidebarTitleDB;
    @FXML private Label lblSidebarTitleNAR;
    @FXML private Label lblSidebarTitleW;
    @FXML private Label lblSidebarTitleWI;
    @FXML private Label lblAccountBalance;
    @FXML private Label lblLockBalance;
    @FXML private Label lblAvailableBalance;
    @FXML private Label lblUsername;
    @FXML private Label lblOverview;
    @FXML private Label lblAuction;

    @FXML private HBox navActiveBids;
    @FXML private HBox navDashboard;
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

    // --- Balance state (tracked in-memory, synced with SessionManager) ---
    private long currentBalance;
    private long currentUnsettledBalance;

    private AuctionClientSocket socket = AuctionClientSocket.getInstance();

    private Runnable onSuccessCallback;
    
    java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
 
    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    public void initialize() {
        currentBalance          = user.getAccountBalance();
        currentUnsettledBalance = user.getUnsettledBalance();

        lblUsername.setText(user.getUsername());

        // Fix: render each label with the correct value from login response
        renderBalanceLabels(currentBalance, currentUnsettledBalance);

        applyRole(user.getRole());
        initNotification();
        handleNavDashboard(null);
        socket.addListener(this);
    }

    // --- Balance update API (called externally by DepositCardController, etc.) ---

    /**
     * Called after a successful deposit to update the total account balance.
     * Re-renders all three balance labels so available balance stays consistent.
     */
    public void updateBalance(long newBalance) {
        currentBalance = newBalance;
        SessionManager.getInstance().getCurrentUser().setAccountBalance(newBalance);
        renderBalanceLabels(currentBalance, currentUnsettledBalance);
    }

    /**
     * Called in response to an UNSETTLED_UPDATE push from the server.
     * Updates the locked/pending and available balance labels in real-time.
     */
    public void updateUnsettledBalance(long newUnsettled) {
        currentUnsettledBalance = newUnsettled;
        SessionManager.getInstance().getCurrentUser().setUnsettledBalance(newUnsettled);
        renderBalanceLabels(currentBalance, currentUnsettledBalance);
    }

    // --- Private helpers ---

    /**
     * Single source of truth for rendering all three balance labels.
     *
     * Bidder semantics:
     *   accountBalance  = total wallet
     *   unsettled       = locked (amount held for current winning bid)
     *   available       = accountBalance - locked  (free to spend)
     *
     * Seller semantics:
     *   accountBalance  = settled earnings already in wallet
     *   unsettled       = pending (sum of current highest bids across active auctions)
     *   available       = accountBalance  (pending isn't deducted, it's incoming)
     */
    private void renderBalanceLabels(long balance, long unsettled) {
        lblAccountBalance.setText("Balance: " + formatter.format(balance));

        UserRole role = SessionManager.getInstance().getCurrentUser().getRole();
        if (role == UserRole.BIDDER) {
            long available = balance - unsettled;
            lblLockBalance.setText("Locked: "    + formatter.format(unsettled));
            lblAvailableBalance.setText("Available: " + formatter.format(available));
        } else if (role == UserRole.SELLER) {
            // For sellers: pending is money coming in, not deducted from balance.
            lblLockBalance.setText("Pending: "   + formatter.format(unsettled));
            lblAvailableBalance.setText("Balance: " + formatter.format(balance));
        } else {
            // ADMIN or fallback: show raw values without derived arithmetic
            lblLockBalance.setText("Locked: "    + formatter.format(unsettled));
            lblAvailableBalance.setText("Available: " + formatter.format(balance));
        }
    }

    // --- MessageListener implementation ---

    @Override
    public void onMessageReceived(String json) {
        try {
            JsonObject root   = JsonParser.parseString(json).getAsJsonObject();
            String     action = root.has("action") ? root.get("action").getAsString() : "";
            switch (action) {
                case "SESSION_KICKED" ->
                    Platform.runLater(this::handleSessionKicked);
                case "UNSETTLED_UPDATE" -> {
                    long newUnsettled = root.get("data").getAsLong();
                    Platform.runLater(() -> updateUnsettledBalance(newUnsettled));
                }
                case "BALANCE_UPDATE" -> {
                    System.out.println("[DEBUG] đã nhận balance update");
                    long newBalance = root.get("data").getAsLong();
                    Platform.runLater(() -> updateBalance(newBalance));
                }
                case "AUCTION_ENDED" -> {
                    System.out.println("[DEBUG] đã nhận auction ended");
                    JsonObject data     = root.getAsJsonObject("data");
                    String     winnerId = data.has("winnerId") ? data.get("winnerId").getAsString() : "";
                    String     winner   = data.has("winner")   ? data.get("winner").getAsString()   : "N/A";
                    long       price    = data.has("finalPrice") ? data.get("finalPrice").getAsLong() : 0L;
                    String     myId     = user.getId();
                    //Platform.runLater(() -> showAuctionEndedAlert(winnerId, winner, price, myId));
                }
            }
        } catch (Exception e) {
            System.err.println("[MainLayout] onMessageReceived error: " + e.getMessage());
        }
    }

    private void removeListener() {
        socket.removeListener(this);
    }

    // --- Session lifecycle ---

    private void handleSessionKicked() {
        removeListener();
        if (notificationController != null) notificationController.destroy();
        SessionManager.getInstance().logout();

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Phiên đăng nhập hết hạn");
        alert.setHeaderText(null);
        alert.setContentText("Tài khoản của bạn đã đăng nhập ở nơi khác. Bạn đã bị đăng xuất.");
        alert.showAndWait();

        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxml/login-signup.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
            stage.setMaximized(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Notification ---

    private void initNotification() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/notification-popup.fxml"));
            notifPopupRoot = loader.load(); 
            notificationController = loader.getController();
            notificationController.init(this::navigateToAuction);
            
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

    private void navigateToAuction(String auctionId) {
        if (notifPopup != null && notifPopup.isShowing()) notifPopup.hide();
        BidderDisplayDTO dummy = new BidderDisplayDTO();
        dummy.setId(auctionId);
        loadBiddingRoom(dummy);
    }

    // --- Role-based UI visibility ---

    private void applyRole(UserRole role) {
        navWonItems.setVisible(false);      navWonItems.setManaged(false);
        navWatchlist.setVisible(false);     navWatchlist.setManaged(false);
        navNewAuctionRoom.setVisible(false); navNewAuctionRoom.setManaged(false);
        navActiveBids.setVisible(false);    navActiveBids.setManaged(false);
        lblLockBalance.setVisible(false);   lblLockBalance.setManaged(false);
        lblAvailableBalance.setVisible(false); lblAvailableBalance.setManaged(false);

        switch (role) {
            case BIDDER -> {
                lblAccountBalance.setVisible(true);  lblAccountBalance.setManaged(true);
                lblLockBalance.setVisible(true);     lblLockBalance.setManaged(true);
                lblAvailableBalance.setVisible(true); lblAvailableBalance.setManaged(true);
                navWatchlist.setVisible(true);       navWatchlist.setManaged(true);
                navWonItems.setVisible(true);        navWonItems.setManaged(true);
                navActiveBids.setVisible(true);      navActiveBids.setManaged(true);
            }
            case SELLER -> {
                lblAccountBalance.setVisible(true);  lblAccountBalance.setManaged(true);
                lblLockBalance.setVisible(true);     lblLockBalance.setManaged(true);
                navNewAuctionRoom.setVisible(true);  navNewAuctionRoom.setManaged(true);
            }

            case ADMIN -> {
                lblAccountBalance.setVisible(false); lblAccountBalance.setManaged(false);
                return;
            }
        }
    }

    // __CLEANUP__

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

    // __NAVIGATION__

    @FXML
    void handleBell(ActionEvent event) {
        if (notificationController == null || notifPopupRoot == null) return;
        if (notifPopup != null && notifPopup.isShowing()) { notifPopup.hide(); return; }
        if (notifPopup == null) {
            notifPopup = new javafx.stage.Popup();
            notifPopup.setAutoHide(true);    
            notifPopup.setAutoFix(true);     
            notifPopup.getContent().add(notifPopupRoot);
        }
        Node bell = (Node) event.getSource();
        Bounds b = bell.localToScreen(bell.getBoundsInLocal());
        double popupWidth = 340;
        notifPopup.show(bell.getScene().getWindow(), b.getMaxX() - popupWidth, b.getMaxY() + 6);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to log out?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            removeListener();
            if (notificationController != null) notificationController.destroy();
            SessionManager.getInstance().logout();
            try {
                Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxml/login-signup.fxml"));
                Stage stage = (Stage) contentArea.getScene().getWindow();
                stage.getScene().setRoot(loginRoot);
                stage.setMaximized(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void handleNavActiveBids(MouseEvent event) {
        updateActiveStyle(navActiveBids); 
        clearContent();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bidded-auction-list.fxml"));
            Parent biddedAuctionListView = loader.load();
            BiddedAuctionsListController ctrl = loader.getController();
            ctrl.setOnOpenAuction(this::loadBiddingRoom);
            contentArea.getChildren().add(biddedAuctionListView);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi load file bidded-auction-list.fxml");
        }
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

    @FXML
    void handleNavDashboard(MouseEvent event) {
        updateActiveStyle(navDashboard);
        clearContent();
        try {
            contentArea.getChildren().clear();
            String fxmlPath = switch (user.getRole()) {
                case BIDDER -> "/fxml/BidderDashboard.fxml";
                case SELLER -> "/fxml/SellerDashboard.fxml";
                case ADMIN -> "/fxml/AdminDashboard.fxml";
                default -> throw new IllegalStateException("Unexpected role: " + user.getRole());
            };
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent dashboardView = loader.load();

            switch (user.getRole()) {
                case BIDDER -> {
                    BidderDashboardController ctrl = loader.getController();
                    ctrl.setOnOpenBidRoom(this::loadBiddingRoom);
                    currentController = ctrl;
                }

                case SELLER -> {
                    SellerDashboardController ctrl = loader.getController();
                    ctrl.setOnOpenBidRoom(this::loadBiddingRoomAsSeller);
                    currentController = ctrl;
                }

                case ADMIN -> {
                    AdminDashboardController ctrl = loader.getController();
                    ctrl.setOnOpenBidRoom(this::loadBiddingRoomAsAdmin);
                    currentController = ctrl;
                }
            }

            contentArea.getChildren().add(dashboardView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleNavNewAuctionRoom(MouseEvent event) {
        updateActiveStyle(navNewAuctionRoom); 
        clearContent();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-auction.fxml"));
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

    public void loadBiddingRoom(BidderDisplayDTO basicInfo) {
        if (basicInfo == null) { handleNavDashboard(null); return; }
        loadBiddingRoomGeneral(basicInfo.getId(), false);
    }

    public void loadBiddingRoomAsSeller(SellerDisplayDTO basicInfo) {
        if (basicInfo == null) { handleNavDashboard(null); return; }
        loadBiddingRoomGeneral(basicInfo.getId(), true);
    }

    public void loadBiddingRoomAsAdmin(AdminDisplayDTO basicInfo) {
        if (basicInfo == null) { handleNavDashboard(null); return; }
        loadBiddingRoomGeneral(basicInfo.getAuctionId(), true);
    }

    @FXML void handleNavUserInfo(MouseEvent event) { System.out.println("Đã click vào khu vực User Info!"); }

    private void updateActiveStyle(HBox activeItem) {
        HBox[] allNavItems = { navDashboard, navActiveBids, navWatchlist, navWonItems, navNewAuctionRoom };
        for (HBox item : allNavItems) {
            if (item != null) item.getStyleClass().remove("active-nav");
        }
        if (activeItem != null && !activeItem.getStyleClass().contains("active-nav")) {
            activeItem.getStyleClass().add("active-nav");
        }
    }

    @FXML
    void handleNavWatchlist(MouseEvent event) {
        updateActiveStyle(navWatchlist);
        clearContent();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/watchlist.fxml"));
            Parent watchlistView = loader.load();
            WatchlistController ctrl = loader.getController();
            ctrl.setOnOpenAuction(this::loadBiddingRoom);
            contentArea.getChildren().add(watchlistView);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi load file watchlist.fxml");
        }
    }

    @FXML void handleNavWonItems(MouseEvent event) {}
    @FXML void handleSearching(MouseEvent event) {}

    @FXML
    void toggleSidebar(ActionEvent event) {
        Timeline timeline = new Timeline();
        Duration duration = Duration.millis(150);
        double targetWidth = isSidebarExpanded ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;

        KeyFrame kf = new KeyFrame(duration,
                new KeyValue(sidebar.prefWidthProperty(), targetWidth),
                new KeyValue(sidebar.minWidthProperty(),  targetWidth),
                new KeyValue(sidebar.maxWidthProperty(),  targetWidth));
        timeline.getKeyFrames().add(kf);

        Label[] navLabels = {
            lblSidebarTitleAB, lblSidebarTitleDB, lblSidebarTitleNAR, lblSidebarTitleW, lblSidebarTitleWI
        };

        if (isSidebarExpanded) {
            for (Label lbl : navLabels) { lbl.setVisible(false); lbl.setManaged(false); }
            lblOverview.setText("");
            lblAuction.setText("");
            btnLogOut.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        } else {
            timeline.setOnFinished(e -> {
                for (Label lbl : navLabels) { lbl.setVisible(true); lbl.setManaged(true); }
                lblOverview.setText("OVERVIEW");
                lblAuction.setText("AUCTION");
                btnLogOut.setContentDisplay(ContentDisplay.LEFT);
            });
        }

        timeline.play();
        isSidebarExpanded = !isSidebarExpanded;
    }

    public void loadBiddingRoomGeneral(String auctionId, boolean isViewOnly) {
        if (loading != null && loadingController != null) {
                loading.setVisible(true);
                loadingController.playAnimation();
        } else { System.out.println("Loading overlay not ready"); return; }
        new Thread(() -> {
            GetAuctionDetailsRequest req = new GetAuctionDetailsRequest(auctionId);
            String jsonResponse = socket.sendAndReceive(
                    JsonUtils.toJson(ClientMessage.request("GET_AUCTION_DETAILS", req)));

            AuctionDTO fullAuctionData = null;
            if (jsonResponse != null && !jsonResponse.isEmpty()) {
                ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);
                if ("GET_AUCTION_DETAILS_RESPONSE".equals(serverMsg.getAction())) {
                    String raw = JsonUtils.toJson(serverMsg.getData());
                    Type type = new TypeToken<ApiResponse<AuctionDTO>>() {}.getType();
                    ApiResponse<AuctionDTO> resp = JsonUtils.fromJsonGeneric(raw, type);
                    if (resp != null && resp.isSuccess()) fullAuctionData = resp.getData();
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
                        ctrl.setAuction(finalData);
                        ctrl.setOnSuccessCallback(() -> handleNavDashboard(null));
                        if (isViewOnly) ctrl.enableSellerViewMode();
                        currentController = ctrl;
                        contentArea.getChildren().add(view);
                    } catch (IOException e) { e.printStackTrace(); }
                } else { System.out.println("Cannot load auction data for auction id: " + auctionId); }
                if (loading != null) loading.setVisible(false);
            });
        }).start();
    }
}

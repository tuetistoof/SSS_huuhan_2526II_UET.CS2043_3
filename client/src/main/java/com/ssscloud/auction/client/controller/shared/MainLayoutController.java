package com.ssscloud.auction.client.controller.shared;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.DecimalFormat;
import java.util.Optional;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.request.GetAuctionDetailsRequest;
import com.ssscloud.auction.common.payload.response.DTO.AdminDisplayDTO;
import com.ssscloud.auction.common.payload.response.DTO.AuctionDTO;
import com.ssscloud.auction.common.payload.response.DTO.BidderDisplayDTO;
import com.ssscloud.auction.common.payload.response.DTO.SellerDisplayDTO;
import com.ssscloud.auction.common.payload.response.DTO.UserDTO;
import com.ssscloud.auction.common.payload.response.request.ApiResponse;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.controller.seller.SellerDashboardController;
import com.ssscloud.auction.client.controller.admin.AdminDashboardController;
import com.ssscloud.auction.client.controller.bidder.BiddedAuctionsListController;
import com.ssscloud.auction.client.controller.bidder.BidderDashboardController;
import com.ssscloud.auction.client.controller.bidder.BiddingRoomController;
import com.ssscloud.auction.client.controller.bidder.DepositCardController;
import com.ssscloud.auction.client.controller.bidder.WatchlistController;
import com.ssscloud.auction.client.controller.bidder.ItemsWonController;
import com.ssscloud.auction.client.controller.seller.CreateAuctionController;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.networking.MessageListener;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.client.util.ThemeManager;
import com.ssscloud.auction.client.util.ViewLoader;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

    // Icon ImageViews — để swap light/dark
    @FXML private javafx.scene.image.ImageView iconMenuBurger;
    @FXML private javafx.scene.image.ImageView iconNotification;
    @FXML private javafx.scene.image.ImageView iconHome;
    @FXML private javafx.scene.image.ImageView iconActiveBids;
    @FXML private javafx.scene.image.ImageView iconWatchlist;
    @FXML private javafx.scene.image.ImageView iconWonItems;
    @FXML private javafx.scene.image.ImageView iconNewAuction;
    @FXML private javafx.scene.image.ImageView iconLogout;

    @FXML private Label lblSidebarTitleAB;
    @FXML private Label lblSidebarTitleDB;
    @FXML private Label lblSidebarTitleNAR;
    @FXML private Label lblSidebarTitleW;
    @FXML private Label lblSidebarTitleWI;
    @FXML private Label lblAvailableBalance;
    @FXML private Label lblUsername;
    @FXML private Label lblOverview;
    @FXML private Label lblAuction;

    private Label[] navLabels; 
    private HBox[] navItems;

    @FXML private HBox navActiveBids;
    @FXML private HBox navDashboard;
    @FXML private HBox navNewAuctionRoom;
    @FXML private HBox navWatchlist;
    @FXML private HBox navWonItems;
    @FXML private Button btnLogOut;

    @FXML private Button btnToggleTheme;
    @FXML private Button btnAvailableBalance;

    @FXML private Label lblBellBadge;
    @FXML private Button btnBell;
    @FXML private Button btnDeposit;

    private NotificationController notificationController; 
    private Popup notifPopup;
    private Parent notifPopupRoot; // root đã load sẵn từ fxml, tái sử dụng cho mọi lần show/hide
        
    private BalancePopupController balancePopupController;
    private Popup balancePopup;
    private Parent balancePopupRoot;

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
    private final SocketDispatcher    dispatcher = SocketDispatcher.getInstance();

    private Runnable onSuccessCallback;
    
    DecimalFormat formatter = new DecimalFormat("#,###");
 
    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    public void initialize() {
        currentBalance          = user.getAccountBalance();
        currentUnsettledBalance = user.getUnsettledBalance();

        navLabels = new Label[]{
            lblSidebarTitleAB, lblSidebarTitleDB, lblSidebarTitleNAR, lblSidebarTitleW, lblSidebarTitleWI
        };
        
        navItems = new HBox[]{
            navDashboard, navActiveBids, navWatchlist, navWonItems, navNewAuctionRoom
        };

        lblUsername.setText(user.getUsername());
        renderBalanceLabels(currentBalance, currentUnsettledBalance);
        applyRole(user.getRole());
        initNotification();
        initBalancePopup();
        handleNavDashboard(null);
        socket.addListener(this);

        Platform.runLater(() -> {
            ThemeManager.Theme saved = ThemeManager.getSavedTheme();
            ThemeManager.apply(btnToggleTheme.getScene(), saved);
            btnToggleTheme.setText(saved == ThemeManager.Theme.DARK ? "Light" : "Dark");
            updateIcons(saved == ThemeManager.Theme.DARK);
        });
    }

    private void updateIcons(boolean dark) {
        String suffix = dark ? "-dark" : "";
        setIcon(iconMenuBurger,  "/images/menu-burger"          + suffix + ".png");
        setIcon(iconHome,        "/images/home"                 + suffix + ".png");
        setIcon(iconActiveBids,  "/images/calendar-gavel-legal" + suffix + ".png");
        setIcon(iconWatchlist,   "/images/clock"                + suffix + ".png");
        setIcon(iconWonItems,    "/images/trophy"               + suffix + ".png");
        setIcon(iconNewAuction,  "/images/add"                  + suffix + ".png");
    }

    private void setIcon(ImageView iv, String resourcePath) {
        if (iv == null) return;
        var url = getClass().getResource(resourcePath);
        if (url != null) iv.setImage(new Image(url.toExternalForm()));
    }

    // --- Balance update API (called externally by DepositCardController, etc.) ---


    public void updateBalance(long newBalance) {
        currentBalance = newBalance;
        SessionManager.getInstance().getCurrentUser().setAccountBalance(newBalance);
        renderBalanceLabels(currentBalance, currentUnsettledBalance);
    }

    public void updateUnsettledBalance(long newUnsettled) {
        currentUnsettledBalance = newUnsettled;
        SessionManager.getInstance().getCurrentUser().setUnsettledBalance(newUnsettled);
        renderBalanceLabels(currentBalance, currentUnsettledBalance);
    }

    private void renderBalanceLabels(long balance, long unsettled) {
        UserRole role = SessionManager.getInstance().getCurrentUser().getRole();
        long available = (role == UserRole.SELLER) ? balance : balance - unsettled;
        if (lblAvailableBalance != null) {
            lblAvailableBalance.setText("Available: " + formatter.format(available) + " ₫");
        }
        if (balancePopupController != null) {
            balancePopupController.update(role, balance, unsettled);
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
                    long newBalance = root.get("data").getAsLong();
                    Platform.runLater(() -> updateBalance(newBalance));
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
        alert.setTitle("Login session expired");
        alert.setHeaderText(null);
        alert.setContentText("Your account are logged in from another location! Logging out.");
        alert.showAndWait(); //blocking intentionally, not thread block

        Parent loginRoot = ViewLoader.load("login-signup.fxml").root();
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.getScene().setRoot(loginRoot);
        stage.setMaximized(false);
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
            System.err.println("[MainlayoutController] can extract notification-popup.fxml: " + e.getMessage());
        }
    }

    private void initBalancePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/balance-popup.fxml"));
            balancePopupRoot = loader.load();
            balancePopupController = loader.getController();
            balancePopupController.update(
                user.getRole(), currentBalance, currentUnsettledBalance);
        } catch (IOException e) {
            System.err.println("[MainLayoutController] cannot load balance-popup.fxml: " + e.getMessage());
        }
    }

    private void navigateToAuction(String auctionId) {
        if (notifPopup != null && notifPopup.isShowing()) notifPopup.hide();
        // Từ notification không có tab nguồn rõ ràng → back về Dashboard
        loadBiddingRoomGeneral(auctionId, false, () -> handleNavDashboard(null));
    }

    // --- Role-based UI visibility ---

    private void applyRole(UserRole role) {
        btnDeposit.setVisible(false);       btnDeposit.setManaged(false);
        navWonItems.setVisible(false);      navWonItems.setManaged(false);
        navWatchlist.setVisible(false);     navWatchlist.setManaged(false);
        navNewAuctionRoom.setVisible(false); navNewAuctionRoom.setManaged(false);
        navActiveBids.setVisible(false);    navActiveBids.setManaged(false);
        if (btnAvailableBalance != null) { btnAvailableBalance.setVisible(false); btnAvailableBalance.setManaged(false); }

        switch (role) {
            case BIDDER -> {
                if (btnAvailableBalance != null) { btnAvailableBalance.setVisible(true); btnAvailableBalance.setManaged(true); }
                btnDeposit.setVisible(true);       btnDeposit.setManaged(true);
                navWatchlist.setVisible(true);       navWatchlist.setManaged(true);
                navWonItems.setVisible(true);        navWonItems.setManaged(true);
                navActiveBids.setVisible(true);      navActiveBids.setManaged(true);
            }
            case SELLER -> {
                if (btnAvailableBalance != null) { btnAvailableBalance.setVisible(true); btnAvailableBalance.setManaged(true); }
                navNewAuctionRoom.setVisible(true);  navNewAuctionRoom.setManaged(true);
            }
            case ADMIN -> {
                // Admin has no balance display
            }
        }
    }

    // __CLEANUP__

    private void cleanupCurrentController() {
        if (currentController == null) return;
        else if (currentController instanceof BiddingRoomController        c) c.cleanup();
        else if (currentController instanceof BiddedAuctionsListController c) c.cleanup();
        else if (currentController instanceof WatchlistController          c) c.cleanup();
        else if (currentController instanceof ItemsWonController           c) c.cleanup();
        else if (currentController instanceof BidderDashboardController    c) c.cleanup();
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
    void handleAvailableBalance(ActionEvent event) {
        if (balancePopupController == null || balancePopupRoot == null) return;

        balancePopupController.update(user.getRole(), currentBalance, currentUnsettledBalance);

        if (balancePopup != null && balancePopup.isShowing()) { balancePopup.hide(); return; }
        if (balancePopup == null) {
            balancePopup = new Popup();
            balancePopup.setAutoHide(true);
            balancePopup.setAutoFix(true);
            balancePopup.getContent().add(balancePopupRoot);
        }
        Node src = (Node) event.getSource();
        Bounds b = src.localToScreen(src.getBoundsInLocal());
        double popupWidth = 280;
        balancePopup.show(src.getScene().getWindow(), b.getMaxX() - popupWidth, b.getMaxY() + 6);
    }

    @FXML
    private void handleToggleTheme() {
        ThemeManager.toggle(btnToggleTheme.getScene());
        boolean dark = ThemeManager.isDark(btnToggleTheme.getScene());
        btnToggleTheme.setText(dark ? "Light" : "Dark");
        updateIcons(dark);
        Platform.runLater(() -> {
            if (!isSidebarExpanded) {
                // Re-apply toàn bộ trạng thái collapsed
                for (Label lbl : navLabels) {
                    if (lbl != null) { lbl.setVisible(false); lbl.setManaged(false); }
                }
                if (lblOverview != null) { lblOverview.setVisible(false); lblOverview.setManaged(false); }
                if (lblAuction  != null) { lblAuction.setVisible(false);  lblAuction.setManaged(false); }

                for (HBox item : navItems) {
                    if (item != null) {
                        item.setAlignment(javafx.geometry.Pos.CENTER);
                        item.setPadding(new javafx.geometry.Insets(12, 0, 12, 0));
                    }
                }
                btnLogOut.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnLogOut.setAlignment(javafx.geometry.Pos.CENTER);
                btnLogOut.setPadding(new javafx.geometry.Insets(12, 0, 12, 0));

                sidebar.setPrefWidth(SIDEBAR_COLLAPSED_WIDTH);
                sidebar.setMinWidth(SIDEBAR_COLLAPSED_WIDTH);
                sidebar.setMaxWidth(SIDEBAR_COLLAPSED_WIDTH);
            } else {
                // Expanded — re-assert width phòng trường hợp CSS reset
                sidebar.setPrefWidth(SIDEBAR_EXPANDED_WIDTH);
                sidebar.setMinWidth(SIDEBAR_EXPANDED_WIDTH);
                sidebar.setMaxWidth(SIDEBAR_EXPANDED_WIDTH);
            }
        });
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
        ViewLoader.LoadResult<BiddedAuctionsListController> r = ViewLoader.load("bidded-auction-list.fxml");
        r.controller().setOnOpenAuction(dto ->
            loadBiddingRoomGeneral(dto.getId(), false, () -> handleNavActiveBids(null)));
        contentArea.getChildren().add(r.root());
        currentController = r.controller();
    }

    @FXML
    void handleDeposit(ActionEvent event) {
        ViewLoader.LoadResult<DepositCardController> r = ViewLoader.load("deposit-card.fxml");
        r.controller().setMainLayoutController(this);
        Stage depositStage = new Stage();
        depositStage.setTitle("Deposit");
        depositStage.setScene(new Scene(r.root()));
        depositStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        depositStage.show();
    }

    @FXML
    void handleNavDashboard(MouseEvent event) {
        updateActiveStyle(navDashboard);
        clearContent();
        try {
            contentArea.getChildren().clear();
            String fxmlPath = switch (user.getRole()) {
                case BIDDER -> "/fxml/bidder-dashboard.fxml";
                case SELLER -> "/fxml/seller-dashboard.fxml";
                case ADMIN -> "/fxml/admin-dashboard.fxml";
                default -> throw new IllegalStateException("Unexpected role: " + user.getRole());
            };
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent dashboardView = loader.load();

            switch (user.getRole()) {
                case BIDDER -> {
                    BidderDashboardController ctrl = loader.getController();
                    ctrl.setOnOpenBidRoom(this::loadBiddingRoomAsBidder);
                    currentController = ctrl;
                }

                case SELLER -> {
                    SellerDashboardController ctrl = loader.getController();
                    ctrl.setOnOpenBidRoom(this::loadBiddingRoomAsSeller);
                    ctrl.setOnCreateAuction(() -> handleNavNewAuctionRoom(null));
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
        ViewLoader.LoadResult<CreateAuctionController> r = ViewLoader.load("create-auction.fxml");
        r.controller().setOnSuccessCallback(newAuction -> {
            updateActiveStyle(null);
            clearContent();
            ViewLoader.LoadResult<BiddingRoomController> room = ViewLoader.load("bidding-room.fxml");
            room.controller().setAuction(newAuction);
            room.controller().enableSellerViewMode();
            room.controller().setOnSuccessCallback(() -> handleNavDashboard(null));
    
            currentController = room.controller();
            contentArea.getChildren().add(room.root());
        });


        r.controller().setOnCancelCallback(() -> {
            handleNavDashboard(null);
        });

        contentArea.getChildren().add(r.root());
    }

    public void loadBiddingRoomAsBidder(BidderDisplayDTO basicInfo) {
        if (basicInfo == null) { handleNavDashboard(null); return; }
        loadBiddingRoomGeneral(basicInfo.getId(), false, () -> handleNavDashboard(null));
    }

    public void loadBiddingRoomAsSeller(SellerDisplayDTO basicInfo) {
        if (basicInfo == null) { handleNavDashboard(null); return; }
        loadBiddingRoomGeneral(basicInfo.getId(), true, () -> handleNavDashboard(null));
    }

    public void loadBiddingRoomAsAdmin(AdminDisplayDTO basicInfo) {
        if (basicInfo == null) { handleNavDashboard(null); return; }
        loadBiddingRoomGeneral(basicInfo.getAuctionId(), true, () -> handleNavDashboard(null));
    }

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
        ViewLoader.LoadResult<WatchlistController> r = ViewLoader.load("watchlist.fxml");
        r.controller().setOnOpenAuction(dto ->
            loadBiddingRoomGeneral(dto.getId(), false, () -> handleNavWatchlist(null)));
        contentArea.getChildren().add(r.root());
        currentController = r.controller();
    }

    @FXML void handleNavWonItems(MouseEvent event) {
        updateActiveStyle(navWonItems);
        clearContent();
        ViewLoader.LoadResult<ItemsWonController> r = ViewLoader.load("won-items.fxml");
        r.controller().setOnOpenAuction(dto ->
            loadBiddingRoomGeneral(dto.getId(), false, () -> handleNavWonItems(null)));
        contentArea.getChildren().add(r.root());
        currentController = r.controller();
    }

    @FXML
    void toggleSidebar(ActionEvent event) {
        Timeline timeline = new Timeline();
        Duration duration = Duration.millis(150);
        double targetWidth = isSidebarExpanded ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;

        timeline.getKeyFrames().add(new KeyFrame(duration,
                new KeyValue(sidebar.prefWidthProperty(), targetWidth),
                new KeyValue(sidebar.minWidthProperty(),  targetWidth),
                new KeyValue(sidebar.maxWidthProperty(),  targetWidth)));

        if (isSidebarExpanded) {
            for (Label lbl : navLabels) { 
                if (lbl != null) { lbl.setVisible(false); lbl.setManaged(false); }
            }

            if (lblOverview != null) { lblOverview.setVisible(false); lblOverview.setManaged(false); }
            if (lblAuction != null)  { lblAuction.setVisible(false); lblAuction.setManaged(false); }
            
            for (HBox item : navItems) {
                if (item != null) {
                    item.setAlignment(javafx.geometry.Pos.CENTER);
                    item.setPadding(new javafx.geometry.Insets(12, 0, 12, 0)); 
                }
            }
            
            btnLogOut.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btnLogOut.setAlignment(javafx.geometry.Pos.CENTER);
            btnLogOut.setPadding(new javafx.geometry.Insets(12, 0, 12, 0));
            
        } else {
            timeline.setOnFinished(e -> {
                for (Label lbl : navLabels) { 
                    if (lbl != null) { lbl.setVisible(true); lbl.setManaged(true); }
                }

                if (lblOverview != null) { 
                    lblOverview.setVisible(true); lblOverview.setManaged(true); 
                    lblOverview.setText("OVERVIEW"); 
                }
                if (lblAuction != null) { 
                    lblAuction.setVisible(true); lblAuction.setManaged(true); 
                    lblAuction.setText("AUCTION"); 
                }

                for (HBox item : navItems) {
                    if (item != null) {
                        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        item.setPadding(new javafx.geometry.Insets(12, 20, 12, 20)); 
                    }
                }
                
                btnLogOut.setContentDisplay(ContentDisplay.LEFT);
                btnLogOut.setAlignment(javafx.geometry.Pos.CENTER);
                btnLogOut.setPadding(new javafx.geometry.Insets(12, 20, 12, 20));
            });
        }

        timeline.play();
        isSidebarExpanded = !isSidebarExpanded;
    }

    public void loadBiddingRoomGeneral(String auctionId, boolean isViewOnly, Runnable backAction) {
        if (loading != null && loadingController != null) {
                loading.setVisible(true);
                loadingController.playAnimation();
        } else { 
            System.out.println("Loading overlay not ready"); 
            return; }
        loading.setVisible(true);
        loadingController.playAnimation();
 
        GetAuctionDetailsRequest req  = new GetAuctionDetailsRequest(auctionId);
        String                   json = JsonUtils.toJson(ClientMessage.request("GET_AUCTION_DETAILS", req));
 
        dispatcher.request(json, raw -> {
            loading.setVisible(false);
            loadingController.stopAnimation();
 
            AuctionDTO auction = ServerResponse.unwrap(raw, "GET_AUCTION_DETAILS_RESPONSE", AuctionDTO.class);
            if (auction != null) {
                updateActiveStyle(null);
                clearContent();
                ViewLoader.LoadResult<BiddingRoomController> r = ViewLoader.load("bidding-room.fxml");
                r.controller().setAuction(auction);
                r.controller().setOnSuccessCallback(backAction != null ? backAction : () -> handleNavDashboard(null));
                if (isViewOnly) r.controller().enableSellerViewMode();
                currentController = r.controller();
                contentArea.getChildren().add(r.root());
            } else {
                System.out.println("Cannot load auction data for: " + auctionId);
            }
        }, () -> {
            loading.setVisible(false);
            loadingController.stopAnimation();
        });
    }
}
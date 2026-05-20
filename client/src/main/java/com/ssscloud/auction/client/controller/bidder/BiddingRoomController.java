package com.ssscloud.auction.client.controller.bidder;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AutoBidStatusDTO;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.util.JsonUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.networking.MessageListener;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.AuctionCountdownTimer;
import com.ssscloud.auction.client.util.PriceChartManager;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.client.util.SessionManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class BiddingRoomController implements MessageListener {

    // ------------------- FXML fields -------------------

    @FXML private Button btnAutoToggle;
    @FXML private Button btnBack;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnTabAuto;
    @FXML private Button btnTabManual;
    @FXML private Button btnBackImage;
    @FXML private Button btnFrontImage;
    @FXML private Button btnFollow;

    @FXML private NumberAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;
    @FXML private VBox formAuto;
    @FXML private VBox formManual;
    @FXML private VBox bidForm;
    @FXML private StackPane containerImage;
    @FXML private ImageView imgBiddingRoom;

    @FXML private Label infoAntiSnipe;
    @FXML private Label infoEndTime;
    @FXML private Label infoItemType;
    @FXML private Label infoMinIncrement;
    @FXML private Label infoName;
    @FXML private Label infoSeller;
    @FXML private Label infoStartPrice;
    @FXML private Label infoStartTime;
    @FXML private Label infoDescription;

    @FXML private Label lblAuctionName;
    @FXML private Label lblBidCount;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeaderName;
    @FXML private Label lblMinHint;
    @FXML private Label lblMinIncrement;
    @FXML private Label lblStartPrice;
    @FXML private Label lblStatusBadge;
    @FXML private Label lblTimer;
    @FXML private Label lblOutbid;

    @FXML private ListView<BidDTO> listViewBidHistory;
    @FXML private VBox panelChart;
    @FXML private VBox panelHistory;
    @FXML private VBox panelInfo;
    @FXML private LineChart<Number, Number> priceLineChart;

    @FXML private Button tabBtnChart;
    @FXML private Button tabBtnHistory;
    @FXML private Button tabBtnInfo;
    @FXML private TextField txtAutoIncrement;
    @FXML private TextField txtManualBid;
    @FXML private TextField txtMaxBid;

    // ------------------- State fields -------------------

    private AuctionCountdownTimer timer;
    private PriceChartManager chartManager;

    private boolean isFollowing   = false;
    private boolean isAutoBidding = false;
    private long    autoBidMaxBid = 0;

    private long maxBid = 0;
    private long increment = 0;

    private AuctionDTO   currentAuction;
    private List<String> itemUrls;
    private int          currentImageIndex = 0;

    private String currentUserName = SessionManager.getInstance().getCurrentUser() != null
            ? SessionManager.getInstance().getCurrentUser().getUsername() : null;

    private Runnable onSuccessCallback;
    public void setOnSuccessCallback(Runnable cb) { this.onSuccessCallback = cb; }

    private final ObservableList<BidDTO> bidHistory = FXCollections.observableArrayList();
    private final AuctionClientSocket socket     = AuctionClientSocket.getInstance();
    private final SocketDispatcher    dispatcher = SocketDispatcher.getInstance();

    // ------------------- Lifecycle -------------------

    public void initialize() {
        socket.addListener(this);
        timer        = new AuctionCountdownTimer(lblTimer);
        chartManager = new PriceChartManager(priceLineChart, chartXAxis, chartYAxis);
        setupBidHistoryList();
    }

    private void setupBidHistoryList() {
        listViewBidHistory.setItems(bidHistory);
        listViewBidHistory.setPlaceholder(new Label("There is no bid transaction"));
        listViewBidHistory.setCellFactory(lv -> new BidHistoryCell());
    }

    private static class BidHistoryCell extends ListCell<BidDTO> {
        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
        private final HBox   root        = new HBox(10);
        private final Label  badgeLabel  = new Label();
        private final VBox   nameTimeBox = new VBox(2);
        private final Label  nameLabel   = new Label();
        private final Label  timeLabel   = new Label();
        private final Region spacer      = new Region();
        private final Label  amountLabel = new Label();

        BidHistoryCell() {
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(4, 0, 4, 0));
            nameTimeBox.getChildren().addAll(nameLabel, timeLabel);
            nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #222222;");
            timeLabel.getStyleClass().add("br-bid-time");
            amountLabel.getStyleClass().add("br-bid-amount");
            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(badgeLabel, nameTimeBox, spacer, amountLabel);
        }

        @Override
        protected void updateItem(BidDTO bid, boolean empty) {
            super.updateItem(bid, empty);
            if (empty || bid == null) { setGraphic(null); return; }
            if ("AUTO".equalsIgnoreCase(bid.getBidType())) {
                badgeLabel.setText("Auto");
                badgeLabel.getStyleClass().setAll("br-bid-type-auto");
            } else {
                badgeLabel.setText("Manual");
                badgeLabel.getStyleClass().setAll("br-bid-type-manual");
            }
            nameLabel.setText(bid.getBidderUsername() != null ? bid.getBidderUsername() : "-");
            timeLabel.setText(bid.getBidTime() != null ? bid.getBidTime().format(TIME_FMT) : "");
            amountLabel.setText(String.format("%,d ₫", bid.getBidAmount()));
            setGraphic(root);
        }
    }

    // ------------------- setAuction (entry point) -------------------
    
    public void setAuction(AuctionDTO auction) {
        this.currentAuction = auction;
        itemUrls = auction.getItemDTO().getImageUrls();

        boolean isFinished  = auction.getStatus() == AuctionStatus.FINISHED;
        boolean isCancelled = auction.getStatus() == AuctionStatus.CANCELED;

        Platform.runLater(() -> {
            populateUI();
            setUpItemImage(itemUrls);
            if (btnFollow != null) { btnFollow.setDisable(true); btnFollow.setText("..."); }
            if (isFinished || isCancelled) disableAllBidUI(isCancelled);
        });

        String subJson = JsonUtils.toJson(ClientMessage.request("SUBSCRIBE_AUCTION", auction.getId()));
        dispatcher.request(subJson, subRaw -> {
            loadBidHistoryAsync();
            setupBidStatusAsync();
            if (!isFinished && !isCancelled) btnPlaceBid.setDisable(false);
        });

        checkFollowStatus();
        if (!isFinished && !isCancelled) timer.start(auction.getEndTime());
    }

    private void disableAllBidUI(boolean isCancelled) {
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText(isCancelled ? "Canceled" : "Finished");
        btnAutoToggle.setDisable(true);
        txtManualBid.setDisable(true);
        if (txtMaxBid != null)        txtMaxBid.setDisable(true);
        if (txtAutoIncrement != null) txtAutoIncrement.setDisable(true);
        if (lblTimer != null) lblTimer.setText(isCancelled ? "Canceled" : "Finished");
    }

    private void populateUI() {
        if (currentAuction == null) return;
        if (lblAuctionName != null)
            lblAuctionName.setText(currentAuction.getName() != null ? currentAuction.getName() : "-");
        if (lblCurrentPrice != null)
            lblCurrentPrice.setText(String.format("%,d ₫", currentAuction.getCurrentPrice()));
        if (lblLeaderName != null) {
            String leader = currentAuction.getHighestBidderName();
            lblLeaderName.setText("Leading: " + (leader != null ? leader : "-"));
        }
        btnPlaceBid.setDisable(true);
        if (lblMinIncrement != null)
            lblMinIncrement.setText(currentAuction.getMinIncrement() > 0
                    ? String.format("%,d ₫", currentAuction.getMinIncrement()) : "-");
        if (lblStartPrice != null)
            lblStartPrice.setText(String.format("%,d ₫", currentAuction.getStartPrice()));
        if (lblMinHint != null) {
            if (currentAuction.getBidCount() == 0) {
                lblMinHint.setText("At least: " + String.format("%,d ₫", currentAuction.getStartPrice())
                        + " (start price)");
            } else {
                lblMinHint.setText("At least: " + String.format("%,d ₫",
                        currentAuction.getCurrentPrice() + currentAuction.getMinIncrement()));
            }
        }
        if (lblStatusBadge != null && currentAuction.getStatus() != null) {
            switch (currentAuction.getStatus()) {
                case RUNNING  -> { lblStatusBadge.setText("Running");  lblStatusBadge.getStyleClass().setAll("br-badge-running"); }
                case FINISHED -> { lblStatusBadge.setText("Finished"); lblStatusBadge.getStyleClass().setAll("br-badge-ended"); }
                case CANCELED -> { lblStatusBadge.setText("Canceled"); lblStatusBadge.getStyleClass().setAll("br-badge-ended"); }
                default       -> lblStatusBadge.setText(currentAuction.getStatus().toString());
            }
        }
    }

    // ------------------- Data loading -------------------

    private void checkFollowStatus() {
        if (currentAuction == null) return;
        String json = JsonUtils.toJson(ClientMessage.request("CHECK_FOLLOWING", currentAuction.getId()));
        dispatcher.request(json, raw -> {
            Boolean following = ServerResponse.unwrap(raw, null, Boolean.class);
            if (following != null) isFollowing = following;
            updateFollowButton();
        }, this::updateFollowButton);
    }

    private void loadBidHistoryAsync() {
        if (currentAuction == null) return;
        String json = JsonUtils.toJson(ClientMessage.request("GET_BID_HISTORY", currentAuction.getId()));
        dispatcher.request(json, raw -> {
            List<BidDTO> list = ServerResponse.unwrapDirectList(raw, "GET_BID_HISTORY_RESPONSE", BidDTO.class);
            if (list == null) return;
            Collections.reverse(list);
            mergeBidHistory(list);
            BidDTO latest = findLatestBid();
            if (latest != null && latest.getBidAmount() >= currentAuction.getCurrentPrice()) {
                currentAuction.setCurrentPrice(latest.getBidAmount());
                currentAuction.setHighestBidderName(latest.getBidderUsername());
                lblCurrentPrice.setText(String.format("%,d ₫", latest.getBidAmount()));
                lblLeaderName.setText("Leading: " + latest.getBidderUsername());
                if (lblMinHint != null)
                    lblMinHint.setText("Min: " + String.format("%,d ₫",
                            latest.getBidAmount() + currentAuction.getMinIncrement()));
            }
            lblBidCount.setText(String.valueOf(bidHistory.size()));
        });
    }

    /**
     * Lấy trạng thái auto-bid từ server - non-blocking, callback trên FX thread.
     * Server: GET_AUTOBID_STATUS → ApiResponse<Boolean>
     * Dùng ServerResponse.unwrap() để bóc 2 lớp ClientMessage + ApiResponse.
     */
    private void setupBidStatusAsync() {
        if (currentAuction == null) return;
        String json = JsonUtils.toJson(ClientMessage.request("GET_AUTOBID_STATUS", currentAuction.getId()));
        dispatcher.request(json, raw -> {
            // ServerResponse.unwrap bóc ClientMessage → ApiResponse → Boolean
            AutoBidStatusDTO status = ServerResponse.unwrap(raw, "GET_AUTOBID_STATUS_RESPONSE", AutoBidStatusDTO.class);
            if (status != null && status.isActive()) {
                maxBid = status.getMaxBid();
                increment = status.getIncrement();

                boolean isActive = status.isActive();
                System.out.println("[setupBidStatus] isAutoBidding=" + isActive);
                applyAutoBidState(isActive);
            } else {
                applyAutoBidState(false);
            }
        }, () -> {
            // Timeout/lỗi - default false, không treo UI
            System.err.println("[setupBidStatus] timeout/error - defaulting false");
            applyAutoBidState(false);
        });
    }

    /**
     * Áp dụng trạng thái auto-bid lên toàn bộ UI.
     * Gọi từ: setupBidStatusAsync, handleToggleAutoBid, handleAutoBidStopped.
     * Luôn chạy trên FX thread.
     */
    private void applyAutoBidState(boolean active) {
        isAutoBidding = active;
        if (active) {
            formAuto.setVisible(true);    formAuto.setManaged(true);
            formManual.setVisible(false); formManual.setManaged(false);
            if (maxBid > 0)    txtMaxBid.setText(String.valueOf(maxBid));
            if (increment > 0) txtAutoIncrement.setText(String.valueOf(increment));
            btnTabAuto.getStyleClass().setAll("br-tab-active");
            btnTabManual.getStyleClass().setAll("br-tab");
            txtMaxBid.setDisable(true);
            txtAutoIncrement.setDisable(true);
            btnAutoToggle.setText("Cancel Auto Bid");
            btnAutoToggle.setDisable(false);
            btnAutoToggle.getStyleClass().remove("br-btn-secondary");
            btnAutoToggle.getStyleClass().add("br-btn-auto-active");
        } else {
            formManual.setVisible(true);  formManual.setManaged(true);
            formAuto.setVisible(false);   formAuto.setManaged(false);
            btnTabManual.getStyleClass().setAll("br-tab-active");
            btnTabAuto.getStyleClass().setAll("br-tab");
            txtMaxBid.setDisable(false);
            txtAutoIncrement.setDisable(false);
            btnAutoToggle.getStyleClass().remove("br-btn-auto-active");
            btnAutoToggle.getStyleClass().add("br-btn-secondary");
            resetAutoBidButton();
        }
    }

    // ------------------- @FXML handlers -------------------

    @FXML
    void handleFollowRoom(ActionEvent event) {
        if (currentAuction == null) return;
        String action = isFollowing ? "UNFOLLOW_AUCTION" : "FOLLOW_AUCTION";
        if (btnFollow != null) { btnFollow.setDisable(true); btnFollow.setText("..."); }
        String json = JsonUtils.toJson(ClientMessage.request(action, currentAuction.getId()));
        dispatcher.request(json, raw -> {
            if (ServerResponse.isSuccess(raw)) isFollowing = !isFollowing;
            updateFollowButton();
        }, this::updateFollowButton);
    }

    private void updateFollowButton() {
        if (btnFollow == null) return;
        btnFollow.setDisable(false);
        if (isFollowing) {
            btnFollow.setText("Following");
            btnFollow.getStyleClass().setAll("br-btn-follow-active");
        } else {
            btnFollow.setText("Follow");
            btnFollow.getStyleClass().setAll("br-btn-follow");
        }
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        String amountText = txtManualBid.getText().trim();
        if (amountText.isEmpty()) { showError("Please enter your bid amount"); return; }
        long amount;
        try { amount = Long.parseLong(amountText); }
        catch (NumberFormatException e) { showError("Invalid bid amount"); return; }
        if (amount <= 0) { showError("Bid amount needs to be larger than 0"); return; }

        if (currentAuction.getBidCount() == 0) {
            if (amount < currentAuction.getStartPrice()) {
                showError("Bid amount cannot be lower than start price: "
                        + String.format("%,d ₫", currentAuction.getStartPrice())); return;
            }
        } else {
            if (amount <= currentAuction.getCurrentPrice()) {
                showError("Bid amount cannot be lower than current price"); return;
            }
            if (amount < currentAuction.getCurrentPrice() + currentAuction.getMinIncrement()) {
                showError("Bid amount needs to pass min increment ("
                        + String.format("%,d ₫", currentAuction.getMinIncrement()) + ")"); return;
            }
        }
        txtManualBid.clear();
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("Progressing...");
        socket.send(JsonUtils.toJson(ClientMessage.request("PLACE_BID",
                new PlaceBidRequest(currentAuction.getId(), amount))));
    }

    @FXML void handleBack(ActionEvent event) {
        if (onSuccessCallback != null) onSuccessCallback.run();
    }

    @FXML void handleSwitchToAuto(ActionEvent event) {
        formManual.setVisible(false); formManual.setManaged(false);
        formAuto.setVisible(true);    formAuto.setManaged(true);
        btnTabAuto.getStyleClass().setAll("br-tab-active");
        btnTabManual.getStyleClass().setAll("br-tab");
    }

    @FXML void handleSwitchToManual(ActionEvent event) {
        formAuto.setVisible(false);   formAuto.setManaged(false);
        formManual.setVisible(true);  formManual.setManaged(true);
        btnTabManual.getStyleClass().setAll("br-tab-active");
        btnTabAuto.getStyleClass().setAll("br-tab");
    }

    @FXML void handleTabChart(ActionEvent event) {
        panelHistory.setVisible(false); panelHistory.setManaged(false);
        panelInfo.setVisible(false);    panelInfo.setManaged(false);
        panelChart.setVisible(true);    panelChart.setManaged(true);
        tabBtnChart.getStyleClass().setAll("br-tab-active");
        tabBtnHistory.getStyleClass().setAll("br-tab");
        tabBtnInfo.getStyleClass().setAll("br-tab");
        chartManager.rebuild(bidHistory, currentAuction);
    }

    @FXML void handleTabHistory(ActionEvent event) {
        panelChart.setVisible(false);   panelChart.setManaged(false);
        panelInfo.setVisible(false);    panelInfo.setManaged(false);
        panelHistory.setVisible(true);  panelHistory.setManaged(true);
        tabBtnHistory.getStyleClass().setAll("br-tab-active");
        tabBtnChart.getStyleClass().setAll("br-tab");
        tabBtnInfo.getStyleClass().setAll("br-tab");
    }

    @FXML void handleTabInfo(ActionEvent event) {
        panelHistory.setVisible(false); panelHistory.setManaged(false);
        panelChart.setVisible(false);   panelChart.setManaged(false);
        panelInfo.setVisible(true);     panelInfo.setManaged(true);
        tabBtnInfo.getStyleClass().setAll("br-tab-active");
        tabBtnHistory.getStyleClass().setAll("br-tab");
        tabBtnChart.getStyleClass().setAll("br-tab");
        if (currentAuction == null) return;
        infoName.setText(currentAuction.getName() != null ? currentAuction.getName() : "-");
        infoSeller.setText(currentAuction.getUserDTO().getUsername() != null
                ? currentAuction.getUserDTO().getUsername() : "-");
        infoStartPrice.setText(String.format("%,d ₫", currentAuction.getStartPrice()));
        infoMinIncrement.setText(String.format("%,d ₫", currentAuction.getMinIncrement()));
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        infoStartTime.setText(currentAuction.getStartTime() != null
                ? currentAuction.getStartTime().format(dtFmt) : "-");
        infoEndTime.setText(currentAuction.getEndTime() != null
                ? currentAuction.getEndTime().format(dtFmt) : "-");
        infoDescription.setText(currentAuction.getItemDTO().getDescription() != null
                ? currentAuction.getItemDTO().getDescription() : "-");
    }

    /**
     * Toggle Auto Bid:
     *  - Nếu đang bật → Cancel (gọi CANCEL_AUTO_BID - cần server implement)
     *  - Nếu đang tắt → Start (gọi AUTO_BID)
     *
     * ⚠ CANCEL_AUTO_BID chưa có ở server - nút Cancel sẽ hiện dialog thông báo tạm thời.
     */
    @FXML void handleToggleAutoBid(ActionEvent event) {
        if (isAutoBidding) {
            // ⚠ Server chưa implement CANCEL_AUTO_BID
            // Tạm thời: thông báo user, không gọi server
            showError("Cancel Auto Bid is not yet supported. Your auto-bid will stop automatically when outbid or auction ends.");
            return;

            // TODO: Khi server implement CANCEL_AUTO_BID, thay bằng:
            // btnAutoToggle.setDisable(true);
            // String json = JsonUtils.toJson(ClientMessage.request("CANCEL_AUTO_BID", currentAuction.getId()));
            // dispatcher.request(json, raw -> {
            //     if (ServerResponse.isSuccess(raw)) {
            //         autoBidMaxBid = 0;
            //         applyAutoBidState(false);
            //         txtMaxBid.clear();
            //         txtAutoIncrement.clear();
            //     } else {
            //         showError(ServerResponse.errorMessage(raw));
            //         btnAutoToggle.setDisable(false);
            //     }
            // }, () -> { showError("Connection error."); btnAutoToggle.setDisable(false); });
        }

        // --- Start Auto Bid ---
        if (txtMaxBid.getText().isEmpty() || txtAutoIncrement.getText().isEmpty()) {
            showError("Please fill in all Auto Bidding fields."); return;
        }
        
        long maxBid, increment;
        try {
            maxBid    = Long.parseLong(txtMaxBid.getText().trim());
            increment = Long.parseLong(txtAutoIncrement.getText().trim());
        } catch (NumberFormatException e) {
            showError("Invalid max bid or increment."); return;
        }
        if (maxBid <= currentAuction.getCurrentPrice()) {
            showError("Max bid must be higher than current price."); return;
        }

        btnAutoToggle.setDisable(true);
        btnAutoToggle.setText("Registering...");

        final long finalMaxBid = maxBid;
        String json = JsonUtils.toJson(ClientMessage.request("AUTO_BID",
                new AutoBidRequest(currentAuction.getId(), maxBid, increment)));
        dispatcher.request(json, raw -> {
            if (ServerResponse.isSuccess(raw)) {
                autoBidMaxBid = finalMaxBid;
                applyAutoBidState(true);
            } else {
                showError(ServerResponse.errorMessage(raw));
                resetAutoBidButton();
            }
        }, () -> {
            showError("Connection error.");
            resetAutoBidButton();
        });
    }

    // ------------------- Socket push -------------------

    public void onMessageReceived(String jsonMessage) {
        Platform.runLater(() -> handleServerPush(jsonMessage));
    }

    private void handleServerPush(String json) {
        try {
            JsonObject root   = JsonParser.parseString(json).getAsJsonObject();
            String     action = root.has("action") ? root.get("action").getAsString() : "";
            switch (action.toUpperCase()) {
                case "BID_UPDATE"       -> handleBidUpdate(root);
                case "BID_ERROR"        -> handleBidError(root);
                case "AUCTION_ENDED"    -> handleAuctionEnded(root);
                case "AUTO_BID_STOPPED" -> handleAutoBidStopped(root);
                case "AUCTION_CANCELED" -> handleAuctionCanceled(root);
                default -> { /* bỏ qua push không liên quan */ }
            }
        } catch (Exception e) {
            System.err.println("[BiddingRoom] Push error: " + e.getMessage());
        }
    }

    private void handleAutoBidStopped(JsonObject root) {
        autoBidMaxBid = 0;
        applyAutoBidState(false);
        showBanner("⚠ Auto Bid stopped - current price exceeded your maximum.", 5);
    }

    private void handleBidUpdate(JsonObject root) {
        BidDTO bid = JsonUtils.fromJson(JsonUtils.toJson(root.get("data")), BidDTO.class);
        if (bid == null || currentAuction == null) return;
        if (bid.getAuctionId() != null && currentAuction.getId() != null
                && !bid.getAuctionId().equals(currentAuction.getId())) return;

        String  prevLeader  = currentAuction.getHighestBidderName();
        long    prevPrice   = currentAuction.getCurrentPrice();
        boolean isNewLeader = bid.getBidAmount() >= prevPrice;

        mergeBidIntoHistory(bid);
        lblBidCount.setText(String.valueOf(bidHistory.size()));
        if (!isNewLeader) return;

        currentAuction.setCurrentPrice(bid.getBidAmount());
        currentAuction.setHighestBidderName(bid.getBidderUsername());

        try {
            if (currentUserName != null && currentUserName.equals(bid.getBidderUsername())) {
                Window window = btnBack.getScene().getWindow();
                if (window != null)
                    BidSuccessToastController.show(currentAuction.getName(), bid.getBidAmount(), window);
            } else if (prevLeader != null && prevLeader.equals(currentUserName)
                    && !currentUserName.equals(bid.getBidderUsername())) {
                showOutbidAlert(bid.getBidderUsername(), bid.getBidAmount());
            }
        } catch (Exception ignored) {}

        lblCurrentPrice.setText(String.format("%,d ₫", bid.getBidAmount()));
        lblLeaderName.setText("Leading: " + (bid.getBidderUsername() != null ? bid.getBidderUsername() : "-"));
        if (lblMinHint != null)
            lblMinHint.setText("Min: " + String.format("%,d ₫", bid.getBidAmount() + currentAuction.getMinIncrement()));

        if (bid.getNewEndTime() != null && bid.getNewEndTime().isAfter(currentAuction.getEndTime())) {
            currentAuction.setEndTime(bid.getNewEndTime());
            timer.extendTo(bid.getNewEndTime());
            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            if (infoEndTime != null) infoEndTime.setText(bid.getNewEndTime().format(dtFmt));
        }
        if (chartManager.isReady() && panelChart.isVisible()) chartManager.append(bid.getBidAmount());
        resetPlaceBidButton();
    }

    private void handleBidError(JsonObject root) {
        String message = "Bid failed.";
        if (root.has("data") && root.get("data").isJsonObject()) {
            JsonObject data = root.get("data").getAsJsonObject();
            if (data.has("message")) message = data.get("message").getAsString();
        }
        showBanner(message, 4);
        resetPlaceBidButton();
    }

    private void handleAuctionEnded(JsonObject root) {
        JsonObject data   = root.has("data") ? root.get("data").getAsJsonObject() : new JsonObject();
        String winner     = data.has("winner")     ? data.get("winner").getAsString()   : "Unknown";
        long   finalPrice = data.has("finalPrice") ? data.get("finalPrice").getAsLong() : 0;
        timer.stop();
        btnPlaceBid.setDisable(true);  btnPlaceBid.setText("Finished");
        btnAutoToggle.setDisable(true);
        txtManualBid.setDisable(true);
        if (txtMaxBid != null)        txtMaxBid.setDisable(true);
        if (txtAutoIncrement != null) txtAutoIncrement.setDisable(true);
        if (lblStatusBadge != null) {
            lblStatusBadge.setText("Finished");
            lblStatusBadge.getStyleClass().add("br-badge-finished");
        }
        if (finalPrice > 0) lblCurrentPrice.setText(String.format("%,d ₫", finalPrice));
        if (lblTimer != null) lblTimer.setText("Finished");
        String myUsername = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getUsername() : "";
        String resultMsg = myUsername.equals(winner)
                ? "🎉 Congrats! You won " + String.format("%,d ₫", finalPrice)
                : "Winner: " + winner + " (" + String.format("%,d ₫", finalPrice) + ")";
        if (lblOutbid != null) {
            lblOutbid.setText(resultMsg);
            lblOutbid.setVisible(true);
            lblOutbid.setManaged(true);
        }
    }

    private void handleAuctionCanceled(JsonObject root) {
        JsonObject data    = root.has("data") ? root.get("data").getAsJsonObject() : new JsonObject();
        String reason      = data.has("reason")      ? data.get("reason").getAsString()      : "No reason";
        String auctionName = data.has("auctionName") ? data.get("auctionName").getAsString() : "";
        timer.stop();
        if (lblOutbid != null) {
            lblOutbid.setText("\"" + auctionName + "\" was canceled: " + reason);
            lblOutbid.setVisible(true);
            lblOutbid.setManaged(true);
        }
        new Timeline(new KeyFrame(javafx.util.Duration.seconds(3),
                e -> { if (onSuccessCallback != null) onSuccessCallback.run(); })).play();
    }

    // ------------------- Bid history helpers -------------------

    private void mergeBidHistory(List<BidDTO> incoming) {
        if (incoming == null || incoming.isEmpty()) return;
        List<BidDTO> merged = new ArrayList<>(bidHistory);
        for (BidDTO bid : incoming) { if (!containsBid(merged, bid)) merged.add(bid); }
        merged.sort(Comparator.comparingLong(BidDTO::getBidAmount).reversed()
                .thenComparing(BidDTO::getBidTime, Comparator.nullsLast(Comparator.reverseOrder())));
        bidHistory.setAll(merged);
        if (!bidHistory.isEmpty()) listViewBidHistory.scrollTo(0);
        if (chartManager.isReady() && panelChart.isVisible()) chartManager.rebuild(bidHistory, currentAuction);
    }

    private void mergeBidIntoHistory(BidDTO bid) {
        if (containsBid(bidHistory, bid)) return;
        int i = 0;
        while (i < bidHistory.size() && bidHistory.get(i).getBidAmount() > bid.getBidAmount()) i++;
        bidHistory.add(i, bid);
        listViewBidHistory.scrollTo(Math.max(0, i - 1));
        if (chartManager.isReady() && panelChart.isVisible()) chartManager.rebuild(bidHistory, currentAuction);
    }

    private BidDTO findLatestBid() {
        return bidHistory.stream().max(Comparator.comparingLong(BidDTO::getBidAmount)).orElse(null);
    }

    private boolean containsBid(List<BidDTO> bids, BidDTO target) {
        if (target == null) return false;
        return bids.stream().anyMatch(e -> sameBid(e, target));
    }

    private boolean sameBid(BidDTO a, BidDTO b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.getAuctionId(),     b.getAuctionId())
            && Objects.equals(a.getBidderUsername(), b.getBidderUsername())
            && a.getBidAmount() == b.getBidAmount()
            && Objects.equals(a.getBidTime(),        b.getBidTime())
            && Objects.equals(a.getBidType(),        b.getBidType());
    }

    // ------------------- Misc -------------------

    public void enableSellerViewMode() {
        bidForm.setVisible(false); bidForm.setManaged(false);
        if (btnFollow != null) { btnFollow.setVisible(false); btnFollow.setManaged(false); }
        btnPlaceBid.setDisable(true);
    }

    public void cleanup() {
        timer.stop();
        if (currentAuction != null)
            socket.send(JsonUtils.toJson(ClientMessage.request("UNSUBSCRIBE_AUCTION", currentAuction.getId())));
        socket.removeListener(this);
    }

    private void resetPlaceBidButton() {
        btnPlaceBid.setDisable(false);
        btnPlaceBid.setText("Place Bid");
    }

    private void resetAutoBidButton() {
        btnAutoToggle.setDisable(false);
        btnAutoToggle.setText("Start Auto Bid");
    }

    private void showBanner(String message, int seconds) {
        if (lblOutbid == null) return;
        lblOutbid.setText(message);
        lblOutbid.setVisible(true);
        lblOutbid.setManaged(true);
        if (seconds > 0)
            new Timeline(new KeyFrame(javafx.util.Duration.seconds(seconds),
                    e -> { lblOutbid.setVisible(false); lblOutbid.setManaged(false); })).play();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showOutbidAlert(String newLeader, long newPrice) {
        showBanner(String.format("You have been outbid by %s (%,d ₫)", newLeader, newPrice), 5);
    }

    // ------------------- Image gallery -------------------

    private void setUpItemImage(List<String> urls) {
        this.itemUrls = urls;
        this.currentImageIndex = 0;
        updateImageView();
    }

    @FXML void navBackImage(ActionEvent event) {
        if (itemUrls == null || itemUrls.isEmpty()) return;
        currentImageIndex = currentImageIndex > 0 ? currentImageIndex - 1 : itemUrls.size() - 1;
        updateImageView();
    }

    @FXML void navFrontImage(ActionEvent event) {
        if (itemUrls == null || itemUrls.isEmpty()) return;
        currentImageIndex = currentImageIndex < itemUrls.size() - 1 ? currentImageIndex + 1 : 0;
        updateImageView();
    }

    private void updateImageView() {
        if (itemUrls != null && !itemUrls.isEmpty())
            imgBiddingRoom.setImage(new Image(itemUrls.get(currentImageIndex), true));
    }
<<<<<<< HEAD

}
    
=======
}
>>>>>>> a640ce0863529328f3a15b79d3da7e16112fc661

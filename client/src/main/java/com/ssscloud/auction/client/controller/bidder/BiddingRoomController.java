package com.ssscloud.auction.client.controller.bidder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.controller.bidder.BidSuccessToastController;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.client.networking.*;
import com.ssscloud.auction.client.util.AuctionCountdownTimer;
import com.ssscloud.auction.client.util.PriceChartManager;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.client.util.SessionManager;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class BiddingRoomController implements MessageListener{

    @FXML private Button btnAutoToggle;
    @FXML private Button btnBack;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnTabAuto;
    @FXML private Button btnTabManual;
    @FXML private Button btnBackImage;
    @FXML private Button btnFrontImage;

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
    @FXML private Button btnFollow;

    private AuctionCountdownTimer timer;
    private PriceChartManager     chartManager;

    private XYChart.Series<Number, Number> priceSeries; 
    private int bidSequence = 0;

    private boolean isFollowing = false;
    private boolean isAutoBidding = false;   
    private long autoBidMaxBid = 0;          // Mức giá tối đa mà người dùng sẵn sàng trả khi bật Auto-Bid
    private Timeline countdownTimer; 

    //inject từ màn hình trước
    private AuctionDTO currentAuction;
    private String currentUserName = SessionManager.getInstance().getCurrentUser() != null ? SessionManager.getInstance().getCurrentUser().getUsername() : null;
    private List<String> itemUrls;
    private int currentImageIndex = 0;

    private Runnable onSuccessCallback;
 
    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }
    
    private final ObservableList<BidDTO> bidHistory = FXCollections.observableArrayList(); //cập nhập list view tự động
    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();

    public void initialize() {
        socket.addListener(this);
        timer        = new AuctionCountdownTimer(lblTimer);
        chartManager = new PriceChartManager(priceLineChart, chartXAxis, chartYAxis);
        setupBidHistoryList();
    }

    private void setupBidHistoryList() {
        listViewBidHistory.setItems(bidHistory);    //listView.setItems() sẽ tự động cập nhật khi bidHistory thay đổi
        listViewBidHistory.setPlaceholder(new Label("There is no bid transaction")); //listView placeholder khi không có dữ liệu
        listViewBidHistory.setCellFactory(lv -> new BidHistoryCell()); // thêm cell 
    }

    private static class BidHistoryCell extends ListCell<BidDTO> { //custom cell để hiển thị 
        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

        //LAYOUT NODES
        private final HBox root = new HBox(10);
        private final Label badgeLabel = new Label(); 

        private final VBox nameTimeBox = new VBox(2);
        private final Label nameLabel  = new Label();
        private final Label timeLabel  = new Label();
        private final Region spacer = new Region();
        private final Label amountLabel = new Label();
        BidHistoryCell() {
            // Căn giữa theo trục dọc để badge và số tiền thẳng hàng với tên
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(4, 0, 4, 0));
 
            nameTimeBox.getChildren().addAll(nameLabel, timeLabel);
            nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #222222;");

            timeLabel.getStyleClass().add("br-bid-time");     
            amountLabel.getStyleClass().add("br-bid-amount");

            // Spacer phải grow để chiếm hết khoảng trống còn lại
            HBox.setHgrow(spacer, Priority.ALWAYS);
 
            // Xếp theo thứ tự: badge | tên+giờ | spacer | số tiền
            root.getChildren().addAll(badgeLabel, nameTimeBox, spacer, amountLabel);
        }
        @Override
        protected void updateItem(BidDTO bid, boolean empty) {
            // PHẢI gọi super trước — JavaFX cần xử lý nội bộ trước khi ta override
            super.updateItem(bid, empty);
            if (empty || bid == null) {
                setGraphic(null);
                return;
            }

            //điền dữ liệu
            String type = bid.getBidType(); // "AUTO" hoặc "MANUAL"
            if ("AUTO".equalsIgnoreCase(type)) {
                badgeLabel.setText("Auto");
                badgeLabel.getStyleClass().setAll("br-bid-type-auto");
            } else {
                badgeLabel.setText("Manual");
                badgeLabel.getStyleClass().setAll("br-bid-type-manual");
            }
            nameLabel.setText(bid.getBidderUsername() != null ? bid.getBidderUsername() : "—"); //bidder name
            timeLabel.setText(bid.getBidTime() != null? bid.getBidTime().format(TIME_FMT) : "");//bid time
            amountLabel.setText(String.format("%,d ₫", bid.getBidAmount())); //bid amount
            setGraphic(root);
        }
    }

    // ------------------- @FXML handlers------------------

    @FXML
    void handleFollowRoom(ActionEvent event) {
        if (currentAuction == null) {
            return;
        }
        String action = isFollowing ? "UNFOLLOW_AUCTION" : "FOLLOW_AUCTION";
        if (btnFollow != null) { 
            btnFollow.setDisable(true);
            btnFollow.setText("...");
        }
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
        if (amountText.isEmpty()) {                        // check trước
            showError("Please enter your bid amount");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(amountText);           // parse sau
        } catch (NumberFormatException e) {
            showError("Invalid bid amount");
            return;
        }
        if (amount <= 0){
            showError("Bid amount needs to be larger than 0");
            return;
        }

        if (currentAuction.getBidCount() == 0) {
            // Chưa có bid — chỉ cần >= startPrice
            if (amount < currentAuction.getStartPrice()) {
                showError("Bid amount cannot be lower than start price. " + String.format("%,d ₫", currentAuction.getStartPrice()));
            return;
            }        
        } else {
            // Đã có bid — phải vượt currentPrice + minIncrement
            if (amount <= currentAuction.getCurrentPrice()) {
                showError("Bid amount cannot be lower than current price");
            return;
            }
            if (amount < currentAuction.getCurrentPrice() + currentAuction.getMinIncrement()) {
                showError("Bid amount needs to pass min increment (" + String.format("%,d ₫", currentAuction.getMinIncrement()) + ")");
            return;
        }
    }

        txtManualBid.clear();
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("Progressing..."); 
        PlaceBidRequest req = new PlaceBidRequest(currentAuction.getId(), amount);
        socket.send(JsonUtils.toJson(ClientMessage.request("PLACE_BID", req)));

    }

    @FXML
    void handleBack(ActionEvent event) {
        if (onSuccessCallback != null) {
            onSuccessCallback.run();
        }
    }

    @FXML
    void handleSwitchToAuto(ActionEvent event) {
        formManual.setVisible(false);
        formManual.setManaged(false);
        formAuto.setVisible(true);
        formAuto.setManaged(true);
        btnTabAuto.getStyleClass().setAll("br-tab-active");
        btnTabManual.getStyleClass().setAll("br-tab");
    }

    @FXML
    void handleSwitchToManual(ActionEvent event) {
        formAuto.setVisible(false);
        formAuto.setManaged(false);
        formManual.setVisible(true);
        formManual.setManaged(true);
        btnTabManual.getStyleClass().setAll("br-tab-active");
        btnTabAuto.getStyleClass().setAll("br-tab");

    }

    @FXML
    void handleTabChart(ActionEvent event) {
        panelHistory.setVisible(false);
        panelHistory.setManaged(false);
        panelInfo.setVisible(false);
        panelInfo.setManaged(false);
        panelChart.setVisible(true);
        panelChart.setManaged(true);

        tabBtnChart.getStyleClass().setAll("br-tab-active");
        tabBtnHistory.getStyleClass().setAll("br-tab");
        tabBtnInfo.getStyleClass().setAll("br-tab");

        chartManager.rebuild(bidHistory, currentAuction);
    }

    @FXML
    void handleTabHistory(ActionEvent event) {
        // Hiện panel lịch sử, ẩn các panel khác
        panelChart.setVisible(false);
        panelChart.setManaged(false);
        panelInfo.setVisible(false);
        panelInfo.setManaged(false);
        panelHistory.setVisible(true);
        panelHistory.setManaged(true);

        // Cập nhật trạng thái tab button
        tabBtnHistory.getStyleClass().setAll("br-tab-active");
        tabBtnChart.getStyleClass().setAll("br-tab");
        tabBtnInfo.getStyleClass().setAll("br-tab");

    }

    @FXML
    void handleTabInfo(ActionEvent event) {
        panelHistory.setVisible(false);
        panelHistory.setManaged(false);
        panelChart.setVisible(false);
        panelChart.setManaged(false);
        panelInfo.setVisible(true);
        panelInfo.setManaged(true);

        tabBtnInfo.getStyleClass().setAll("br-tab-active");
        tabBtnHistory.getStyleClass().setAll("br-tab");
        tabBtnChart.getStyleClass().setAll("br-tab");

        if (currentAuction == null) return;

        infoName.setText(currentAuction.getName() != null ? currentAuction.getName() : "—");
        infoSeller.setText(currentAuction.getUserDTO().getUsername() != null ? currentAuction.getUserDTO().getUsername() : "—");
        infoStartPrice.setText(String.format("%,d ₫", currentAuction.getStartPrice()));
        infoMinIncrement.setText(String.format("%,d ₫", currentAuction.getMinIncrement()));
        java.time.format.DateTimeFormatter dtFmt =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
 
        infoStartTime.setText(currentAuction.getStartTime() != null
                ? currentAuction.getStartTime().format(dtFmt) : "—");
 
        infoEndTime.setText(currentAuction.getEndTime() != null
                ? currentAuction.getEndTime().format(dtFmt) : "—");
        infoDescription.setText(currentAuction.getItemDTO().getDescription() != null
                ? currentAuction.getItemDTO().getDescription() : "—");

    }

    @FXML
    void handleToggleAutoBid(ActionEvent event) {
        if (isAutoBidding) {
            btnAutoToggle.setDisable(true);
            new Thread(() -> {
                try {
                    String json = JsonUtils.toJson(ClientMessage.request("CANCEL_AUTO_BID", currentAuction.getId()));
                    String responseJson = socket.sendAndReceive(json);
                    Platform.runLater(() -> {
                        if (responseJson != null) {
                            ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                            String dataJson = JsonUtils.toJson(serverMsg.getData());
                            ApiResponse<?> response = JsonUtils.fromJson(dataJson, ApiResponse.class);
                            if (response != null && response.isSuccess()) {
                                isAutoBidding = false;
                                autoBidMaxBid = 0;
                                resetAutoBidButton();
                            } else {
                                showError(response != null ? response.getMessage() : "Hủy Auto Bid thất bại.");
                                btnAutoToggle.setDisable(false);
                            }
                        } else {
                            showError("Không nhận được phản hồi từ server.");
                            btnAutoToggle.setDisable(false);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showError("Lỗi kết nối Server.");
                        btnAutoToggle.setDisable(false);
                    });
                }
            }).start();
            return; // thoát sớm, không chạy phần start bên dưới    
        }

        if (txtMaxBid.getText().isEmpty() || txtAutoIncrement.getText().isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin Auto Bidding.");
            return;
        }
        long maxBid, increment;
        try {
            maxBid    = Long.parseLong(txtMaxBid.getText().trim());
            increment = Long.parseLong(txtAutoIncrement.getText().trim());
        } catch (NumberFormatException e) {
            showError("Giá tối đa hoặc bước giá không hợp lệ.");
            return;
        }
        if (maxBid <= currentAuction.getCurrentPrice()) {
            showError("Giá tối đa phải cao hơn giá hiện tại.");
            return;
        }
 
        btnAutoToggle.setDisable(true);
        btnAutoToggle.setText("Đang đăng ký...");
 
        new Thread(() -> {
            try {
                AutoBidRequest req = new AutoBidRequest(currentAuction.getId(), maxBid, increment);
                String json = JsonUtils.toJson(ClientMessage.request("AUTO_BID", req));
                String responseJson = socket.sendAndReceive(json); // cần biết có thành công không
 
                Platform.runLater(() -> {
                    if (responseJson == null) {
                        showError("Không nhận được phản hồi từ server.");
                        resetAutoBidButton();
                        return;
                    }
                    // Unwrap ClientMessage wrapper
                    ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                    String dataJson = JsonUtils.toJson(serverMsg.getData());
                    ApiResponse<?> response = JsonUtils.fromJson(dataJson, ApiResponse.class);
 
                    if (response != null && response.isSuccess()) {
                        isAutoBidding = true;
                        autoBidMaxBid = maxBid;
                        isAutoBidding = true;
                        autoBidMaxBid = maxBid;
                        btnAutoToggle.setText("Cancel Auto Bid");
                        btnAutoToggle.setDisable(false);
                        // Nút giữ disable — AUTO_BID_STOPPED push sẽ reset lại
                    } else {
                        showError(response != null ? response.getMessage() : "Đăng ký Auto Bid thất bại.");
                        resetAutoBidButton();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi kết nối Server.");
                    resetAutoBidButton();
                });
            }
        }).start();
    }
    

    public void onMessageReceived(String jsonMessage){
            Platform.runLater(()-> handleServerPush(jsonMessage));
    }

    private void handleServerPush(String json){
        try {
            JsonObject root   = JsonParser.parseString(json).getAsJsonObject();
            String     action = root.has("action") ? root.get("action").getAsString() : "";
 
            switch (action.toUpperCase()) {
                case "BID_UPDATE":       handleBidUpdate(root);       break;
                case "BID_ERROR":        handleBidError(root);        break;
                case "AUCTION_ENDED":    handleAuctionEnded(root);    break;
                case "AUTO_BID_STOPPED":   handleAutoBidStopped(root); break;
                case "AUCTION_CANCELED": handleAuctionCanceled(root); break;
                default:
                    // Action khác không liên quan đến màn hình này — bỏ qua
                    break;
            }
        } catch (Exception e) {
            System.err.println("Lỗi xử lý server push: " + e.getMessage());
        }
    }
    private void handleAutoBidStopped(JsonObject root) {
        isAutoBidding = false;
        btnAutoToggle.getStyleClass().remove("br-btn-auto-active");
        btnAutoToggle.getStyleClass().add("br-btn-secondary");
        btnAutoToggle.setText("Start Auto Bid");
        btnAutoToggle.setDisable(false);
        txtMaxBid.setDisable(false);
        txtAutoIncrement.setDisable(false);

        showBanner("⚠ Auto Bid đã dừng — giá hiện tại vượt ngưỡng tối đa của bạn.", 5);
    }

    private void handleBidUpdate(JsonObject root) {
        BidDTO bid = JsonUtils.fromJson(JsonUtils.toJson(root.get("data")), BidDTO.class);
        if (bid == null || currentAuction == null) return;
        if (bid.getAuctionId() != null
                && currentAuction.getId() != null
                && !bid.getAuctionId().equals(currentAuction.getId())) {
            return;
        }

        String prevLeader   = currentAuction.getHighestBidderName();
        long   prevPrice    = currentAuction.getCurrentPrice();
        boolean isNewLeader = bid.getBidAmount() >= prevPrice;
 
        mergeBidIntoHistory(bid);
        lblBidCount.setText(String.valueOf(bidHistory.size()));
 
        if (!isNewLeader) return;
 
        currentAuction.setCurrentPrice(bid.getBidAmount());
        currentAuction.setHighestBidderName(bid.getBidderUsername());
 
        try {
            if (currentUserName != null && currentUserName.equals(bid.getBidderUsername())) {
                Window window = btnBack.getScene().getWindow();
                if (window != null) BidSuccessToastController.show(
                        currentAuction.getName(), bid.getBidAmount(), window);
            } else if (prevLeader != null && prevLeader.equals(currentUserName)
                    && !currentUserName.equals(bid.getBidderUsername())) {
                showOutbidAlert(bid.getBidderUsername(), bid.getBidAmount());
            }
        } catch (Exception ignored) {}
 
        lblCurrentPrice.setText(String.format("%,d ₫", bid.getBidAmount()));
        lblLeaderName.setText("Leadind: "
                + (bid.getBidderUsername() != null ? bid.getBidderUsername() : "Chưa có ai"));
 
        if (lblMinHint != null) {
            long minRequired = bid.getBidAmount() + currentAuction.getMinIncrement();
            lblMinHint.setText("Min: " + String.format("%,d ₫", minRequired));
        }
 
        // Anti-Sniping: cập nhật endTime nếu được gia hạn
        if (bid.getNewEndTime() != null && bid.getNewEndTime().isAfter(currentAuction.getEndTime())) {
            currentAuction.setEndTime(bid.getNewEndTime());
            timer.extendTo(bid.getNewEndTime()); // util
            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            if (infoEndTime != null) infoEndTime.setText(bid.getNewEndTime().format(dtFmt));
        }
 
        if (chartManager.isReady() && panelChart.isVisible()) {
            chartManager.append(bid.getBidAmount()); // util
        }
        resetPlaceBidButton();
    }

    private void handleBidError(JsonObject root) {
        String message = "Đặt giá thất bại.";
        if (root.has("data") && root.get("data").isJsonObject()) {
            JsonObject data = root.get("data").getAsJsonObject();
            if (data.has("message")) {
                message = data.get("message").getAsString();
            }
        }
        showBanner(message, 4);
        resetPlaceBidButton();
    }
    private void handleAuctionEnded(JsonObject root) {
        JsonObject data = root.has("data") ? root.get("data").getAsJsonObject() : new JsonObject();
        String winner     = data.has("winner")     ? data.get("winner").getAsString()          : "Không xác định";
        long   finalPrice = data.has("finalPrice") ? data.get("finalPrice").getAsLong()        : 0;
        timer.stop();

        // Disable toàn bộ UI đấu giá
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("Finished");
        btnAutoToggle.setDisable(true);
        txtManualBid.setDisable(true);
        if (txtMaxBid != null) txtMaxBid.setDisable(true);
        if (txtAutoIncrement != null) txtAutoIncrement.setDisable(true);

        if (lblStatusBadge != null) {
            lblStatusBadge.setText("Finished");
            lblStatusBadge.getStyleClass().add("br-badge-finished");
        }

        if (finalPrice > 0) lblCurrentPrice.setText(String.format("%,d ₫", finalPrice));
        if (lblTimer != null) lblTimer.setText("Finished");

        String myUsername = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getUsername()
                : "";
        String resultMsg = myUsername.equals(winner)
                ? "🎉 Congrats! You win" + String.format("%,d ₫", finalPrice)
                : "Winner: " + winner
                  + " (" + String.format("%,d ₫", finalPrice) + ")";
        if (lblOutbid != null) {
            lblOutbid.setText(resultMsg);
            lblOutbid.setVisible(true);
            lblOutbid.setManaged(true);
        }

    
    }

    private void handleAuctionCanceled(JsonObject root) {
        JsonObject data   = root.has("data") ? root.get("data").getAsJsonObject() : new JsonObject();
        String reason     = data.has("reason") ? data.get("reason").getAsString() : "Không có lý do";
        String auctionName = data.has("auctionName") ? data.get("auctionName").getAsString() : "";
        timer.stop();

        if (lblOutbid != null) {
            lblOutbid.setText("\"" + auctionName + "\" was cancelleds as  " + reason);
            lblOutbid.setVisible(true);
            lblOutbid.setManaged(true);
        }
        new Timeline(new KeyFrame(javafx.util.Duration.seconds(3),
                e -> { if (onSuccessCallback != null) onSuccessCallback.run(); })).play();
    }


    // Setters — màn hình trước inject context 
    public void setAuction(AuctionDTO auction) {
        this.currentAuction = auction;
        itemUrls = auction.getItemDTO().getImageUrls();

        boolean isFinished = auction.getStatus() == AuctionStatus.FINISHED;
        boolean isCancelled = auction.getStatus() == AuctionStatus.CANCELED;

        Platform.runLater(() -> {
            populateUI();
            setUpItemImage(itemUrls);
            if (btnFollow != null) {
                btnFollow.setDisable(true);
                btnFollow.setText("...");
            }
            if (isFinished || isCancelled) {
                btnPlaceBid.setDisable(true);
                btnPlaceBid.setText(isCancelled ? "Canceled" : "Finished");
                btnAutoToggle.setDisable(true);
                txtManualBid.setDisable(true);
                if (txtMaxBid != null) txtMaxBid.setDisable(true);
                if (txtAutoIncrement != null) txtAutoIncrement.setDisable(true);
                if (lblTimer != null) lblTimer.setText(isCancelled ? "Canceled" : "Finished");
            }
            
        });
        String subJson = JsonUtils.toJson(ClientMessage.request("SUBSCRIBE_AUCTION", auction.getId()));
        dispatcher.request(subJson, subRaw -> {
            loadBidHistoryAsync();
            if (!isFinished && !isCancelled) btnPlaceBid.setDisable(false);
        });

        checkFollowStatus();
        if (!isFinished && !isCancelled) timer.start(auction.getEndTime());
    }

    private void populateUI() {
        if (currentAuction == null) return;
        if (lblAuctionName != null) {
            lblAuctionName.setText(currentAuction.getName() != null
                    ? currentAuction.getName() : "—");
        }
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(String.format("%,d ₫", currentAuction.getCurrentPrice()));
        }

        if (lblLeaderName != null) {
            String leader = currentAuction.getHighestBidderName();
            lblLeaderName.setText("Leading: " + (leader != null ? leader : "—"));
        }
        btnPlaceBid.setDisable(true); // Tạm khóa nút đặt giá cho đến khi tải xong lịch sử và cập nhật giá hiện tại
        if (lblMinIncrement != null) {
            lblMinIncrement.setText(currentAuction.getMinIncrement() > 0
                    ? String.format("%,d ₫", currentAuction.getMinIncrement()) : "—");
        }

        // Giá khởi điểm (dùng currentPrice nếu không có startPrice riêng)
        if (lblStartPrice != null) {
            lblStartPrice.setText(String.format("%,d ₫", currentAuction.getStartPrice()));
        }
        if (lblMinHint != null) {
            if (currentAuction.getBidCount() == 0) {
            lblMinHint.setText("At least: " + String.format("%,d ₫", currentAuction.getStartPrice())
                + " (start price)");
            } else {
                long minRequired = currentAuction.getCurrentPrice() + currentAuction.getMinIncrement();
                lblMinHint.setText("At least: " + String.format("%,d ₫", minRequired));
    }
}
        if (lblStatusBadge != null && currentAuction.getStatus() != null) {
            switch (currentAuction.getStatus()) {
                case RUNNING   -> { lblStatusBadge.setText("Running");  lblStatusBadge.getStyleClass().setAll("br-badge-running"); }
                case FINISHED -> { lblStatusBadge.setText("Finished"); lblStatusBadge.getStyleClass().setAll("br-badge-ended"); }
                case CANCELED -> { lblStatusBadge.setText("Canceled"); lblStatusBadge.getStyleClass().setAll("br-badge-ended"); }
                default       -> lblStatusBadge.setText(currentAuction.getStatus().toString());
            }
        }
    }
    private void checkFollowStatus(){
        if (currentAuction == null) return;
        String json = JsonUtils.toJson(ClientMessage.request("CHECK_FOLLOWING", currentAuction.getId()));
        dispatcher.request(json, raw -> {
            Boolean following = ServerResponse.unwrap(raw, null, Boolean.class);
            if (following != null) isFollowing = following;
            updateFollowButton();
        }, this::updateFollowButton);
    }

    private void loadBidHistoryAsync(){
        if (currentAuction == null) return;
        
        String json = JsonUtils.toJson(ClientMessage.request("GET_BID_HISTORY", currentAuction.getId()));
 
        dispatcher.request(json, raw -> {
            List<BidDTO> historyList = ServerResponse.unwrapDirectList(raw, "GET_BID_HISTORY_RESPONSE", BidDTO.class);
            if (historyList != null) {
                Collections.reverse(historyList);
                mergeBidHistory(historyList);
                BidDTO latestBid = findLatestBid();
                if (latestBid != null && latestBid.getBidAmount() >= currentAuction.getCurrentPrice()) {
                    currentAuction.setCurrentPrice(latestBid.getBidAmount());
                    currentAuction.setHighestBidderName(latestBid.getBidderUsername());
                    lblCurrentPrice.setText(String.format("%,d ₫", latestBid.getBidAmount()));
                    lblLeaderName.setText("Leading: " + latestBid.getBidderUsername());
                    if (lblMinHint != null) {
                        long minRequired = latestBid.getBidAmount() + currentAuction.getMinIncrement();
                        lblMinHint.setText("Min: " + String.format("%,d ₫", minRequired));
                    }
                }
                lblBidCount.setText(String.valueOf(bidHistory.size()));
            }
        });
    }

    private void mergeBidHistory(List<BidDTO> incomingBids) {
        if (incomingBids == null || incomingBids.isEmpty()) return;
        List<BidDTO> merged = new ArrayList<>(bidHistory);
        for (BidDTO bid : incomingBids) {
            if (!containsBid(merged, bid)) merged.add(bid);
        }
        merged.sort(Comparator.comparingLong(BidDTO::getBidAmount).reversed()
                .thenComparing(BidDTO::getBidTime, Comparator.nullsLast(Comparator.reverseOrder())));
        bidHistory.setAll(merged);
        if (!bidHistory.isEmpty()) listViewBidHistory.scrollTo(0);
        if (chartManager.isReady() && panelChart.isVisible()) {
            chartManager.rebuild(bidHistory, currentAuction);
        }
    }

    private void mergeBidIntoHistory(BidDTO bid) {
        if (containsBid(bidHistory, bid)) return;
        int insertIndex = 0;
        while (insertIndex < bidHistory.size()
                && bidHistory.get(insertIndex).getBidAmount() > bid.getBidAmount()) {
            insertIndex++;
        }
        bidHistory.add(insertIndex, bid);
        listViewBidHistory.scrollTo(Math.max(0, insertIndex - 1));
        if (chartManager.isReady() && panelChart.isVisible()) {
            chartManager.rebuild(bidHistory, currentAuction);
        }
    }

    private BidDTO findLatestBid() {
        return bidHistory.stream()
                .max(Comparator.comparingLong(BidDTO::getBidAmount))
                .orElse(null);
    }

    private boolean containsBid(List<BidDTO> bids, BidDTO target) {
        if (target == null) return false;
        return bids.stream().anyMatch(existing -> sameBid(existing, target));
    }

    private boolean sameBid(BidDTO first, BidDTO second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.getAuctionId(), second.getAuctionId())
                && Objects.equals(first.getBidderUsername(), second.getBidderUsername())
                && first.getBidAmount() == second.getBidAmount()
                && Objects.equals(first.getBidTime(), second.getBidTime())
                && Objects.equals(first.getBidType(), second.getBidType());
    }


    //Hỗ trợ navigate của seller
    public void enableSellerViewMode() {
        bidForm.setVisible(false);
        bidForm.setManaged(false);
        if (btnFollow != null) { 
            btnFollow.setVisible(false); 
            btnFollow.setManaged(false); 
        }
        btnPlaceBid.setDisable(true);
    }
 
    // Cleanup khi rời phòng
    public void cleanup() {
        timer.stop();
        if (currentAuction != null) {  
            String json = JsonUtils.toJson(ClientMessage.request("UNSUBSCRIBE_AUCTION", currentAuction.getId()));
            socket.send(json);
        }
        socket.removeListener(this);

    }
 
    //Helpers
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
        if (seconds > 0) {
            new Timeline(new KeyFrame(javafx.util.Duration.seconds(seconds),
                    e -> { lblOutbid.setVisible(false); lblOutbid.setManaged(false); })).play();
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void showOutbidAlert(String newLeader, long newPrice) {
        showBanner(String.format("You have been outbid by %s (%,d ₫)", newLeader, newPrice), 5);
    }

    // --------------- image gallery----------------
    private void setUpItemImage(List<String> itemUrls) {
        this.itemUrls = itemUrls;
        this.currentImageIndex = 0;
        updateImageView();
    }

    @FXML
    void navBackImage(ActionEvent event) {
        if (itemUrls == null || itemUrls.isEmpty()) return;
        if (currentImageIndex > 0) {
            currentImageIndex--;
        } else {
            currentImageIndex = itemUrls.size() - 1;
        }
        updateImageView();
    }

    @FXML
    void navFrontImage(ActionEvent event) {
        if (itemUrls == null || itemUrls.isEmpty()) return;
        if (currentImageIndex < itemUrls.size() - 1) {
            currentImageIndex++;
        } else {
            currentImageIndex = 0;
        }
        updateImageView();
    }

    private void updateImageView() {
        if (itemUrls != null && !itemUrls.isEmpty()) {
            String url = itemUrls.get(currentImageIndex);
            Image image = new Image(url, true);
            imgBiddingRoom.setImage(image);
        }
    }

<<<<<<< HEAD:client/src/main/java/com/ssscloud/auction/client/controller/BiddingRoomController.java
    private void setupBidStatus() {
        if (currentAuction == null) return;
        
        try {
            String json = JsonUtils.toJson(ClientMessage.request("GET_AUTOBID_STATUS", currentAuction.getId()));
            String responseJson = socket.sendAndReceive(json);
            if (responseJson == null) return;
                // Unwrap ClientMessage wrapper
            ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
            if (!"GET_AUTOBID_STATUS_RESPONSE".equals(serverMsg.getAction())) return;

            String rawData = JsonUtils.toJson(serverMsg.getData());
            Type apiType = new TypeToken<ApiResponse<Boolean>>(){}.getType(); 
            ApiResponse<Boolean> apiResponse = JsonUtils.fromJsonGeneric(rawData, apiType);
        
            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                boolean serverAutoBidding = apiResponse.getData();

                Platform.runLater(() -> {
                    if (serverAutoBidding) {
                        // Đồng bộ đầy đủ local state
                        this.isAutoBidding = true;

                        // Switch sang form Auto và lock các field (đang chạy thì không cho sửa)
                        formAuto.setVisible(true);
                        formAuto.setManaged(true);
                        formManual.setVisible(false);
                        formManual.setManaged(false);
                        txtMaxBid.setDisable(true);
                        txtAutoIncrement.setDisable(true);

                        // Cập nhật nút
                        btnAutoToggle.setText("Cancel Auto Bid");
                        btnAutoToggle.setDisable(false);
                        btnAutoToggle.getStyleClass().remove("br-btn-secondary");
                        btnAutoToggle.getStyleClass().add("br-btn-auto-active");
                    } else {
                        this.isAutoBidding = false;
                        resetAutoBidButton();
                        txtMaxBid.setDisable(false);
                        txtAutoIncrement.setDisable(false);
                        btnAutoToggle.getStyleClass().remove("br-btn-auto-active");
                        btnAutoToggle.getStyleClass().add("br-btn-secondary");
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error fetching auto-bid status: " + e.getMessage());
        }
    }

}
=======
}
    
>>>>>>> 488b1141011831eb6f2775f8359b2d063f6cd538:client/src/main/java/com/ssscloud/auction/client/controller/bidder/BiddingRoomController.java

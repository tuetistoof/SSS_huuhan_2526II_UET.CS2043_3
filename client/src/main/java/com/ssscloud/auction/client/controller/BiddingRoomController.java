package com.ssscloud.auction.client.controller;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.util.JsonUtils;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.client.networking.*;
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

    private XYChart.Series<Number, Number> priceSeries; 
    private int bidSequence = 0;

    private boolean isFollowing = false;

    private boolean isAutoBidding = false;   
    private long autoBidMaxBid = 0;   
    private Timeline countdownTimer; 

    //inject từ màn hình trước
    private AuctionDTO currentAuction;
    private String currentUserId;
    private String currentUserName = SessionManager.getInstance().getCurrentUser() != null ? SessionManager.getInstance().getCurrentUser().getUsername() : null;
    private List<String> itemUrls;
    private int currentImageIndex = 0;

    private Runnable onSuccessCallback;
 
    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }
    
    private final ObservableList<BidDTO> bidHistory = FXCollections.observableArrayList(); //cập nhập list view tự động
    
    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();

    public void initialize() {
        imgBiddingRoom.fitWidthProperty().bind(containerImage.widthProperty().subtract(100));
        imgBiddingRoom.fitHeightProperty().bind(containerImage.heightProperty().subtract(40));
        socket.addListener(this);
        setupBidHistoryList();
    }
    private void setupBidHistoryList() {
        listViewBidHistory.setItems(bidHistory);    //listView.setItems() sẽ tự động cập nhật khi bidHistory thay đổi
        listViewBidHistory.setPlaceholder(new Label("Chưa có lịch sử đặt giá nào.")); //listView placeholder khi không có dữ liệu
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

            timeLabel.getStyleClass().add("br-bid-time");     // font nhỏ, màu xám
            amountLabel.getStyleClass().add("br-bid-amount"); // đậm, màu #72243E
 
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
        new Thread(() -> {
            try {
                String json = JsonUtils.toJson(ClientMessage.request(action, currentAuction.getId()));
                String responseJson = socket.sendAndReceive(json);
                Platform.runLater(() -> {
                    if (responseJson == null) {
                        updateFollowButton(); // restore
                        return;
                    }
                    ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                    String dataJson = JsonUtils.toJson(serverMsg.getData());
                    ApiResponse<?> resp = JsonUtils.fromJson(dataJson, ApiResponse.class);
                    if (resp != null && resp.isSuccess()) {
                        isFollowing = !isFollowing;
                    }
                    updateFollowButton();
                });
            } catch (Exception e) {
                Platform.runLater(this::updateFollowButton);
            }
        }).start();  
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
            showError("Vui lòng nhập số tiền muốn đặt.");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(amountText);           // parse sau
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ.");
            return;
        }
        if (amount <= 0){
            showError("Số tiền đặt phải lớn hơn 0");
            return;
        }
        if (amount <= currentAuction.getCurrentPrice()) {
            showError("Giá phải cao hơn giá hiện tại");
            return;
        }

        if (amount < currentAuction.getCurrentPrice() + currentAuction.getMinIncrement()) {
            showError("Giá phải cao hơn ít nhất bước giá tối thiểu");
            return;
        }

        txtManualBid.clear();
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("Đang xử lý..."); 
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

    @SuppressWarnings("unchecked")
    private void setupPriceChart() {
        // Reset mỗi lần build — đảm bảo đúng dữ liệu
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đấu");
        bidSequence = 0;

        int size = bidHistory.size();
        for (int i = size - 1; i >= 0; i--) {
            BidDTO bid = bidHistory.get(i);
            bidSequence++;
            priceSeries.getData().add(new XYChart.Data<>(bidSequence, bid.getBidAmount()));
        }

        if (currentAuction != null && currentAuction.getMinIncrement() > 0)
            chartYAxis.setTickUnit(currentAuction.getMinIncrement());

        chartYAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override public String toString(Number n) { return String.format("%,d", n.longValue()); }
            @Override public Number fromString(String s) { return 0; }
        });
        chartXAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override public String toString(Number n) { return "Bid " + n.intValue(); }
            @Override public Number fromString(String s) { return 0; }
        });
        chartXAxis.setMinorTickVisible(false);
        chartXAxis.setTickUnit(1);

        priceLineChart.getData().clear();
        priceLineChart.getData().add(priceSeries);
        priceLineChart.setLegendVisible(false);
        priceLineChart.setAnimated(false);
        priceLineChart.setCreateSymbols(true);
    }
    

    /** Append điểm mới realtime khi chart đang hiển thị */
    private void appendChartPoint(long bidAmount) {
        if (priceSeries == null || !panelChart.isVisible()) return;
        bidSequence++;
        priceSeries.getData().add(new XYChart.Data<>(bidSequence, bidAmount));
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

        // Build/rebuild chart mỗi lần mở tab — lấy đúng dữ liệu hiện tại
        setupPriceChart();
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
        infoStartPrice.setText(String.format("%,d ₫", currentAuction.getCurrentPrice()));
        infoMinIncrement.setText(String.format("%,d ₫", currentAuction.getMinIncrement()));
        java.time.format.DateTimeFormatter dtFmt =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
 
        infoStartTime.setText(currentAuction.getStartTime() != null
                ? currentAuction.getStartTime().format(dtFmt) : "—");
 
        infoEndTime.setText(currentAuction.getEndTime() != null
                ? currentAuction.getEndTime().format(dtFmt) : "—");


    }

    @FXML
    void handleToggleAutoBid(ActionEvent event) {
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
                        btnAutoToggle.setText("Auto Bidding...");
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
                default:
                    // Action khác không liên quan đến màn hình này — bỏ qua
                    break;
            }
        } catch (Exception e) {
            System.err.println("Lỗi xử lý server push: " + e.getMessage());
        }
    }
    private void handleAutoBidStopped(JsonObject root) {
        Platform.runLater(() -> {
            isAutoBidding = false;

            btnAutoToggle.setText("Bắt đầu Auto Bid");
            btnAutoToggle.getStyleClass().remove("br-btn-stop"); 
            btnAutoToggle.getStyleClass().add("br-btn-secondary");

            txtMaxBid.setDisable(false);
            txtAutoIncrement.setDisable(false);
            
        });
        
    }

    private void handleBidUpdate(JsonObject root) {
        BidDTO bid = JsonUtils.fromJson(JsonUtils.toJson(root.get("data")), BidDTO.class);
        if (bid == null || currentAuction == null) return;

        Platform.runLater(() -> {
            try {
                String prevLeader = currentAuction.getHighestBidderName();

                currentAuction.setCurrentPrice(bid.getBidAmount());
                currentAuction.setHighestBidderName(bid.getBidderUsername());

            
                if (currentUserName != null && currentUserName.equals(bid.getBidderUsername())) {
                    Window window = btnBack.getScene().getWindow();
                    if (window != null) {
                        BidSuccessToastController.show(
                            currentAuction.getName(), 
                            bid.getBidAmount(), 
                            window
                        );
                    }
                } else if (prevLeader != null && prevLeader.equals(currentUserName) && !currentUserName.equals(bid.getBidderUsername())) {
                    showOutbidAlert(bid.getBidderUsername(), bid.getBidAmount());
                
                }
            // 3. Cập nhật UI chính (Giá & Người dẫn đầu)
                String formattedPrice = String.format("%,d ₫", bid.getBidAmount());
                lblCurrentPrice.setText(formattedPrice);
                lblLeaderName.setText("Dẫn đầu: " + (bid.getBidderUsername() != null ? bid.getBidderUsername() : "Chưa có ai"));

            // 4. Cập nhật lịch sử đấu giá (đưa lên đầu danh sách)
                bidHistory.add(0, bid);
                lblBidCount.setText(String.valueOf(bidHistory.size()));

            // 5. Cập nhật gợi ý giá tối thiểu cho lượt bid tiếp theo
                if (lblMinHint != null) {
                    long minRequired = bid.getBidAmount() + currentAuction.getMinIncrement();
                    lblMinHint.setText("Tối thiểu: " + String.format("%,d ₫", minRequired));
                }

            // 6. Xử lý gia hạn thời gian (nếu có)
                if (bid.getNewEndTime() != null && bid.getNewEndTime().isAfter(currentAuction.getEndTime())) {
                    currentAuction.setEndTime(bid.getNewEndTime());
                    DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                
                    if (infoEndTime != null) {
                        infoEndTime.setText(bid.getNewEndTime().format(dtFmt));
                    }
                    if (lblTimer != null) {
                        lblTimer.setText("Gia hạn đến: " + bid.getNewEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    }
                }

            // // 7. Logic Auto-Bid (Kiểm tra nếu mình bị vượt giá)
            //     if (isAutoBidding) {
            //         boolean iAmWinning = currentUserId.equals(String.valueOf(bid.getHighestBidderId())); 
            //         long nextPriceNeeded = bid.getBidAmount() + currentAuction.getMinIncrement();
            //         boolean canStillBid = autoBidMaxBid >= nextPriceNeeded;

            //         if (!iAmWinning && !canStillBid) {
            //             isAutoBidding = false;
            //             autoBidMaxBid = 0;
            //             resetAutoBidButton();
            //             showInfo("Auto Bid đã dừng — Giá hiện tại (" + formattedPrice + ") đã vượt ngưỡng tối đa của bạn.");
            //         }
            //     }

            // 8. Các hiệu ứng bổ sung
                appendChartPoint(bid.getBidAmount()); 
                resetPlaceBidButton();

            } catch (Exception e) {
                System.err.println("Lỗi cập nhật UI realtime: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleBidError(JsonObject root) {
        String message = "Đặt giá thất bại.";
        if (root.has("data") && root.get("data").isJsonObject()) {
            JsonObject data = root.get("data").getAsJsonObject();
            if (data.has("message")) {
                message = data.get("message").getAsString();
            }
        }
        showError(message);
        resetPlaceBidButton();
    }
    private void handleAuctionEnded(JsonObject root) {
        // Khóa toàn bộ UI đấu giá
        btnPlaceBid.setDisable(true);
        btnAutoToggle.setDisable(true);
        txtManualBid.setDisable(true);
 
        String winner = root.has("data") && root.get("data").getAsJsonObject().has("winner")
                ? root.get("data").getAsJsonObject().get("winner").getAsString()
                : "Không xác định";
 
        showInfo("Phiên đấu giá đã kết thúc. Người thắng: " + winner);
    }


    // Setters — màn hình trước inject context 
    public void setAuction(AuctionDTO auction)  { 
        this.currentAuction  = auction; 
        itemUrls = auction.getItemDTO().getImageUrls();
        Platform.runLater(() -> {
            populateUI();
            setUpItemImage(itemUrls);});
        new Thread(() -> {
            loadBidHistory();
            subcribeToAuction();
            Platform.runLater(() -> btnPlaceBid.setDisable(false));
        }).start();
        checkFollowStatus();
        startTimer();

    }

    public void setUserId(String userId)         { this.currentUserId   = userId; }
    public void setUserName(String userName)     { this.currentUserName = userName; }

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
            lblLeaderName.setText("Dẫn đầu: " + (leader != null ? leader : "—"));
        }
        btnPlaceBid.setDisable(true); // Tạm khóa nút đặt giá cho đến khi tải xong lịch sử và cập nhật giá hiện tại
        if (lblMinIncrement != null) {
            lblMinIncrement.setText(currentAuction.getMinIncrement() > 0
                    ? String.format("%,d ₫", currentAuction.getMinIncrement()) : "—");
        }

        // Giá khởi điểm (dùng currentPrice nếu không có startPrice riêng)
        if (lblStartPrice != null) {
            lblStartPrice.setText(String.format("%,d ₫", currentAuction.getCurrentPrice()));
        }
        if (lblMinHint != null && currentAuction.getMinIncrement() > 0) {
            long minRequired = currentAuction.getCurrentPrice() + currentAuction.getMinIncrement();
            lblMinHint.setText("Tối thiểu: " + String.format("%,d ₫", minRequired));
        }
        if (lblStatusBadge != null && currentAuction.getStatus() != null) {
            switch (currentAuction.getStatus()) {
                case RUNNING   -> { lblStatusBadge.setText("Đang chạy");  lblStatusBadge.getStyleClass().setAll("br-badge-running"); }
                case FINISHED -> { lblStatusBadge.setText("Đã kết thúc"); lblStatusBadge.getStyleClass().setAll("br-badge-ended"); }
                default       -> lblStatusBadge.setText(currentAuction.getStatus().toString());
            }
        }
    }
    private void checkFollowStatus(){
        if (currentAuction == null) return;
        new Thread(()-> {
            try {
                String json = JsonUtils.toJson(ClientMessage.request("CHECK_FOLLOWING", currentAuction.getId()));
                String responseJson = socket.sendAndReceive(json);
                if (responseJson == null) return;
                ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                String dataJson = JsonUtils.toJson(serverMsg.getData());
                ApiResponse<?> resp = JsonUtils.fromJson(dataJson, ApiResponse.class);
                if (resp != null && resp.isSuccess() && resp.getData() != null) {
                    isFollowing = Boolean.parseBoolean(resp.getData().toString());
                }
                Platform.runLater(this::updateFollowButton);
            } catch (Exception e) {
                System.err.println("Lỗi kiểm tra trạng thái follow: " + e.getMessage());
            }
        }).start();
    }

    private void startTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        if (currentAuction == null || currentAuction.getEndTime() == null) return;

        countdownTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.Duration remaining = java.time.Duration.between(now, currentAuction.getEndTime());
            if (remaining.isNegative() || remaining.isZero()) {
                if (lblTimer != null) lblTimer.setText("Còn lại: 00:00:00");
                countdownTimer.stop();
                return;
            }
            long h = remaining.toHours();
            long m = remaining.toMinutesPart();
            long s = remaining.toSecondsPart();
            if (lblTimer != null) {
                lblTimer.setText(String.format("Còn lại: %02d:%02d:%02d", h, m, s));
            }
        }));
        countdownTimer.setCycleCount(Animation.INDEFINITE);
        countdownTimer.play();
    }
    
    private void loadBidHistory(){
        if (currentAuction == null) return;
        
        try {
            String json = JsonUtils.toJson(ClientMessage.request("GET_BID_HISTORY", currentAuction.getId()));
            String responseJson = socket.sendAndReceive(json);
            if (responseJson == null) return;
                // Unwrap ClientMessage wrapper
            ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
            if (!"GET_BID_HISTORY_RESPONSE".equals(serverMsg.getAction())) return;

            String rawData = JsonUtils.toJson(serverMsg.getData());
            Type apiType = new TypeToken<ApiResponse<List<BidDTO>>>(){}.getType(); 
            ApiResponse<List<BidDTO>> apiResponse = JsonUtils.fromJsonGeneric(rawData, apiType);
            // if (apiResponse == null || !apiResponse.isSuccess() || apiResponse.getData() == null) return;

            //     //Double-parsed
            // String listJson = JsonUtils.toJson(apiResponse.getData());
            // Type listType = new TypeToken<List<BidDTO>>(){}.getType();
            // List<BidDTO> historyList = JsonUtils.fromJsonGeneric(listJson, listType);
            // if (historyList == null) return;
            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                List<BidDTO> historyList = apiResponse.getData();
                Collections.reverse(historyList);
                Platform.runLater(() -> {
                    bidHistory.setAll(historyList);
                    lblBidCount.setText(String.valueOf(historyList.size()));
                    if (!historyList.isEmpty()) {
                        BidDTO latestBid = historyList.get(0);
                        currentAuction.setCurrentPrice(latestBid.getBidAmount());
                        currentAuction.setHighestBidderName(latestBid.getBidderUsername());

                        lblCurrentPrice.setText(String.format("%,d ₫", latestBid.getBidAmount()));
                        lblLeaderName.setText("Dẫn đầu: " + latestBid.getBidderUsername());
                        if (lblMinHint != null) {
                            long minRequired = latestBid.getBidAmount() + currentAuction.getMinIncrement();
                            lblMinHint.setText("Tối thiểu: " + String.format("%,d ₫", minRequired));
                        }
                    }
        
                });
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải lịch sử đặt giá: " + e.getMessage());
        }


    }

    private void subcribeToAuction(){
        if (currentAuction == null) return;
        new Thread(() -> {
            try {
                String json = JsonUtils.toJson(ClientMessage.request("SUBSCRIBE_AUCTION", currentAuction.getId()));
                socket.send(json);
            } catch (Exception e) {
                System.err.println("Lỗi đăng ký nhận push: " + e.getMessage());
            }
        }).start();
    }
 
    // Cleanup khi rời phòng
    public void cleanup() {
        
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void showOutbidAlert(String newLeader, long newPrice) {
        // Dùng non-blocking notification thay vì Alert để không chặn UI
        // lblOutbid là Label nhỏ hiển thị ngay dưới giá dẫn đầu
        if (lblOutbid != null) {
            lblOutbid.setText(String.format("⚠ Bạn vừa bị %s vượt giá (%,d ₫)", newLeader, newPrice));
            lblOutbid.setVisible(true);
            lblOutbid.setManaged(true);
            // Tự ẩn sau 5 giây
            new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5),
                    e -> {
                        lblOutbid.setVisible(false);
                        lblOutbid.setManaged(false);
                    })
            ).play();
        }
    }


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
}
    
package com.ssscloud.auction.client.controller;
import java.time.format.DateTimeFormatter;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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

    @FXML private ListView<BidDTO> listViewBidHistory;

    @FXML private VBox panelChart;
    @FXML private VBox panelHistory;
    @FXML private VBox panelInfo;
    @FXML private LineChart<?, ?> priceLineChart;

    @FXML private Button tabBtnChart;
    @FXML private Button tabBtnHistory;
    @FXML private Button tabBtnInfo;
    @FXML private TextField txtAutoIncrement;
    @FXML private TextField txtManualBid;
    @FXML private TextField txtMaxBid;

    @FXML private Button btnFollow;

    private boolean isFollowing = false;

    private boolean isAutoBidding = false;   
    private long autoBidMaxBid = 0;    

    //inject từ màn hình trước
    private AuctionDTO currentAuction;
    private String currentUserId;
    private String currentUserName;

    private Runnable onSuccessCallback;
 
    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }
    
    private final ObservableList<BidDTO> bidHistory = FXCollections.observableArrayList(); //cập nhập list view tự động
    
    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();

    public void initialize() {
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
        private final Label badgeLabel = new Label(); //manual hoặc auto

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
    void handleFollow(ActionEvent event) {
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
                String json = JsonUtils.toJson(
                        ClientMessage.request(action, currentAuction.getId()));
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
            btnFollow.getStyleClass().setAll("br-btn-following"); 
        } else {
            btnFollow.setText("Follow");
            btnFollow.getStyleClass().setAll("br-btn-follow");
        }
    }

    private void checkFollowStatus(){
        if (currentAuction == null) return;
        new Thread(()-> {
            try {
                String json = JsonUtils.toJson(ClientMessage.request("CHECK_WATCH_STATUS", currentAuction.getId()));
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
        infoSeller.setText(currentAuction.getSellerName() != null ? currentAuction.getSellerName() : "—");
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
                default:
                    // Action khác không liên quan đến màn hình này — bỏ qua
                    break;
            }
        } catch (Exception e) {
            System.err.println("Lỗi xử lý server push: " + e.getMessage());
        }
    }

    private void handleBidUpdate(JsonObject root) {
        BidDTO bid = JsonUtils.fromJson(JsonUtils.toJson(root.get("data")), BidDTO.class);
        if (bid == null) return;
 
        // Cập nhật UI
        lblCurrentPrice.setText(String.format("%,d ₫", bid.getCurrentPrice()));
        if (bid.getBidderUsername() != null) lblLeaderName.setText(bid.getBidderUsername());
        currentAuction.setCurrentPrice(bid.getCurrentPrice());
        bidHistory.add(0, bid);
        lblBidCount.setText(String.valueOf(bidHistory.size()));
        resetPlaceBidButton();
        // Nếu có gia hạn thời gian, cập nhật luôn
        if (bid.getNewEndTime() != null && bid.getNewEndTime().isAfter(currentAuction.getEndTime())) {
            currentAuction.setEndTime(bid.getNewEndTime());
            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            if (infoEndTime != null){
                infoEndTime.setText(bid.getNewEndTime().format(dtFmt));
            }
            if (lblTimer != null){
                lblTimer.setText("Gia hạn đến: "+ bid.getNewEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
        }
        // Tự check trạng thái auto bid — không cần server push riêng
        if (isAutoBidding) {
            boolean iAmWinning  = currentUserId.equals(bid.getHighestBidderId());
            boolean canStillBid = autoBidMaxBid >= bid.getCurrentPrice()
                                + currentAuction.getMinIncrement();
 
            if (!iAmWinning && !canStillBid) {
                // Hết hạn mức → reset tự động
                isAutoBidding = false;
                autoBidMaxBid = 0;
                resetAutoBidButton();
                showInfo("Auto Bid đã dừng — đã đạt giá tối đa.");
            }
        }
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
        loadBidHistory(); 
        subcribeToAuction();
        checkFollowStatus();
    }
    public void setUserId(String userId)         { this.currentUserId   = userId; }
    public void setUserName(String userName)     { this.currentUserName = userName; }

    private void loadBidHistory(){
        if (currentAuction == null) return;
        new Thread(() -> {
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
                if (apiResponse == null || !apiResponse.isSuccess() || apiResponse.getData() == null) return;

                //Double-parsed
                String listJson = JsonUtils.toJson(apiResponse.getData());
                Type listType = new TypeToken<List<BidDTO>>(){}.getType();
                List<BidDTO> historyList = JsonUtils.fromJsonGeneric(listJson, listType);
                if (historyList == null) return;
                Platform.runLater(() -> {
                    bidHistory.setAll(historyList);
                    lblBidCount.setText(String.valueOf(historyList.size()));
                });
            } catch (Exception e) {
                System.err.println("Lỗi tải lịch sử đặt giá: " + e.getMessage());
            }
        }).start();

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

    @FXML
    void navBackImage(ActionEvent event) {

    }

    @FXML
    void navFrontImage(ActionEvent event) {

    }
}

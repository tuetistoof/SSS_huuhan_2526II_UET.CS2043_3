package com.ssscloud.auction.client.controller;

import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.util.SceneManager;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.dto.response.SellerDisplayInfoDTO; // bạn tự tạo
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SellerDashboardController
 *
 * Hiển thị toàn bộ phiên đấu giá của seller dưới dạng bảng tổng hợp.
 * Mỗi row có nút "Xem phòng" → navigate sang BiddingRoomController
 * với bid controls bị disable (seller chỉ quan sát).
 *
 * DTO phụ thuộc: SellerDisplayInfoDTO (bạn tự tạo phía server + common)
 * Fields cần thiết:
 *   String  auctionId
 *   String  itemName
 *   long    startingPrice
 *   long    currentPrice
 *   int     bidCount
 *   AuctionStatus status
 *   LocalDateTime endTime
 */
public class SellerDashboardController {

    // ── FXML: metric cards ─────────────────────────────────────────────────
    @FXML private Label lblRunningCount;   // "Phiên đang chạy"
    @FXML private Label lblFinishedCount;  // "Phiên đã kết thúc"
    @FXML private Label lblAccountBalance; // "Số dư tài khoản"
    @FXML private Label lblBankAccount;    // "Số tài khoản"

    // ── FXML: filter tabs ──────────────────────────────────────────────────
    @FXML private ToggleButton tabAll;
    @FXML private ToggleButton tabRunning;
    @FXML private ToggleButton tabOpen;
    @FXML private ToggleButton tabDone;

    // ── FXML: table ────────────────────────────────────────────────────────
    @FXML private TableView<SellerDisplayInfoDTO>         tblAuctions;
    @FXML private TableColumn<SellerDisplayInfoDTO, String> colTitle;
    @FXML private TableColumn<SellerDisplayInfoDTO, String> colStrartingPrice; // typo intentional: matches fxml id
    @FXML private TableColumn<SellerDisplayInfoDTO, String> colCurrentPrice;
    @FXML private TableColumn<SellerDisplayInfoDTO, String> colBidCount;
    @FXML private TableColumn<SellerDisplayInfoDTO, String> colTimeLeft;
    @FXML private TableColumn<SellerDisplayInfoDTO, String> colStatus;
    @FXML private TableColumn<SellerDisplayInfoDTO, Void>   colActions;

    // ── Internal state ─────────────────────────────────────────────────────
    private final AuctionClientSocket socket  = AuctionClientSocket.getInstance();
    private final SessionManager      session = SessionManager.getInstance();

    private final ObservableList<SellerDisplayInfoDTO> masterList   = FXCollections.observableArrayList();
    private final FilteredList<SellerDisplayInfoDTO>   filteredList = new FilteredList<>(masterList, p -> true);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Số tài khoản ngân hàng — load từ UserDTO sau khi đăng nhập
    private String currentBankAccount = null;


    @FXML
    public void initialize() {
        setupTable();
        tblAuctions.setItems(filteredList);
        populateAccountInfo();
        loadMyAuctions();
    }

    // ══════════════════════════════════════════════════════════════════════
    // TABLE SETUP
    // ══════════════════════════════════════════════════════════════════════

    private void setupTable() {
        colTitle.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getItemName()));

        colStrartingPrice.setCellValueFactory(
                c -> new SimpleStringProperty(
                        String.format("%,d ₫", c.getValue().getStartingPrice())));

        colCurrentPrice.setCellValueFactory(
                c -> new SimpleStringProperty(
                        String.format("%,d ₫", c.getValue().getCurrentPrice())));

        colBidCount.setCellValueFactory(
                c -> new SimpleStringProperty(String.valueOf(c.getValue().getBidCount())));

        colTimeLeft.setCellValueFactory(c -> {
            LocalDateTime end = c.getValue().getEndTime();
            if (end == null) return new SimpleStringProperty("—");
            Duration remaining = Duration.between(LocalDateTime.now(), end);
            if (remaining.isNegative()) return new SimpleStringProperty("Ended");
            long h = remaining.toHours();
            long m = remaining.toMinutesPart();
            long s = remaining.toSecondsPart();
            return new SimpleStringProperty(String.format("%02d:%02d:%02d", h, m, s));
        });

        colStatus.setCellValueFactory(
                c -> new SimpleStringProperty(statusLabel(c.getValue().getStatus())));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("View room");

            {
                btnView.getStyleClass().add("btn-primary");
                btnView.setOnAction(e -> {
                    SellerDisplayInfoDTO row = getTableView().getItems().get(getIndex());
                    openBiddingRoom(row.getAuctionId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(btnView));
            }
        });
    }


    private void loadMyAuctions() {
        String sellerId = session.getCurrentUser() != null
                ? session.getCurrentUser().getId() : null;
        if (sellerId == null) return;

        new Thread(() -> {
            try {
                ClientMessage msg = new ClientMessage("GET_MY_AUCTIONS", null);
                String jsonResponse = socket.sendAndReceive(JsonUtils.toJson(msg));
                if (jsonResponse == null) return;

                ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);
                if (!"GET_MY_AUCTIONS_RESPONSE".equals(serverMsg.getAction())) return;

                String rawData = JsonUtils.toJson(serverMsg.getData());
                ApiResponse<?> resp = JsonUtils.fromJson(rawData, ApiResponse.class);
                if (resp == null || !resp.isSuccess()) return;

                String listJson = JsonUtils.toJson(resp.getData());
                ListResponse<SellerDisplayInfoDTO> listResp = JsonUtils.fromJson(listJson, ListResponse.class, SellerDisplayInfoDTO.class);
                if (listResp == null) return;

                List<SellerDisplayInfoDTO> items = listResp.getData();

                Platform.runLater(() -> {
                    masterList.setAll(items);
                    refreshMetrics(items);
                });

            } catch (Exception e) {
                System.err.println("[SellerDashboard] loadMyAuctions error: " + e.getMessage());
            }
        }).start();
    }

    
    private void populateAccountInfo() {
        if (session.getCurrentUser() == null) return;
        // TODO: lấy từ UserDTO khi server trả về balance + bankAccount
        // Hiện để placeholder — thay bằng getter thực khi DTO có sẵn
        lblAccountBalance.setText("—");
        lblBankAccount.setText("—");
    }

    private void refreshMetrics(List<SellerDisplayInfoDTO> items) {
        int running  = 0;
        int finished = 0;

        for (SellerDisplayInfoDTO dto : items) {
            if (dto.getStatus() == AuctionStatus.RUNNING || dto.getStatus() == AuctionStatus.OPEN) {
                running++;
            }
            if (dto.getStatus() == AuctionStatus.FINISHED || dto.getStatus() == AuctionStatus.PAID || dto.getStatus() == AuctionStatus.CANCELED) {
                finished++;
            }
        }

        lblRunningCount.setText(String.valueOf(running));
        lblFinishedCount.setText(String.valueOf(finished));
    }

    @FXML
    private void filterAll() {
        filteredList.setPredicate(p -> true);
        setActiveTab(tabAll);
    }

    @FXML
    private void filterRunning() {
        filteredList.setPredicate(
                dto -> dto.getStatus() == AuctionStatus.RUNNING);
        setActiveTab(tabRunning);
    }

    @FXML
    private void filterOpen() {
        filteredList.setPredicate(
                dto -> dto.getStatus() == AuctionStatus.OPEN);
        setActiveTab(tabOpen);
    }

    @FXML
    private void filterFinished() {
        filteredList.setPredicate(dto ->
                dto.getStatus() == AuctionStatus.FINISHED
                || dto.getStatus() == AuctionStatus.PAID
                || dto.getStatus() == AuctionStatus.CANCELED);
        setActiveTab(tabDone);
    }

    private void setActiveTab(ToggleButton active) {
        for (ToggleButton tb : new ToggleButton[]{tabAll, tabRunning, tabOpen, tabDone}) {
            tb.setSelected(tb == active);
        }
    }

    @FXML
    private void changeBankAccount() {
        // TODO: mở dialog nhập / đổi số tài khoản ngân hàng
        // Khi server hỗ trợ UPDATE_BANK_ACCOUNT thì gửi request ở đây
        // và gọi lblBankAccount.setText(newAccount) sau khi thành công
    }

    @FXML
    private void openCreateDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/CreateAuction.fxml"));
            Parent root = loader.load();

            CreateAuctionController ctrl = loader.getController();
            // Khi tạo xong → reload danh sách
            ctrl.setOnSuccessCallback(auction -> loadMyAuctions());

            Stage modal = new Stage();
            modal.setTitle("Tạo phiên đấu giá mới");
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setScene(new Scene(root));
            modal.showAndWait();

        } catch (IOException e) {
            System.err.println("[SellerDashboard] openCreateDialog error: " + e.getMessage());
        }
    }

=

    /**
     * Fetch AuctionDTO đầy đủ theo auctionId, rồi mở BiddingRoom ở chế độ
     * seller: bid controls disabled, nút Follow ẩn.
     */
    private void openBiddingRoom(String auctionId) {
        new Thread(() -> {
            try {
                // Reuse action GET_AUCTION_DETAILS đã có
                ClientMessage req = ClientMessage.request("GET_AUCTION_DETAILS",
                        java.util.Map.of("auctionId", auctionId));
                String responseJson = socket.sendAndReceive(JsonUtils.toJson(req));
                if (responseJson == null) return;

                ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                String rawData = JsonUtils.toJson(serverMsg.getData());
                ApiResponse<?> resp = JsonUtils.fromJson(rawData, ApiResponse.class);
                if (resp == null || !resp.isSuccess()) return;

                String dtoJson = JsonUtils.toJson(resp.getData());
                AuctionDTO auction = JsonUtils.fromJson(dtoJson, AuctionDTO.class);
                if (auction == null) return;

                Platform.runLater(() -> loadBiddingRoomAsViewer(auction));

            } catch (Exception e) {
                System.err.println("[SellerDashboard] openBiddingRoom error: " + e.getMessage());
            }
        }).start();
    }

    private void loadBiddingRoomAsViewer(AuctionDTO auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/BiddingRoom.fxml"));
            Parent root = loader.load();

            BiddingRoomController ctrl = loader.getController();

            ctrl.setOnSuccessCallback(() -> {
                ctrl.cleanup();
                loadMyAuctions();
                SceneManager.getInstance().showSellerDashboard();
            });

            ctrl.setAuction(auction);

            // ── Disable toàn bộ bid controls sau khi inject ──────────────
            //   setAuction() đã gọi Platform.runLater(populateUI),
            //   nên ta cũng wrap trong runLater để chạy sau populateUI.
            Platform.runLater(() -> disableBidControls(ctrl));

            // Chuyển scene
            Stage stage = (Stage) tblAuctions.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            System.err.println("[SellerDashboard] loadBiddingRoomAsViewer error: " + e.getMessage());
        }
    }

    /**
     * Tắt toàn bộ phần đặt giá trong BiddingRoomController.
     * Dùng reflection-free: truy cập trực tiếp qua các setter/method công khai
     * mà BiddingRoomController expose, hoặc thêm method disableBidMode() vào đó.
     *
     * ── Cách khuyên dùng ──────────────────────────────────────────────────
     * Thêm method sau vào BiddingRoomController:
     *
     *   public void enableSellerViewMode() {
     *       // Bid controls
     *       btnPlaceBid.setDisable(true);
     *       btnPlaceBid.setVisible(false);   // hoặc chỉ disable tuỳ UI
     *       btnAutoToggle.setDisable(true);
     *       btnAutoToggle.setVisible(false);
     *       txtManualBid.setDisable(true);
     *       txtMaxBid.setDisable(true);
     *       txtAutoIncrement.setDisable(true);
     *       btnTabManual.setVisible(false);
     *       btnTabAuto.setVisible(false);
     *       formManual.setVisible(false);
     *       formManual.setManaged(false);
     *       formAuto.setVisible(false);
     *       formAuto.setManaged(false);
     *       // Follow không có nghĩa với seller
     *       if (btnFollow != null) {
     *           btnFollow.setVisible(false);
     *           btnFollow.setManaged(false);
     *       }
     *   }
     *
     * Sau đó gọi: ctrl.enableSellerViewMode();
     */
    private void disableBidControls(BiddingRoomController ctrl) {
        ctrl.enableSellerViewMode(); // method bạn cần thêm vào BiddingRoomController
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private String statusLabel(AuctionStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case OPEN     -> "Sắp mở";
            case RUNNING  -> "Đang chạy";
            case FINISHED -> "Kết thúc";
            case PAID     -> "Đã thanh toán";
            case CANCELED -> "Đã hủy";
        };
    }
}
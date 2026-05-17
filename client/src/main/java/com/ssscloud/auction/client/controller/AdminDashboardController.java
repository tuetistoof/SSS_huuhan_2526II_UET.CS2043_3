package com.ssscloud.auction.client.controller;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.gson.reflect.TypeToken;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.AdminDisplayDTO;
import com.ssscloud.auction.common.dto.response.AdminMetrics;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AdminDashboardController {

    // ── Auction table ──
    @FXML private TableColumn<AdminDisplayDTO, Void>   aColAction;
    @FXML private TableColumn<AdminDisplayDTO, String> aColEnd;
    @FXML private TableColumn<AdminDisplayDTO, String> aColName;
    @FXML private TableColumn<AdminDisplayDTO, String> aColPrice;
    @FXML private TableColumn<AdminDisplayDTO, String> aColSeller;
    @FXML private TableColumn<AdminDisplayDTO, String> aColStatus;
    @FXML private TableView<AdminDisplayDTO>           tblAuctions;

    // ── User table ──
    @FXML private TableColumn<UserDTO, Void>   uColAction;
    @FXML private TableColumn<UserDTO, String> uColAccountBalance;
    @FXML private TableColumn<UserDTO, String> uColEmail;
    @FXML private TableColumn<UserDTO, String> uColRole;
    @FXML private TableColumn<UserDTO, String> uColUsername;
    @FXML private TableView<UserDTO>           tblUsers;

    // ── Filter buttons ──
    @FXML private ToggleButton aFilterAll;
    @FXML private ToggleButton aFilterDone;
    @FXML private ToggleButton aFilterOpen;
    @FXML private ToggleButton aFilterRunning;
    @FXML private ToggleButton uFilterAll;
    @FXML private ToggleButton uFilterBidder;
    @FXML private ToggleButton uFilterSeller;

    // ── Metric labels ──
    @FXML private Label lblMetricEnded;
    @FXML private Label lblMetricRunning;
    @FXML private Label lblMetricUsers;

    // ── Panels & tabs ──
    @FXML private VBox         panelAuctions;
    @FXML private VBox         panelUsers;
    @FXML private ToggleButton tabAuction;
    @FXML private ToggleButton tabUser;

    // ─────────────────────────── CONSTANTS & STATE ───────────────────────────

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();

    private final ObservableList<AdminDisplayDTO> auctionMaster   = FXCollections.observableArrayList();
    private final FilteredList<AdminDisplayDTO>   auctionFiltered = new FilteredList<>(auctionMaster, p -> true);
    private final ObservableList<UserDTO>         userMaster      = FXCollections.observableArrayList();
    private final FilteredList<UserDTO>           userFiltered    = new FilteredList<>(userMaster, p -> true);

    private Consumer<AdminDisplayDTO> onOpenBidRoomHandler;

    public void setOnOpenBidRoom(Consumer<AdminDisplayDTO> handler) {
        this.onOpenBidRoomHandler = handler;
    }

    // ─────────────────────────── INIT ───────────────────────────

    @FXML
    public void initialize() {
        setupAuctionTable();
        setupUserTable();
        tblAuctions.setItems(auctionFiltered);
        tblUsers.setItems(userFiltered);
        loadMetrics();
        loadAuctions();
        loadUsers();
    }

    // ─────────────────────────── TAB SWITCHING ───────────────────────────

    @FXML void switchToAuctions(ActionEvent e) { switchPanel(panelAuctions, panelUsers, tabAuction, tabUser); }
    @FXML void switchToUsers(ActionEvent e)    { switchPanel(panelUsers, panelAuctions, tabUser, tabAuction); }

    // ─────────────────────────── AUCTION FILTERS ───────────────────────────

    @FXML void filterAuctionAll(ActionEvent e)     { applyAuctionFilter(dto -> true, aFilterAll); }
    @FXML void filterAuctionRunning(ActionEvent e) { applyAuctionFilter(dto -> dto.getStatus() == AuctionStatus.RUNNING, aFilterRunning); }
    @FXML void filterAuctionOpen(ActionEvent e)    { applyAuctionFilter(dto -> dto.getStatus() == AuctionStatus.OPEN, aFilterOpen); }
    @FXML void filterAuctionDone(ActionEvent e) {
        applyAuctionFilter(dto -> dto.getStatus() == AuctionStatus.FINISHED
                                || dto.getStatus() == AuctionStatus.CANCELED
                                || dto.getStatus() == AuctionStatus.PAID,
                           aFilterDone);
    }

    // ─────────────────────────── USER FILTERS ───────────────────────────

    @FXML void filterUserAll(ActionEvent e)     { applyUserFilter(null, uFilterAll); }
    @FXML void filterUserSeller(ActionEvent e)  { applyUserFilter(UserRole.SELLER, uFilterSeller); }
    @FXML void filterUserBidder(ActionEvent e)  { applyUserFilter(UserRole.BIDDER, uFilterBidder); }

    // ─────────────────────────── TABLE SETUP ───────────────────────────

    private void setupAuctionTable() {
        aColName.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getAuctionName()));
        aColSeller.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSellerName()));
        aColPrice.setCellValueFactory(c  -> new SimpleStringProperty(formatVnd(c.getValue().getCurrentPrice())));
        aColEnd.setCellValueFactory(c    -> {
            LocalDateTime end = c.getValue().getEndTime();
            return new SimpleStringProperty(end != null ? end.format(DT_FMT) : "—");
        });

        // Status — badge có màu
        aColStatus.setCellValueFactory(c -> new SimpleStringProperty(auctionStatusLabel(c.getValue().getStatus())));
        aColStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (emptyCellOrNoItem(empty, getTableRow())) { setGraphic(null); return; }
                AuctionStatus st = getTableRow().getItem().getStatus();
                badge.setText(auctionStatusLabel(st));
                badge.getStyleClass().setAll(auctionBadgeStyle(st));
                setGraphic(badge);
                setText(null);
            }
        });

        // Action — nút Cancel, chỉ enable khi OPEN hoặc RUNNING
        aColAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnCancel = new Button("Cancel");
            private final HBox   box       = new HBox(btnCancel);
            {
                btnCancel.getStyleClass().add("btn-cancel-row");
                btnCancel.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx < 0 || idx >= getTableView().getItems().size()) return; // guard đúng chiều
                    handleCancelAuction(getTableView().getItems().get(idx));
                });
                box.setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (emptyCellOrNoItem(empty, getTableRow())) { setGraphic(null); setText(null); return; }
                AuctionStatus status = getTableRow().getItem().getStatus();
                btnCancel.setDisable(status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING);
                setPadding(Insets.EMPTY);
                setGraphic(box);
                setText(null);
            }
        });
    }

    private void setupUserTable() {
        uColUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        uColEmail.setCellValueFactory(c    -> new SimpleStringProperty(
            c.getValue().getEmail() != null ? c.getValue().getEmail() : "—"));
        uColAccountBalance.setCellValueFactory(c -> new SimpleStringProperty(formatVnd(c.getValue().getAccountBalance())));

        // Role — badge có màu theo role
        uColRole.setCellValueFactory(c -> {
            UserRole role = c.getValue().getRole();
            return new SimpleStringProperty(role != null ? role.name() : "—");
        });
        uColRole.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (emptyCellOrNoItem(empty, getTableRow())) { setGraphic(null); return; }
                UserRole role = getTableRow().getItem().getRole();
                if (role == null) { setText("—"); setGraphic(null); return; }
                badge.setText(role.name());
                badge.getStyleClass().setAll(role == UserRole.SELLER ? "badge-seller" : "badge-bidder");
                setGraphic(badge);
                setText(null);
            }
        });

        // Action — placeholder (chưa có thêm thao tác)
        uColAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setText(empty ? null : "—");
            }
        });
    }

    // ─────────────────────────── DATA LOADING ───────────────────────────

    private void loadMetrics() {
        fetchAsync("ADMIN_GET_METRICS", null, response -> {
            Type type = new TypeToken<ApiResponse<AdminMetrics>>() {}.getType();
            ApiResponse<AdminMetrics> apiResp = JsonUtils.fromJsonGeneric(response, type);
            if (apiResp == null || !apiResp.isSuccess() || apiResp.getData() == null) return;

            AdminMetrics m = JsonUtils.fromJson(JsonUtils.toJson(apiResp.getData()), AdminMetrics.class);
            if (m == null) return;

            Platform.runLater(() -> {
                lblMetricRunning.setText(String.valueOf(m.getRunningCount()));
                lblMetricEnded.setText(String.valueOf(m.getEndedCount()));
                lblMetricUsers.setText(String.valueOf(m.getTotalUsers()));
            });
        });
    }

    private void loadAuctions() {
        fetchList("ADMIN_GET_AUCTIONS", 
            new TypeToken<ApiResponse<List<AdminDisplayDTO>>>() {}.getType(),
            new TypeToken<List<AdminDisplayDTO>>() {},
            items -> Platform.runLater(() -> auctionMaster.setAll(items)));
    }

    private void loadUsers() {
        fetchList("ADMIN_GET_USERS",
            new TypeToken<ApiResponse<List<UserDTO>>>() {}.getType(),
            new TypeToken<List<UserDTO>>() {},
            items -> Platform.runLater(() -> userMaster.setAll(items)));
    }

    // ─────────────────────────── CANCEL AUCTION ───────────────────────────

    private void handleCancelAuction(AdminDisplayDTO dto) {
        TextArea taReason = new TextArea();
        taReason.setPromptText("Enter reason to cancel auction...");
        taReason.setWrapText(true);
        taReason.setPrefRowCount(3);
        taReason.setPrefWidth(360);

        Label lbl = new Label("Cancel reason (required):");
        lbl.setStyle("-fx-font-size: 13px; -fx-padding: 0 0 6 0;");
        VBox content = new VBox(6, lbl, taReason);
        content.setPadding(new Insets(10, 0, 0, 0));

        Alert confirm = buildAlert(Alert.AlertType.CONFIRMATION,
            "Cancel Auction",
            "You are going to cancel: \"" + dto.getAuctionName() + "\"");
        confirm.getDialogPane().setContent(content);
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String reason = taReason.getText().trim();
        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cancel reason cannot be empty.");
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("auctionId", dto.getAuctionId());
        payload.put("reason", reason);

        fetchAsync("ADMIN_CANCEL_AUCTION", payload, response -> {
            ApiResponse<?> apiResp = JsonUtils.fromJson(response, ApiResponse.class);
            Platform.runLater(() -> {
                if (apiResp != null && apiResp.isSuccess()) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Auction cancelled successfully.");
                    // Cập nhật local — không cần reload toàn bộ list
                    auctionMaster.stream()
                        .filter(a -> a.getAuctionId().equals(dto.getAuctionId()))
                        .findFirst()
                        .ifPresent(a -> a.setStatus(AuctionStatus.CANCELED));
                    tblAuctions.refresh();
                    loadMetrics();
                } else {
                    String errMsg = (apiResp != null) ? apiResp.getMessage() : "Unexpected error.";
                    showAlert(Alert.AlertType.ERROR, "Failed", "Cancel failed: " + errMsg);
                }
            });
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS — tất cả logic tái sử dụng nằm dưới đây
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Gửi request socket trên background thread, trả về innerJson của ApiResponse data
     * vào callback. Callback chạy trên background thread — gọi Platform.runLater nếu cần UI.
     */
    private void fetchAsync(String action, Object payload, Consumer<String> onSuccess) {
        new Thread(() -> {
            try {
                String req  = JsonUtils.toJson(ClientMessage.request(action, payload));
                String resp = socket.sendAndReceive(req);
                if (resp == null) return;

                ClientMessage msg = JsonUtils.fromJson(resp, ClientMessage.class);
                if (msg == null || msg.getData() == null) return;

                onSuccess.accept(JsonUtils.toJson(msg.getData()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Variant của fetchAsync dành riêng cho response dạng ApiResponse<List<T>>.
     * Parse xong gọi onSuccess với List<T> đã giải tuần tự hoàn chỉnh.
     */
    private <T> void fetchList(String action, Type apiType, TypeToken<List<T>> listToken, Consumer<List<T>> onSuccess) {
        fetchAsync(action, null, innerJson -> {
            ApiResponse<List<T>> apiResp = JsonUtils.fromJsonGeneric(innerJson, apiType);
            if (apiResp == null || !apiResp.isSuccess() || apiResp.getData() == null) {
                onSuccess.accept(new ArrayList<>());
                return;
            }
            List<T> list = JsonUtils.fromJsonGeneric(JsonUtils.toJson(apiResp.getData()), listToken.getType());
            onSuccess.accept(list != null ? list : new ArrayList<>());
        });
    }

    /**
     * Hiển thị/ẩn hai panel khi chuyển tab.
     * show: panel cần hiện; hide: panel cần ẩn.
     */
    private void switchPanel(VBox show, VBox hide, ToggleButton activeTab, ToggleButton inactiveTab) {
        show.setVisible(true);
        show.setManaged(true);
        hide.setVisible(false);
        hide.setManaged(false);
        activeTab.setSelected(true);
        inactiveTab.setSelected(false);
    }

    /**
     * Đặt predicate filter cho auctionFiltered và highlight tab tương ứng.
     */
    private void applyAuctionFilter(Predicate<AdminDisplayDTO> pred, ToggleButton activeBtn) {
        auctionFiltered.setPredicate(pred);
        setActiveToggleGroup(activeBtn, aFilterAll, aFilterRunning, aFilterOpen, aFilterDone);
    }

    /**
     * Đặt predicate filter cho userFiltered theo role (null = tất cả) và highlight tab.
     */
    private void applyUserFilter(UserRole role, ToggleButton activeBtn) {
        userFiltered.setPredicate(u -> role == null || u.getRole() == role);
        setActiveToggleGroup(activeBtn, uFilterAll, uFilterSeller, uFilterBidder);
    }

    /**
     * Đặt đúng một ToggleButton là selected, còn lại deselect.
     * Tránh việc phải liệt kê từng button.setSelected() thủ công.
     */
    private void setActiveToggleGroup(ToggleButton active, ToggleButton... group) {
        for (ToggleButton btn : group) btn.setSelected(btn == active);
    }

    /** Guard kiểm tra cell rỗng, dùng chung cho cả hai bảng. */
    private boolean emptyCellOrNoItem(boolean empty, javafx.scene.control.TableRow<?> row) {
        return empty || row == null || row.getItem() == null;
    }

    /** Format số tiền VNĐ dùng chung. */
    private String formatVnd(long amount) {
        return String.format("%,d ₫", amount);
    }

    /** Tạo Alert với title và header sẵn. */
    private Alert buildAlert(Alert.AlertType type, String title, String header) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(header);
        return a;
    }

    /** Tạo và hiển thị Alert đơn giản (không có header). */
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = buildAlert(type, title, null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ─────────────────────────── LABEL/STYLE MAPPINGS ───────────────────────────

    private String auctionStatusLabel(AuctionStatus s) {
        if (s == null) return "—";
        return switch (s) {
            case OPEN     -> "Open";
            case RUNNING  -> "Running";
            case FINISHED -> "Finished";
            case PAID     -> "Paid";
            case CANCELED -> "Canceled";
        };
    }

    private String auctionBadgeStyle(AuctionStatus s) {
        if (s == null) return "";
        return switch (s) {
            case RUNNING  -> "badge-running";
            case OPEN     -> "badge-open";
            case FINISHED -> "badge-finished";
            case PAID     -> "badge-paid";
            case CANCELED -> "badge-canceled";
        };
    }
}
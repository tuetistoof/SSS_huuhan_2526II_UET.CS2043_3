package com.ssscloud.auction.client.controller.admin;

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
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.response.DTO.AdminDisplayDTO;
import com.ssscloud.auction.common.payload.response.DTO.UserDTO;
import com.ssscloud.auction.common.payload.response.request.AdminMetrics;
import com.ssscloud.auction.common.payload.response.request.ApiResponse;
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
import javafx.scene.control.TableRow;
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

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();

    private final ObservableList<AdminDisplayDTO> auctionMaster   = FXCollections.observableArrayList();
    private final FilteredList<AdminDisplayDTO>   auctionFiltered = new FilteredList<>(auctionMaster, p -> true);
    private final ObservableList<UserDTO>         userMaster      = FXCollections.observableArrayList();
    private final FilteredList<UserDTO>           userFiltered    = new FilteredList<>(userMaster, p -> true);

    private Consumer<AdminDisplayDTO> onOpenBidRoom;

    public void setOnOpenBidRoom(Consumer<AdminDisplayDTO> callback) {
        this.onOpenBidRoom = callback;
    }

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

    @FXML void switchToAuctions(ActionEvent event) { 
        switchPanel(panelAuctions, panelUsers, tabAuction, tabUser); 
    }
    @FXML void switchToUsers(ActionEvent event)    { 
        switchPanel(panelUsers, panelAuctions, tabUser, tabAuction); 
    }


    @FXML void filterAuctionAll(ActionEvent event)     { 
        applyAuctionFilter(dto -> true, aFilterAll); 
    }
    @FXML void filterAuctionRunning(ActionEvent event) { 
        applyAuctionFilter(dto -> dto.getStatus() == AuctionStatus.RUNNING, aFilterRunning); 
    }
    @FXML void filterAuctionOpen(ActionEvent event)    { 
        applyAuctionFilter(dto -> dto.getStatus() == AuctionStatus.OPEN, aFilterOpen); 
    }
    @FXML void filterAuctionDone(ActionEvent e) {
        applyAuctionFilter(dto -> dto.getStatus() == AuctionStatus.FINISHED
                                || dto.getStatus() == AuctionStatus.CANCELED
                                || dto.getStatus() == AuctionStatus.PAID,
                           aFilterDone);
    }


    @FXML void filterUserAll(ActionEvent event)     { 
        applyUserFilter(null, uFilterAll); 
    }
    @FXML void filterUserSeller(ActionEvent event)  { 
        applyUserFilter(UserRole.SELLER, uFilterSeller); 
    }
    @FXML void filterUserBidder(ActionEvent event)  { 
        applyUserFilter(UserRole.BIDDER, uFilterBidder); 
    }

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
                if (emptyCellOrNoItem(empty, getTableRow())) { 
                    setGraphic(null); return; 
                }
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
                    if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    handleCancelAuction(getTableView().getItems().get(idx));
                });
                box.setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (emptyCellOrNoItem(empty, getTableRow())) { 
                    setGraphic(null); setText(null); return; 
                }
                AuctionStatus status = getTableRow().getItem().getStatus();
                btnCancel.setDisable(status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING);
                setPadding(Insets.EMPTY);
                setGraphic(box);
                setText(null);
            }
        });

        tblAuctions.setRowFactory(tv -> {
            TableRow<AdminDisplayDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    if (onOpenBidRoom != null) {
                        onOpenBidRoom.accept(row.getItem());
                    }
                }
            });
            return row;
        });
            
    }

    private void setupUserTable() {
        uColUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        uColEmail.setCellValueFactory(c    -> new SimpleStringProperty(
            c.getValue().getEmail() != null ? c.getValue().getEmail() : "—"));
        uColAccountBalance.setCellValueFactory(c -> {
            uColAccountBalance.setStyle("-fx-alignment: CENTER_LEFT;");
            return new SimpleStringProperty(formatVnd(c.getValue().getAccountBalance()));
        });

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
                if (emptyCellOrNoItem(empty, getTableRow())) { 
                    setGraphic(null); return; 
                }
                UserRole role = getTableRow().getItem().getRole();
                if (role == null) { 
                    setText("-"); setGraphic(null); return; 
                }
                badge.setText(role.name());
                String badgeStyle = switch (role) {
                    case SELLER -> "badge-seller";
                    case BIDDER -> "badge-bidder";
                    case ADMIN  -> "badge-admin";
                };
                badge.getStyleClass().setAll(badgeStyle);
                setGraphic(badge);
                setText(null);
            }
        });
    }

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

    private void fetchAsync(String action, Object payload, Consumer<String> onSuccess) {
        String req = JsonUtils.toJson(ClientMessage.request(action, payload));
        dispatcher.request(req, raw -> {
            try {
                ClientMessage msg = JsonUtils.fromJson(raw, com.ssscloud.auction.common.payload.ClientMessage.class);
                if (msg != null && msg.getData() != null) {
                    onSuccess.accept(JsonUtils.toJson(msg.getData()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

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

    private void switchPanel(VBox show, VBox hide, ToggleButton activeTab, ToggleButton inactiveTab) {
        show.setVisible(true);
        show.setManaged(true);
        hide.setVisible(false);
        hide.setManaged(false);
        activeTab.setSelected(true);
        inactiveTab.setSelected(false);
    }

    private void applyAuctionFilter(Predicate<AdminDisplayDTO> pred, ToggleButton activeBtn) {
        auctionFiltered.setPredicate(pred);
        setActiveToggleGroup(activeBtn, aFilterAll, aFilterRunning, aFilterOpen, aFilterDone);
    }

    private void applyUserFilter(UserRole role, ToggleButton activeBtn) {
        userFiltered.setPredicate(u -> role == null || u.getRole() == role);
        setActiveToggleGroup(activeBtn, uFilterAll, uFilterSeller, uFilterBidder);
    }

    private void setActiveToggleGroup(ToggleButton active, ToggleButton... group) {
        for (ToggleButton btn : group) btn.setSelected(btn == active);
    }

    private boolean emptyCellOrNoItem(boolean empty, javafx.scene.control.TableRow<?> row) {
        return empty || row == null || row.getItem() == null;
    }

    private String formatVnd(long amount) {
        return String.format("%,d VND", amount);
    }

    private Alert buildAlert(Alert.AlertType type, String title, String header) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(header);
        return a;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = buildAlert(type, title, null);
        a.setContentText(msg);
        a.showAndWait();
    }

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
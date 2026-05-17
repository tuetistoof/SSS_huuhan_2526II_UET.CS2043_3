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
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyEvent;
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
    @FXML private TableColumn<UserDTO, String> uColEmail;
    @FXML private TableColumn<UserDTO, String> uColAccountBalance;
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

    // ── Search ──
    @FXML private TextField txtAuctionSearch;
    @FXML private TextField txtUserSearch;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();

    private final ObservableList<AdminDisplayDTO> auctionMaster   = FXCollections.observableArrayList();
    private final FilteredList<AdminDisplayDTO>   auctionFiltered = new FilteredList<>(auctionMaster, p -> true);
    private final ObservableList<UserDTO>         userMaster      = FXCollections.observableArrayList();
    private final FilteredList<UserDTO>           userFiltered    = new FilteredList<>(userMaster, p -> true);

    // Auction filter state — kết hợp với search
    private Predicate<AdminDisplayDTO> currentAuctionStatusPred = dto -> true;
    private UserRole currentUserRoleFilter = null;

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

    @FXML
    void switchToAuctions(ActionEvent event) {
        panelAuctions.setVisible(true);
        panelAuctions.setManaged(true);
        panelUsers.setVisible(false);
        panelUsers.setManaged(false);
        tabAuction.setSelected(true);
        tabUser.setSelected(false);
    }

    @FXML
    void switchToUsers(ActionEvent event) {
        panelAuctions.setVisible(false);
        panelAuctions.setManaged(false);
        panelUsers.setVisible(true);
        panelUsers.setManaged(true);
        tabAuction.setSelected(false);
        tabUser.setSelected(true);
    }

    // ─────────────────────────── AUCTION FILTERS ───────────────────────────

    @FXML
    void filterAuctionAll(ActionEvent event) {
        currentAuctionStatusPred = dto -> true;
        applyAuctionFilter();
        setActiveAuctionTab(aFilterAll);
    }

    @FXML
    void filterAuctionRunning(ActionEvent event) {
        currentAuctionStatusPred = dto -> dto.getStatus() == AuctionStatus.RUNNING;
        applyAuctionFilter();
        setActiveAuctionTab(aFilterRunning);
    }

    @FXML
    void filterAuctionOpen(ActionEvent event) {
        currentAuctionStatusPred = dto -> dto.getStatus() == AuctionStatus.OPEN;
        applyAuctionFilter();
        setActiveAuctionTab(aFilterOpen);
    }

    @FXML
    void filterAuctionDone(ActionEvent event) {
        currentAuctionStatusPred = dto ->
            dto.getStatus() == AuctionStatus.FINISHED
            || dto.getStatus() == AuctionStatus.CANCELED
            || dto.getStatus() == AuctionStatus.PAID;
        applyAuctionFilter();
        setActiveAuctionTab(aFilterDone);
    }

    // ─────────────────────────── USER FILTERS ───────────────────────────

    @FXML
    void filterUserAll(ActionEvent event) {
        currentUserRoleFilter = null;
        applyUserFilter();
        setActiveUserTab(uFilterAll);
    }

    @FXML
    void filterUserSeller(ActionEvent event) {
        currentUserRoleFilter = UserRole.SELLER;
        applyUserFilter();
        setActiveUserTab(uFilterSeller);
    }

    @FXML
    void filterUserBidder(ActionEvent event) {
        currentUserRoleFilter = UserRole.BIDDER;
        applyUserFilter();
        setActiveUserTab(uFilterBidder);
    }

    @FXML
    void searchAuctions(KeyEvent event) {
        applyAuctionFilter();
    }

    @FXML
    void searchUsers(KeyEvent event) {
        applyUserFilter();
    }

    // ─────────────────────────── TABLE SETUP ───────────────────────────

    private void setupAuctionTable() {
        aColName.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getAuctionName()));

        aColSeller.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSellerName()));

        aColPrice.setCellValueFactory(c ->
            new SimpleStringProperty(String.format("%,d ₫", c.getValue().getCurrentPrice())));

        aColStatus.setCellValueFactory(c ->
            new SimpleStringProperty(auctionStatusLabel(c.getValue().getStatus())));

        aColStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                AuctionStatus st = getTableRow().getItem().getStatus();
                badge.setText(auctionStatusLabel(st));
                badge.getStyleClass().setAll(auctionBadgeStyle(st));
                setGraphic(badge);
                setText(null);
            }
        });

        aColEnd.setCellValueFactory(c -> {
            LocalDateTime end = c.getValue().getEndTime();
            return new SimpleStringProperty(end != null ? end.format(DT_FMT) : "—");
        });

        aColAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnCancel = new Button("Cancel");
            private final HBox   box       = new HBox(btnCancel);
            {
                btnCancel.getStyleClass().add("btn-cancel-row");
                btnCancel.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) return;
                    handleCancelAuction(getTableView().getItems().get(idx));
                });
                box.setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                AuctionStatus status = getTableRow().getItem().getStatus();
                btnCancel.setDisable(status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING);
                setPadding(Insets.EMPTY);
                setGraphic(box);
                setText(null);
            }
        });
    }

    private void setupUserTable() {
        uColUsername.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getUsername()));

        uColEmail.setCellValueFactory(c -> {
            String email = c.getValue().getEmail();
            return new SimpleStringProperty(email != null ? email : "—");
        });

        uColRole.setCellValueFactory(c -> {
            UserRole role = c.getValue().getRole();
            return new SimpleStringProperty(role != null ? role.name() : "—");
        });

        uColRole.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                UserRole role = getTableRow().getItem().getRole();
                if (role == null) { setText("—"); setGraphic(null); return; }
                badge.setText(role.name());
                badge.getStyleClass().add((role == UserRole.SELLER) ? "badge-seller" : "badge-bidder");
                setGraphic(badge);
                setText(null);
            }
        });

        uColAccountBalance.setCellValueFactory(c ->
            new SimpleStringProperty(String.format("%,d ₫", c.getValue().getAccountBalance())));

        uColAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null); setText(empty ? null : "—");
            }
        });
    }

    // ─────────────────────────── DATA LOADING ───────────────────────────

    private void loadMetrics() {
        new Thread(() -> {
            try {
                String requestJson  = JsonUtils.toJson(ClientMessage.request("ADMIN_GET_METRICS", null));
                String responseJson = socket.sendAndReceive(requestJson);
                if (responseJson == null) return;

                ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                if (serverMsg == null || serverMsg.getData() == null) return;

                String innerJson = JsonUtils.toJson(serverMsg.getData());
                Type type = new TypeToken<ApiResponse<AdminMetrics>>() {}.getType();
                ApiResponse<AdminMetrics> apiResp = JsonUtils.fromJsonGeneric(innerJson, type);
                if (apiResp == null || !apiResp.isSuccess() || apiResp.getData() == null) return;

                AdminMetrics metrics = JsonUtils.fromJson(JsonUtils.toJson(apiResp.getData()), AdminMetrics.class);
                if (metrics == null) return;

                Platform.runLater(() -> {
                    lblMetricRunning.setText(String.valueOf(metrics.getRunningCount()));
                    lblMetricEnded.setText(String.valueOf(metrics.getEndedCount()));
                    lblMetricUsers.setText(String.valueOf(metrics.getTotalUsers()));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadAuctions() {
        new Thread(() -> {
            try {
                String requestJson  = JsonUtils.toJson(ClientMessage.request("ADMIN_GET_AUCTIONS", null));
                String responseJson = socket.sendAndReceive(requestJson);
                if (responseJson == null) { Platform.runLater(() -> auctionMaster.clear()); return; }

                ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                if (serverMsg == null || serverMsg.getData() == null) { Platform.runLater(() -> auctionMaster.clear()); return; }

                String innerJson = JsonUtils.toJson(serverMsg.getData());
                Type apiType = new TypeToken<ApiResponse<List<AdminDisplayDTO>>>() {}.getType();
                ApiResponse<List<AdminDisplayDTO>> apiResp = JsonUtils.fromJsonGeneric(innerJson, apiType);

                if (apiResp == null || !apiResp.isSuccess() || apiResp.getData() == null) {
                    Platform.runLater(() -> auctionMaster.clear()); return;
                }

                String listJson   = JsonUtils.toJson(apiResp.getData());
                Type   listType   = new TypeToken<List<AdminDisplayDTO>>() {}.getType();
                List<AdminDisplayDTO> list = JsonUtils.fromJsonGeneric(listJson, listType);
                Platform.runLater(() -> auctionMaster.setAll(list != null ? list : new ArrayList<>()));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> auctionMaster.clear());
            }
        }).start();
    }

    private void loadUsers() {
        new Thread(() -> {
            try {
                String requestJson  = JsonUtils.toJson(ClientMessage.request("ADMIN_GET_USERS", null));
                String responseJson = socket.sendAndReceive(requestJson);
                if (responseJson == null) { Platform.runLater(() -> userMaster.clear()); return; }

                ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                if (serverMsg == null || serverMsg.getData() == null) { Platform.runLater(() -> userMaster.clear()); return; }

                String innerJson = JsonUtils.toJson(serverMsg.getData());
                Type apiType = new TypeToken<ApiResponse<List<UserDTO>>>() {}.getType();
                ApiResponse<List<UserDTO>> apiResp = JsonUtils.fromJsonGeneric(innerJson, apiType);

                if (apiResp == null || !apiResp.isSuccess() || apiResp.getData() == null) {
                    Platform.runLater(() -> userMaster.clear()); return;
                }

                String listJson = JsonUtils.toJson(apiResp.getData());
                Type listType   = new TypeToken<List<UserDTO>>() {}.getType();
                List<UserDTO> list = JsonUtils.fromJsonGeneric(listJson, listType);
                Platform.runLater(() -> userMaster.setAll(list != null ? list : new ArrayList<>()));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> userMaster.clear());
            }
        }).start();
    }

    // ─────────────────────────── CANCEL AUCTION ───────────────────────────

    private void handleCancelAuction(AdminDisplayDTO dto) {
        TextArea taReason = new TextArea();
        taReason.setPromptText("Enter reason to cancle auction...");
        taReason.setWrapText(true);
        taReason.setPrefRowCount(3);
        taReason.setPrefWidth(360);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Auction");
        confirm.setHeaderText("You are going to cancel: \"" + dto.getAuctionName() + "\"");
        confirm.setContentText(null);

        Label lbl = new Label("Cancle reason (Eequired):");
        lbl.setStyle("-fx-font-size: 13px; -fx-padding: 0 0 6 0;");
        VBox content = new VBox(6, lbl, taReason);
        content.setPadding(new Insets(10, 0, 0, 0));
        confirm.getDialogPane().setContent(content);
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String reason = taReason.getText().trim();
        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Reason to cancel needed to be filled.");
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("auctionId", dto.getAuctionId());
        payload.put("reason", reason);

        new Thread(() -> {
            try {
                String req  = JsonUtils.toJson(ClientMessage.request("ADMIN_CANCEL_AUCTION", payload));
                String resp = socket.sendAndReceive(req);

                if (resp == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "No response from server."));
                    return;
                }

                ClientMessage msg = JsonUtils.fromJson(resp, ClientMessage.class);
                String innerJson  = (msg != null && msg.getData() != null) ? JsonUtils.toJson(msg.getData()) : "{}";
                ApiResponse<?> apiResp = JsonUtils.fromJson(innerJson, ApiResponse.class);

                Platform.runLater(() -> {
                    if (apiResp != null && apiResp.isSuccess()) {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Cancel auction successfully.");
                        // Cập nhật status local — tránh reload toàn bộ list
                        auctionMaster.stream()
                            .filter(a -> a.getAuctionId().equals(dto.getAuctionId()))
                            .findFirst()
                            .ifPresent(a -> a.setStatus(AuctionStatus.CANCELED));
                        tblAuctions.refresh();
                        loadMetrics(); // Refresh metric cards
                    } else {
                        String errMsg = (apiResp != null) ? apiResp.getMessage() : "Unexpected Error.";
                        showAlert(Alert.AlertType.ERROR, "Failed", "Failed to cancel: " + errMsg);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Connection Error", e.getMessage()));
            }
        }).start();
    }

    // ─────────────────────────── FILTER HELPERS ───────────────────────────

    private void applyAuctionFilter() {
        String keyword = txtAuctionSearch.getText().toLowerCase().trim();
        Predicate<AdminDisplayDTO> statusPred = currentAuctionStatusPred;
        auctionFiltered.setPredicate(dto -> {
            boolean statusMatch = statusPred.test(dto);
            boolean searchMatch = keyword.isEmpty()
                || dto.getAuctionName().toLowerCase().contains(keyword)
                || dto.getSellerName().toLowerCase().contains(keyword);
            return statusMatch && searchMatch;
        });
    }

    private void applyUserFilter() {
        String keyword      = txtUserSearch.getText().toLowerCase().trim();
        UserRole roleFilter = currentUserRoleFilter;
        userFiltered.setPredicate(u -> {
            boolean roleMatch   = (roleFilter == null) || (u.getRole() == roleFilter);
            boolean searchMatch = keyword.isEmpty()
                || u.getUsername().toLowerCase().contains(keyword)
                || (u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword));
            return roleMatch && searchMatch;
        });
    }

    // ─────────────────────────── UI HELPERS ───────────────────────────

    private void setActiveAuctionTab(ToggleButton active) {
        aFilterAll.setSelected(active == aFilterAll);
        aFilterRunning.setSelected(active == aFilterRunning);
        aFilterOpen.setSelected(active == aFilterOpen);
        aFilterDone.setSelected(active == aFilterDone);
    }

    private void setActiveUserTab(ToggleButton active) {
        uFilterAll.setSelected(active == uFilterAll);
        uFilterSeller.setSelected(active == uFilterSeller);
        uFilterBidder.setSelected(active == uFilterBidder);
    }

    private String auctionStatusLabel(AuctionStatus s) {
        if (s == null) return "—";
        return switch (s) {
            case OPEN     -> "Open";
            case RUNNING  -> "Running";
            case FINISHED -> "Finished";
            case PAID     -> "Paid";
            case CANCELED -> "Cancel";
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

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
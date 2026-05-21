package com.ssscloud.auction.client.controller.seller;

import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.client.util.SessionManager;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.SellerDisplayDTO;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/**
 * Flow call back:
 *  SETUP: handleNavDashboard (Mainlayout lấy loader (source fxml) để trả về
 *      Parent view = loader.load(); và ctrl = loader.getController
 *  TRIGGER:
 *      colAction, chạy setOpenBidRoom.accecpt(dto) tham số là consumer<DTO> nuốt thông tin và đưa ra ngoài
 *      dashboard khôg biết BiddingRoom tồn tại, chỉ đưa data ra ngoài.
 * LOAD:
 *      mainlayout gọi ctrl.setOnBidRoom(this::loadBiddingRoomAsSeller)
 *      :: là ký hiệu "lấy hàm này ra nhưng chưa gọi". Kết quả là một Consumer<SellerDisplayDTO> được lưu vào SellerDashboardController
 *      Sau này khi dashboard gọi onOpenBidRoom.accept(dto), Java tự động điền dto vào chỗ tham số và gọi mainLayoutInstance.loadBiddingRoomAsSeller(dto).
 *  BACK:
 *  onSuccessCallback.run(), cái này được MainLayout set thành () -> handleNavDashboard(null), nên quay về dashboard (method này trong loadBiddingRoomAs)
 */

public class SellerDashboardController {

    @FXML private Label lblRunningCount;   
    @FXML private Label lblFinishedCount; 
    @FXML private Label lblAccountBalance;
    @FXML private Label lblBankAccount; 

    @FXML private ToggleButton tabAll;
    @FXML private ToggleButton tabRunning;
    @FXML private ToggleButton tabOpen;
    @FXML private ToggleButton tabDone;

    @FXML private TableView<SellerDisplayDTO>         tblAuctions;
    @FXML private TableColumn<SellerDisplayDTO, String> colTitle;
    @FXML private TableColumn<SellerDisplayDTO, String> colStartingPrice;
    @FXML private TableColumn<SellerDisplayDTO, String> colCurrentPrice;
    @FXML private TableColumn<SellerDisplayDTO, String> colBidCount;
    @FXML private TableColumn<SellerDisplayDTO, String> colTimeLeft;
    @FXML private TableColumn<SellerDisplayDTO, String> colStatus;
    @FXML private TableColumn<SellerDisplayDTO, Void>   colActions;

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();
    private final SessionManager      session = SessionManager.getInstance();

    private final ObservableList<SellerDisplayDTO> masterList   = FXCollections.observableArrayList();
    private final FilteredList<SellerDisplayDTO>   filteredList = new FilteredList<>(masterList, p -> true);

    private Consumer<SellerDisplayDTO> onOpenBidRoom;

    @FXML
    public void initialize() {
        setupTable();
        tblAuctions.setItems(filteredList);
        populateAccountInfo();
        loadMyAuctions();
    }


    private void setupTable() {
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemName())); // bọc trong Observable lamde expr để tự động cập nhật

        colStartingPrice.setCellValueFactory(c -> new SimpleStringProperty(
            String.format("%,d ₫", c.getValue().getStartPrice())));

        colCurrentPrice.setCellValueFactory(c -> new SimpleStringProperty(
            String.format("%,d ₫", c.getValue().getCurrentPrice())));

        colBidCount.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getBidCount())));

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

        // Trạng thái — badge màu (cần cả cellValueFactory và cellFactory)
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(statusLabel(c.getValue().getStatus())));
 
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                AuctionStatus st = getTableRow().getItem().getStatus();
                badge.setText(statusLabel(st));
                badge.getStyleClass().setAll(badgeStyleClass(st));
                setGraphic(badge);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("View room");
            private final HBox container = new HBox(btnView);
            {
                btnView.getStyleClass().add("btn-view-row");
                container.setAlignment(Pos.CENTER);
                setGraphic(container);
                btnView.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        if (onOpenBidRoom != null)
                            onOpenBidRoom.accept(getTableView().getItems().get(idx));
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
        
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setPadding(Insets.EMPTY);
                    setAlignment(Pos.CENTER);
                    setGraphic(container);
                    setText(null); 
                }
            }
        });
    }


    public void loadMyAuctions() {
        if (session.getCurrentUser() == null) {
            return;
        }
 
        String json = JsonUtils.toJson(ClientMessage.request("GET_MY_AUCTIONS", null));

        dispatcher.request(json, raw -> {
            List<SellerDisplayDTO> items = ServerResponse.unwrapList(raw, null, SellerDisplayDTO.class);
            if (items == null) items = new ArrayList<>();
            refreshMetrics(items);
            masterList.setAll(items);
        });
    }
    
    private void populateAccountInfo() {
        UserDTO user = session.getCurrentUser();
        if (user == null) return;
        lblAccountBalance.setText(String.format("%,d ₫", user.getAccountBalance()));
        lblBankAccount.setText("—");
    }

    private void refreshMetrics(List<SellerDisplayDTO> items) {
        int running  = 0;
        int finished = 0;

        for (SellerDisplayDTO dto : items) {
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
        filteredList.setPredicate(dto -> dto.getStatus() == AuctionStatus.RUNNING);
        setActiveTab(tabRunning);
    }

    @FXML
    private void filterOpen() {
        filteredList.setPredicate(dto -> dto.getStatus() == AuctionStatus.OPEN);
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

    }

    @FXML
    private void openCreateDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-auction.fxml"));
            Parent root = loader.load();

            CreateAuctionController ctrl = loader.getController();
            ctrl.setOnSuccessCallback((Consumer<AuctionDTO>) newAuction -> loadMyAuctions());

            Stage modal = new Stage();
            modal.setTitle("Creat a new auction");
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(tblAuctions.getScene().getWindow());
            modal.setScene(new Scene(root));
            modal.showAndWait();

        } catch (IOException e) {
            System.err.println("[SellerDashboard] openCreateDialog error: " + e.getMessage());
        }
    }


    public void setOnOpenBidRoom(Consumer<SellerDisplayDTO> callback) {
        this.onOpenBidRoom = callback;
    }

    private String badgeStyleClass(AuctionStatus s) {
        if (s == null) return "badge-canceled";
        return switch (s) {
            case RUNNING          -> "badge-running";
            case OPEN             -> "badge-open";
            case FINISHED, PAID   -> "badge-finished";
            case CANCELED         -> "badge-canceled";
        };
    }


    private String statusLabel(AuctionStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case OPEN     -> "OPEN";
            case RUNNING  -> "RUNNING";
            case FINISHED -> "ENDED";
            case PAID     -> "PAID";
            case CANCELED -> "CANCELED";
        };
    }
}
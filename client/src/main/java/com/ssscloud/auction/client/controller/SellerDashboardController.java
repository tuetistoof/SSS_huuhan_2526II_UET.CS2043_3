package com.ssscloud.auction.client.controller;
 
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
 
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
 
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
 
public class SellerDashboardController implements Initializable {
 
    // ── Topbar ──
    @FXML private Label lblShopName;
    @FXML private Label lblAvatar;
 
    // ── Metrics ──
    @FXML private Label lblRunning;
    @FXML private Label lblRunningToday;
    @FXML private Label lblTotal;
    @FXML private Label lblBidCount;
    @FXML private Label lblBidToday;
    @FXML private Label lblRevenue;
 
    // ── Tabs ──
    @FXML private ToggleButton tabAll;
    @FXML private ToggleButton tabRunning;
    @FXML private ToggleButton tabOpen;
    @FXML private ToggleButton tabDone;
 
    // ── Table ──
    @FXML private TableView<AuctionDTO> tblAuctions;
    @FXML private TableColumn<AuctionDTO, String> colTitle;
    @FXML private TableColumn<AuctionDTO, String> colCurrentPrice;
    @FXML private TableColumn<AuctionDTO, String> colBidCount;
    @FXML private TableColumn<AuctionDTO, String> colTimeLeft;
    @FXML private TableColumn<AuctionDTO, String> colStatus;
    @FXML private TableColumn<AuctionDTO, Void>   colActions;

    private final ObservableList<AuctionDTO> masterList = FXCollections.observableArrayList();  //chứa toàn bộ phiên dấu của seller
    private FilteredList<AuctionDTO> filteredList;  //lọc danh sách từ master list sau khi filter theo tên, loại,...
 
    private ScheduledExecutorService scheduler; //Dùng để chạy các tác vụ định kỳ (tự động refresh dsach đấu giá, ktra các phiên đóng,..)
    private UserDTO currentUser; //thông tin seller

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupTabGroup();
 
        filteredList = new FilteredList<>(masterList, a -> true);
        tblAuctions.setItems(filteredList);
 
        startCountdownTimer();
    }

    public void initData(UserDTO user, List<AuctionDTO> auctions) {
        this.currentUser = user;
 
        String name = user.getUsername();
        lblShopName.setText("Shop: " + name);

        masterList.setAll(auctions);
        //updateMetrics();
    }

    private void setupTableColumns() {
 
        // colTitle.setCellValueFactory(c ->
        //     new SimpleStringProperty(c.getValue().getTitle()));
 
        // colCurrentPrice.setCellValueFactory(c ->
        //     new SimpleStringProperty(formatPrice(c.getValue().getCurrentPrice())));
 
        // // colBidCount.setCellValueFactory(c ->
        // //     new SimpleStringProperty(c.getValue().getBidCount() + " bids"));
 
        // // Countdown — tính lại mỗi lần table refresh
        // colTimeLeft.setCellValueFactory(c -> {
        //     AuctionDTO a = c.getValue();
        //     if (a.getStatus() == AuctionStatus.RUNNING || a.getStatus() == AuctionStatus.OPEN) {
        //         return new SimpleStringProperty(formatTimeLeft(a.getEndTime()));
        //     }
        //     return new SimpleStringProperty("—");
        // });
        // colTimeLeft.setCellFactory(col -> new TableCell<>() {
        //     @Override protected void updateItem(String item, boolean empty) {
        //         super.updateItem(item, empty);
        //         if (empty || item == null) { setText(null); setGraphic(null); return; }
        //         setText(item);
        //         // Tô vàng nếu dưới 10 phút
        //         AuctionDTO a = getTableView().getItems().get(getIndex());
        //         if (a != null && a.getEndTime() != null) {
        //             long mins = ChronoUnit.MINUTES.between(LocalDateTime.now(), a.getEndTime());
        //             if (mins < 10 && mins >= 0) {
        //                 getStyleClass().removeAll("countdown-normal");
        //                 getStyleClass().add("countdown-urgent");
        //             } else {
        //                 getStyleClass().removeAll("countdown-urgent");
        //                 getStyleClass().add("countdown-normal");
        //             }
        //         }
        //     }
        // });
        // // Status badge
        // colStatus.setCellValueFactory(c ->
        //     new SimpleStringProperty(c.getValue().getStatus().name()));
        // colStatus.setCellFactory(col -> new TableCell<>() {
        //     @Override protected void updateItem(String item, boolean empty) {
        //         super.updateItem(item, empty);
        //         if (empty || item == null) { setGraphic(null); return; }
        //         Label badge = new Label(statusLabel(item));
        //         badge.getStyleClass().add(statusBadgeClass(item));
        //         setGraphic(badge);
        //         setText(null);
        //     }
        // });
        // colActions.setCellFactory(col -> new TableCell<>() {
        //     @Override protected void updateItem(Void item, boolean empty) {
        //         super.updateItem(item, empty);
        //         if (empty) { setGraphic(null); return; }
        //         AuctionDTO auction = getTableView().getItems().get(getIndex());
        //         HBox box = buildActionButtons(auction);
        //         box.setAlignment(Pos.CENTER_LEFT);
        //         setGraphic(box);
        //     }
        // });
    }
     private HBox buildActionButtons(AuctionDTO auction) {
        HBox box = new HBox(6);
        AuctionStatus status = auction.getStatus();
 
        Button btnView = new Button("Xem");
        btnView.getStyleClass().add("btn-secondary");
        btnView.setOnAction(e -> handleViewAuction(auction));
        box.getChildren().add(btnView);
 
        if (status == AuctionStatus.OPEN) {
            Button btnEdit = new Button("Sửa");
            btnEdit.getStyleClass().add("btn-secondary");
            btnEdit.setOnAction(e -> handleEditAuction(auction));
 
            Button btnDelete = new Button("Xóa");
            btnDelete.getStyleClass().add("btn-danger");
            btnDelete.setOnAction(e -> handleDeleteAuction(auction));
 
            box.getChildren().addAll(btnEdit, btnDelete);
        }
 
        return box;
    }
    private void setupTabGroup() {
        ToggleGroup group = new ToggleGroup();
        tabAll.setToggleGroup(group);
        tabRunning.setToggleGroup(group);
        tabOpen.setToggleGroup(group);
        tabDone.setToggleGroup(group);
 
        // Không cho bỏ chọn tất cả
        group.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw == null) group.selectToggle(old);
        });
    }
 
    @FXML private void filterAll()      { filteredList.setPredicate(a -> true); }
    @FXML private void filterRunning()  { filteredList.setPredicate(a -> a.getStatus() == AuctionStatus.RUNNING); }
    @FXML private void filterOpen()     { filteredList.setPredicate(a -> a.getStatus() == AuctionStatus.OPEN); }
    @FXML private void filterFinished() { filteredList.setPredicate(a ->
        a.getStatus() == AuctionStatus.FINISHED
        || a.getStatus() == AuctionStatus.PAID
        || a.getStatus() == AuctionStatus.CANCELED); }
 

    //metrics
    private void updateMetrics() {
        long running  = masterList.stream().filter(a -> a.getStatus() == AuctionStatus.RUNNING).count();
        long total    = masterList.size();
        //long bidTotal = masterList.stream().mapToLong(AuctionDTO::getBidCount).sum();
        double maxPrice = masterList.stream()
            .filter(a -> a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID)
            .mapToDouble(AuctionDTO::getCurrentPrice).max().orElse(0);
 
        lblRunning.setText(String.valueOf(running));
        lblTotal.setText(String.valueOf(total));
        // lblBidCount.setText(String.valueOf(bidTotal));
        lblRevenue.setText(maxPrice > 0 ? formatPrice(maxPrice) : "—");
    }
 

    //countdown timer
    private void startCountdownTimer() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "seller-countdown");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(
            () -> Platform.runLater(() -> tblAuctions.refresh()),
            1, 1, TimeUnit.SECONDS
        );
    }
 
    public void stopTimer() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
 

    //sidebar
    @FXML private void showDashboard()  { /* load dashboard view — đang ở đây */ }
    @FXML private void showMyAuctions() { tabAll.setSelected(true); filterAll(); }
    @FXML private void showHistory()    { tabDone.setSelected(true); filterFinished(); }
    @FXML private void showInventory()  { /* TODO: inventory screen */ }
    @FXML private void showProfile()    { /* TODO: profile screen */ }
    @FXML private void handleLogout() {
        stopTimer();
        // TODO: SceneManager.switchTo("login-signup.fxml");
    }
 
    // ── Tạo phiên đấu giá mới ──
    @FXML private void openCreateDialog() {
        // TODO: mở Dialog hoặc load CreateAuctionController
        // Dialog<ButtonType> d = new Dialog<>();
        // d.getDialogPane().setContent(FXMLLoader.load(...create-auction.fxml));
        // d.showAndWait();
    }
 
    // ────────────────────────────────────────────────
    // ROW ACTIONS
    // ────────────────────────────────────────────────
    private void handleViewAuction(AuctionDTO auction) {
        // TODO: SceneManager.switchTo("bidding-room.fxml", auction);
    }
 
    private void handleEditAuction(AuctionDTO auction) {
        // TODO: mở edit dialog với dữ liệu auction
    }
 
    private void handleDeleteAuction(AuctionDTO auction) {
        // Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
        //     "Xóa phiên \"" + auction.getTitle() + "\"?",
        //     ButtonType.YES, ButtonType.CANCEL);
        // confirm.setTitle("Xác nhận xóa");
        // confirm.showAndWait().ifPresent(btn -> {
        //     if (btn == ButtonType.YES) {
        //         // TODO: gọi server xóa, sau đó:
        //         masterList.remove(auction);
        //         updateMetrics();
        //     }
        // });
    }








    
    // ────────────────────────────────────────────────
    // HELPERS
    // ────────────────────────────────────────────────
    private String formatPrice(double price) {
        return String.format("%,.0fđ", price).replace(",", ".");
    }
 
    private String formatTimeLeft(LocalDateTime endTime) {
        if (endTime == null) return "—";
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) return "Hết giờ";
        long seconds = ChronoUnit.SECONDS.between(now, endTime);
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
 
    private String statusLabel(String status) {
        return switch (status) {
            case "RUNNING"  -> "Đang chạy";
            case "OPEN"     -> "Sắp mở";
            case "FINISHED" -> "Kết thúc";
            case "PAID"     -> "Đã thanh toán";
            case "CANCELED" -> "Đã hủy";
            default         -> status;
        };
    }
 
    private String statusBadgeClass(String status) {
        return switch (status) {
            case "RUNNING"  -> "badge-running";
            case "OPEN"     -> "badge-open";
            case "FINISHED" -> "badge-finished";
            case "PAID"     -> "badge-paid";
            case "CANCELED" -> "badge-canceled";
            default         -> "badge-finished";
        };
    }

}

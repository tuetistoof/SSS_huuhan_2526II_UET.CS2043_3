package com.ssscloud.auction.client.controller;

import com.ssscloud.auction.common.dto.request.CreateItemRequest;
import com.ssscloud.auction.common.dto.request.UpdateItemRequest;
import com.ssscloud.auction.common.dto.response.ItemDTO;
import com.ssscloud.auction.common.dto.response.ItemListResponse;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Controller quản lý items của seller
 * Hỗ trợ: tạo item, sửa item, xóa item, xem danh sách
 */
public class ItemManagementController implements Initializable {

    // ── Top Section ──
    @FXML private Label lblSellerName;
    @FXML private Button btnCreateItem;
    @FXML private Button btnRefresh;

    // ── Filter Section ──
    @FXML private ComboBox<String> cmbStatus;
    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;

    // ── Table Section ──
    @FXML private TableView<ItemDTO> tblItems;
    @FXML private TableColumn<ItemDTO, String> colId;
    @FXML private TableColumn<ItemDTO, String> colName;
    @FXML private TableColumn<ItemDTO, String> colType;
    @FXML private TableColumn<ItemDTO, Long> colPrice;
    @FXML private TableColumn<ItemDTO, String> colStatus;
    @FXML private TableColumn<ItemDTO, Void> colActions;

    // ── Detail Section ──
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailType;
    @FXML private Label lblDetailPrice;
    @FXML private Label lblDetailStatus;
    @FXML private TextArea txtDetailDescription;
    @FXML private Button btnEditItem;
    @FXML private Button btnDeleteItem;
    @FXML private Button btnStartAuction;


    private String currentUserId;
    private ItemDTO selectedItem;
    private ObservableList<ItemDTO> itemList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupStatusFilter();
        setupEventHandlers();
        loadItems();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("basePrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Add action buttons
        addActionButtonsToTable();

        tblItems.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectItem(newVal);
            }
        });
    }

    private void addActionButtonsToTable() {
        colActions.setCellFactory(col -> new TableCell<ItemDTO, Void>() {
            private final Button btnEdit = new Button("Sửa");
            private final Button btnDelete = new Button("Xóa");
            private final Button btnAuction = new Button("Đấu giá");

            {
                btnEdit.setStyle("-fx-padding: 5; -fx-font-size: 11;");
                btnDelete.setStyle("-fx-padding: 5; -fx-font-size: 11;");
                btnAuction.setStyle("-fx-padding: 5; -fx-font-size: 11;");

                btnEdit.setOnAction(e -> editItem(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteItem(getTableView().getItems().get(getIndex())));
                btnAuction.setOnAction(e -> startAuction(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ItemDTO itemDTO = getTableView().getItems().get(getIndex());
                    btnAuction.setDisable(!isEditableStatus(itemDTO.getStatus()));
                    btnEdit.setDisable(!isEditableStatus(itemDTO.getStatus()));
                    setGraphic(new HBox(5, btnEdit, btnDelete, btnAuction));
                }
            }
        });
    }

    private void setupStatusFilter() {
        cmbStatus.setItems(FXCollections.observableArrayList(
            "Tất cả", "Nháp", "Đang đấu giá", "Đã bán", "Hết hạn", "Đã xóa"
        ));
        cmbStatus.setValue("Tất cả");
    }

    private void setupEventHandlers() {
        btnCreateItem.setOnAction(e -> createNewItem());
        btnRefresh.setOnAction(e -> loadItems());
        btnSearch.setOnAction(e -> searchItems());
        cmbStatus.setOnAction(e -> filterByStatus());
        btnEditItem.setOnAction(e -> editSelectedItem());
        btnDeleteItem.setOnAction(e -> deleteSelectedItem());
        btnStartAuction.setOnAction(e -> startAuctionForSelected());
    }

    private void loadItems() {
        if (currentUserId == null) {
            System.err.println("User chưa đăng nhập");
            return;
        }

        // TODO: Gọi API để lấy danh sách items
        // ItemListResponse response = itemService.getSellerItems(currentUserId);
        // itemList = FXCollections.observableArrayList(response.getItems());
        // tblItems.setItems(itemList);

        System.out.println("[Item Management] Tải danh sách items cho seller: " + currentUserId);
    }

    private void createNewItem() {
        System.out.println("[Item Management] Mở dialog tạo item mới");
        // TODO: Mở ItemCreationDialog
    }

    private void searchItems() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            loadItems();
        } else {
            // TODO: Tìm kiếm items theo tên
            System.out.println("[Item Management] Tìm kiếm items: " + keyword);
        }
    }

    private void filterByStatus() {
        String status = cmbStatus.getValue();
        // TODO: Lọc items theo status
        System.out.println("[Item Management] Lọc items theo status: " + status);
    }

    private void selectItem(ItemDTO item) {
        selectedItem = item;
        updateDetailView();
        updateButtonStates();
    }

    private void updateDetailView() {
        if (selectedItem == null) {
            clearDetailView();
            return;
        }

        lblDetailName.setText(selectedItem.getName());
        lblDetailType.setText(selectedItem.getItemType());
        lblDetailPrice.setText(String.format("%,d VNĐ", selectedItem.getBasePrice()));
        lblDetailStatus.setText(selectedItem.getStatus());
        txtDetailDescription.setText(selectedItem.getDescription());
    }

    private void clearDetailView() {
        lblDetailName.setText("");
        lblDetailType.setText("");
        lblDetailPrice.setText("");
        lblDetailStatus.setText("");
        txtDetailDescription.setText("");
    }

    private void updateButtonStates() {
        if (selectedItem == null) {
            btnEditItem.setDisable(true);
            btnDeleteItem.setDisable(true);
            btnStartAuction.setDisable(true);
        } else {
            boolean isEditable = isEditableStatus(selectedItem.getStatus());
            btnEditItem.setDisable(!isEditable);
            btnDeleteItem.setDisable(false);
            btnStartAuction.setDisable(!isEditable);
        }
    }

    private boolean isEditableStatus(String status) {
        return status.equals("Nháp") || status.equals("Hết hạn");
    }

    private void editItem(ItemDTO item) {
        if (!isEditableStatus(item.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", 
                "Không thể sửa item ở trạng thái: " + item.getStatus());
            return;
        }

        System.out.println("[Item Management] Sửa item: " + item.getId());
        // TODO: Mở ItemEditDialog
    }

    private void editSelectedItem() {
        if (selectedItem != null) {
            editItem(selectedItem);
        }
    }

    private void deleteItem(ItemDTO item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Xóa item: " + item.getName());
        alert.setContentText("Bạn có chắc chắn muốn xóa item này?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            System.out.println("[Item Management] Xóa item: " + item.getId());
            // TODO: Gọi API xóa item
            loadItems();
        }
    }

    private void deleteSelectedItem() {
        if (selectedItem != null) {
            deleteItem(selectedItem);
        }
    }

    private void startAuction(ItemDTO item) {
        if (!isEditableStatus(item.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Lỗi",
                "Chỉ có thể mở đấu giá cho items ở trạng thái: Nháp hoặc Hết hạn");
            return;
        }

        System.out.println("[Item Management] Mở đấu giá cho item: " + item.getId());
        // TODO: Mở dialog tạo auction cho item này
    }

    private void startAuctionForSelected() {
        if (selectedItem != null) {
            startAuction(selectedItem);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        if (lblSellerName != null) {
            lblSellerName.setText("Kho hàng của: " + userId);
        }
        loadItems();
    }
}

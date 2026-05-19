package com.ssscloud.auction.client.controller.bidder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.networking.MessageListener;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * AuctionListController — hiển thị danh sách phiên đấu giá đang mở.
 *
 * Flow:
 *   initialize() → loadAuctions()
 *   → send GET_AUCTIONS qua socket
 *   → nhận GET_AUCTIONS_RESPONSE
 *   → render vào TableView
 */
public class AuctionListController implements MessageListener {

    @FXML private TableView<AuctionDTO>              tblAuctions;
    @FXML private TableColumn<AuctionDTO, String>    colName;
    @FXML private TableColumn<AuctionDTO, Long>      colCurrentPrice;
    @FXML private TableColumn<AuctionDTO, Long>      colMinIncrement;
    @FXML private TableColumn<AuctionDTO, String>    colStatus;
    @FXML private TableColumn<AuctionDTO, String>    colEndTime;
    @FXML private Button                             btnRefresh;
    @FXML private Label                              lblStatus;
    @FXML private TextField                          txtSearch;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private final SocketDispatcher    dispatcher = SocketDispatcher.getInstance();
    private final ObservableList<AuctionDTO> masterList  = FXCollections.observableArrayList(); // Lưu toàn bộ dữ liệu gốc để filter/search
    private final ObservableList<AuctionDTO> displayList = FXCollections.observableArrayList(); // Dùng làm items cho TableView, sẽ được filter từ masterList

    private Consumer<AuctionDTO> onOpenAuction;
 
    public void setOnOpenAuction(Consumer<AuctionDTO> callback) {
        this.onOpenAuction = callback;
    }
    @FXML
    public void initialize() {
        setupTable();
        setupSearch();
        socket.addListener(this);
        loadAuctions();
    }

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colMinIncrement.setCellValueFactory(new PropertyValueFactory<>("minIncrement"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        tblAuctions.setItems(displayList);
        tblAuctions.setPlaceholder(new Label("Chưa có phiên đấu giá nào đang mở."));
        // Double-click vào row để mở BiddingRoom
        tblAuctions.setRowFactory(tv -> {
            TableRow<AuctionDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty() && onOpenAuction != null) {
                    onOpenAuction.accept(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupSearch() {
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, old, newVal) -> filterList(newVal));
        }
    }

    private void loadAuctions() {
        if (lblStatus != null) lblStatus.setText("Đang tải...");

        String json = JsonUtils.toJson(ClientMessage.request("GET_AUCTIONS", null));
 
        dispatcher.request(json, raw -> {
            List<AuctionDTO> auctions = ServerResponse.unwrapList(raw, null, AuctionDTO.class);
            if (auctions != null) {
                updateUI(auctions, null);
            } else {
                updateUI(null, ServerResponse.errorMessage(raw));
            }
        }, () -> updateUI(null, "Không thể kết nối server."));
    }

    private void updateUI(List<AuctionDTO> auctions, String error) {
        if (auctions != null) {
            masterList.setAll(auctions);
            filterList(txtSearch != null ? txtSearch.getText() : "");
            if (lblStatus != null) {
                lblStatus.setText("Tổng: " + auctions.size() + " phiên");
            }
        } else {
            if (lblStatus != null) {
                lblStatus.setText("Lỗi: " + (error != null ? error : "Không xác định"));
            }
        }
    }

    private void filterList(String keyword) {
        displayList.clear();
        if (keyword == null || keyword.isBlank()) {
            displayList.addAll(masterList);
        } else {
            String lower = keyword.toLowerCase();
            masterList.stream()
                    .filter(a -> a.getName() != null && a.getName().toLowerCase().contains(lower))
                    .forEach(displayList::add);
        }
    }

    @Override
    public void onMessageReceived(String json) {
        try {
            ClientMessage msg = JsonUtils.fromJson(json, ClientMessage.class);
            if (msg == null || msg.getAction() == null) return;

            switch (msg.getAction()) {
                case "BID_UPDATE" -> {
                    // Cập nhật thẳng currentPrice vào item trong list — không cần round trip server
                    String dataJson = JsonUtils.toJson(msg.getData());
                    BidDTO bid = JsonUtils.fromJson(dataJson, com.ssscloud.auction.common.dto.response.BidDTO.class);
                    if (bid == null || bid.getAuctionId() == null) return;

                    Platform.runLater(() -> {
                        for (AuctionDTO a : masterList) {
                            if (bid.getAuctionId().equals(a.getId())) {
                                a.setCurrentPrice(bid.getCurrentPrice());
                                break;
                            }
                        }
                        // Refresh TableView vì ObservableList không detect thay đổi field bên trong
                        tblAuctions.refresh();
                    });
                }
                case "AUCTION_ENDED" -> {
                    // Đổi status thành FINISHED trong list
                    String dataJson = JsonUtils.toJson(msg.getData());
                    ClientMessage ended = JsonUtils.fromJson(dataJson, ClientMessage.class);                    
                    String auctionId = ended != null && ended.getData() != null ? ended.getData().toString() : null;

                    if (auctionId == null) {
                        try {
                            JsonObject obj = JsonParser.parseString(dataJson).getAsJsonObject();
                            if (obj.has("auctionId")) auctionId = obj.get("auctionId").getAsString();
                        } catch (Exception ignored) {}
                    }
 
                    if (auctionId == null) return;
                    final String finalId = auctionId;
                    Platform.runLater(() -> {
                        for (AuctionDTO a : masterList) {
                            if (finalId.equals(a.getId())) {
                                a.setStatus(AuctionStatus.FINISHED);
                                break;
                            }
                        }
                        tblAuctions.refresh();
                    });
                }
            }
            
        } catch (Exception e) {
            // Bỏ qua lỗi parse để không crash UI
        }
    }

    public void cleanup() {
        socket.removeListener(this);
    }

    @FXML
    private void handleRefresh() {
        loadAuctions();
    }
}
package com.ssscloud.auction.client.controller;
 
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionListResponse;
import com.ssscloud.auction.common.util.JsonUtils;
 
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
 
import java.io.IOException;
import java.util.List;
 
/** 
 *   - Hiển thị danh sách phiên đấu giá của seller (GET_MY_AUCTIONS).
 *   - Nút "Tạo phiên mới" → mở CreateAuction
 *   - Sau khi tạo xong, tự động refresh danh sách.
 */


//TO_DOS: sau này sẽ bổ sung chức năng refresh thủ công, xem chi tiết phiên đấu giá, xóa/sửa phiên đấu giá (nếu chưa có ai đặt giá)
public class SellerDashboadController {
 
    @FXML private TableView<AuctionDTO>         tblMyAuctions;
    @FXML private TableColumn<AuctionDTO, String> colName;
    @FXML private TableColumn<AuctionDTO, String> colStatus;
    @FXML private TableColumn<AuctionDTO, Long>   colStartPrice;
    @FXML private TableColumn<AuctionDTO, String> colEndTime;
    @FXML private Button                          btnCreateNew;
    @FXML private Label                           lblStatus;
 
    private final AuctionClientSocket socket  = AuctionClientSocket.getInstance();
    private final SessionManager      session = SessionManager.getInstance();
 
    private final ObservableList<AuctionDTO> auctionList = FXCollections.observableArrayList();
 
    @FXML
    public void initialize() {
        setupTable();
        loadMyAuctions();
    }
    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        tblMyAuctions.setItems(auctionList);
        tblMyAuctions.setPlaceholder(new Label("Chưa có phiên đấu giá nào."));
    }
    private void loadMyAuctions() {
        lblStatus.setText("Đang tải...");
        //send and receive, wrap trong ClientMessage để server route đúng controller + action
        ClientMessage msg = new ClientMessage("GET_AUCTIONS", null);
        String jsonRequest = JsonUtils.toJson(msg);
 
        new Thread(() -> {
            List<AuctionDTO> auctions = null;
            String error = null;
            try {
                String jsonResponse = socket.sendAndReceive(jsonRequest);
                if (jsonResponse != null) {
                    ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);
                    if ("GET_AUCTIONS_RESPONSE".equals(serverMsg.getAction())) {
                        String rawData = JsonUtils.toJson(serverMsg.getData());
                        ApiResponse<?> resp = JsonUtils.fromJson(rawData, ApiResponse.class);
                        if (resp.isSuccess()) {
                            String listJson = JsonUtils.toJson(resp.getData());
                            AuctionListResponse listResp = JsonUtils.fromJson(listJson, AuctionListResponse.class);
                            auctions = listResp.getAuctions();
                        } else {
                            error = resp.getMessage();
                        }
                    }
                }
            } catch (Exception e) {
                error = "Lỗi kết nối: " + e.getMessage();
                e.printStackTrace();
            }
 
            final List<AuctionDTO> finalAuctions = auctions;
            final String finalError = error;
 
            Platform.runLater(() -> {
                if (finalAuctions != null) {
                    auctionList.setAll(finalAuctions);
                    lblStatus.setText("Tổng: " + finalAuctions.size() + " phiên");
                } else {
                    lblStatus.setText("Lỗi: " + (finalError != null ? finalError : "Không xác định"));
                }
            });
        }).start();

    }
     @FXML
    private void handleCreateNew() {        //Modal là cửa sổ con hiện lên, khóa tương tác với cửa sổ chính cho đến khi đóng nó lại
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateAuction.fxml")); //nạp giao diện create auction
            Parent root = loader.load();
 
            CreateAuctionController controller = loader.getController();
            // Khi nào người dùng tạo phiên đấu giá thành công, hãy tự động chạy hàm loadMyAuctions ở cửa sổ chính
            controller.setOnSuccessCallback(this::loadMyAuctions); //this::loadMyAuctions là lấy method loadMyAuctions làm tham số
 
            Stage modal = new Stage();
            modal.setTitle("Tạo phiên đấu giá mới");
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setScene(new Scene(root));
            modal.showAndWait();
 
        } catch (IOException e) {
            e.printStackTrace();
            lblStatus.setText("Không thể mở form tạo phiên: " + e.getMessage());
        }
    }


    // private void handleRefresh() {
    //     loadMyAuctions();
    // }
}
package com.ssscloud.auction.client.controller;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.GetAuctionsRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.dto.response.AuctionListResponse;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.networking.AuctionClientSocket;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.Node;
import javafx.application.Platform;

public class BidderDashboardController {

    @FXML private FlowPane auctionContainer;
    @FXML private Label lblPageTitle;
    @FXML private ToggleButton tabAll;
    @FXML private ToggleButton tabArts;
    @FXML private ToggleButton tabElectronnics;
    @FXML private ToggleButton tabVehicles;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private List<AuctionDisplayInfoDTO> allAuctionsDisplayInfo = new ArrayList<>(); // phá json ra để lấy
    private Consumer<AuctionDisplayInfoDTO> onOpenBidRoomHandler;

    public void setOnOpenBidRoom(Consumer<AuctionDisplayInfoDTO> handler) {
        this.onOpenBidRoomHandler = handler;
    }

    @FXML
    public void initialize() {
        fetchActiveAuctions();
    }

    public void loadAuctionsToDashboard(List<AuctionDisplayInfoDTO> auctionsFromDB) {
        // Xóa sạch dữ liệu cũ trước khi nạp mới
        auctionContainer.getChildren().clear();

        for (AuctionDisplayInfoDTO auctionInfo : auctionsFromDB) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-card.fxml"));
                Node card = loader.load();
                
                AuctionCardController cardCtrl = loader.getController();
                cardCtrl.setAuctionDisplayData(auctionInfo, this.onOpenBidRoomHandler); 

                auctionContainer.getChildren().add(card);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void initData(List<AuctionDisplayInfoDTO> dataFromServer) {
        this.allAuctionsDisplayInfo = dataFromServer;
        filterAuctions("ALL"); // Mặc định mở lên là hiện tất cả
    }
    // đang lỗi phải sửa do mới dùng DTo khác
    public void filterAuctions(String categoryType) {
        List<AuctionDisplayInfoDTO> filteredList;

        if (categoryType.equals("ALL")) {
            filteredList = allAuctionsDisplayInfo; // Lấy full kho
        } 
        else {
            // Dùng Stream lọc ra những món đồ khớp với Category
            filteredList = allAuctionsDisplayInfo.stream()
                .filter(auction -> {
                    if (auction.getItemType() == null) {
                        return false;
                    }
                    return auction.getItemName().equals(categoryType);
                })
                .toList();
        }
        loadAuctionsToDashboard(filteredList);
        // Cập nhật giao diện với danh sách đã lọc
    }
    @FXML void filterAll(ActionEvent event) { filterAuctions("ALL"); }
    @FXML void filterElectronics(ActionEvent event) { filterAuctions("ELECTRONIC"); }
    @FXML void filterArts(ActionEvent event) { filterAuctions("ART"); }
    @FXML void filterVehicles(ActionEvent event) { filterAuctions("VEHICLE"); }

    public void fetchActiveAuctions() {
        GetAuctionsRequest req = new GetAuctionsRequest();
        String jsonResponse = socket.sendAndReceive(JsonUtils.toJson(ClientMessage.request("GET_AUCTIONS", req)));
        System.out.println(jsonResponse);
        
        if (jsonResponse != null && !jsonResponse.isEmpty()) {
            ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);
            
            if ("GET_AUCTIONS_RESPONSE".equals(serverMsg.getAction())) {
                String responseRawData = JsonUtils.toJson(serverMsg.getData());
                Type type = new TypeToken<ApiResponse<AuctionListResponse>>() {}.getType();
                ApiResponse<AuctionListResponse> response = JsonUtils.fromJsonGeneric(responseRawData, type);

                if (response != null && response.isSuccess()) {
                    AuctionListResponse listResponse = response.getData();
                    List<AuctionDisplayInfoDTO> auctionInfo = listResponse.getAuctions();
                    updateDashboard(auctionInfo);
                }
            } else {
                Platform.runLater(() -> {
                    lblPageTitle.setText("Không có phòng đấu giá nào cả");
                });
            }
        }
    }
    // hàm ném mấy cái card lên
    public void updateDashboard(List<AuctionDisplayInfoDTO> auctions) {
        Platform.runLater(() -> {
            initData(auctions); // có auction nào thỏa mãn thì ném tất vô
        });
    }
}

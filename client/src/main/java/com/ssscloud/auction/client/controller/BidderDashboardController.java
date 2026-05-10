package com.ssscloud.auction.client.controller;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.GetAuctionsRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.networking.AuctionClientSocket;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.application.Platform;

public class BidderDashboardController {

    @FXML private FlowPane auctionContainer;
    @FXML private Label lblPageTitle;
    @FXML private ToggleButton tabAll;
    @FXML private ToggleButton tabArts;
    @FXML private ToggleButton tabElectronnics;
    @FXML private ToggleButton tabVehicles;
    @FXML private Parent loading; // Giao diện của khung loading
    @FXML private LoadingController loadingController;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private List<AuctionDisplayInfoDTO> allAuctionsDisplayInfo = new ArrayList<>(); // phá json ra để lấy
    private Consumer<AuctionDisplayInfoDTO> onOpenBidRoomHandler;

    public void setOnOpenBidRoom(Consumer<AuctionDisplayInfoDTO> handler) {
        this.onOpenBidRoomHandler = handler;
    }

    @FXML
    public void initialize() {
        new Thread(() -> {
            fetchActiveAuctions();
        }).start();
    }

    public void updateDashboard(List<AuctionDisplayInfoDTO> auctions) {
        Platform.runLater(() -> {
            initData(auctions); // có auction nào thỏa mãn thì ném tất vô
        });
    }

    public void initData(List<AuctionDisplayInfoDTO> dataFromServer) {
        this.allAuctionsDisplayInfo = dataFromServer;
        handleTabSelection("ALL"); // Mặc định mở lên là hiện tất cả
    }

    public void fetchActiveAuctions() {
        GetAuctionsRequest req = new GetAuctionsRequest();
        String jsonResponse = socket.sendAndReceive(JsonUtils.toJson(ClientMessage.request("GET_ACTIVE_AUCTIONS", req)));
        System.out.println(jsonResponse);
        
        if (jsonResponse != null && !jsonResponse.isEmpty()) {
            ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);
            
            if ("GET_ACTIVE_AUCTIONS_RESPONSE".equals(serverMsg.getAction())) {
                String responseRawData = JsonUtils.toJson(serverMsg.getData());
                Type type = new TypeToken<ApiResponse<ListResponse <AuctionDisplayInfoDTO>>>() {}.getType();
                ApiResponse<ListResponse <AuctionDisplayInfoDTO>> response = JsonUtils.fromJsonGeneric(responseRawData, type);
                if (response != null && response.isSuccess()) {
                    ListResponse <AuctionDisplayInfoDTO> listResponse = response.getData();
                    updateDashboard(listResponse.getData());
                }
            } else {
                Platform.runLater(() -> {
                    lblPageTitle.setText("Không có phòng đấu giá nào cả");
                });
            }
        }
    }

    @FXML void filterAll(ActionEvent event) { handleTabSelection("ALL"); }
    @FXML void filterElectronics(ActionEvent event) { handleTabSelection("CATEGORY"); }
    @FXML void filterArts(ActionEvent event) { handleTabSelection("CATEGORY"); }
    @FXML void filterVehicles(ActionEvent event) { handleTabSelection("CATEGORY"); }

    private void handleTabSelection(String actionType) {
        switch (actionType) {
            case "ALL":

                tabAll.setSelected(true);
                tabElectronnics.setSelected(false);
                tabArts.setSelected(false);
                tabVehicles.setSelected(false);
                break;

            case "CATEGORY":
                tabAll.setSelected(false);
                
                if (!tabElectronnics.isSelected() && !tabArts.isSelected() && !tabVehicles.isSelected()) {
                    tabAll.setSelected(true);
                }
                break;
                
            default:
                System.out.println("Lỗi: Không nhận diện được hành động lọc!");
                break;
        }

        // Xử lý xong phần sáng/tối của nút thì gọi thằng đệ đi lọc Data
        applyFilters(); 
    }

    public void applyFilters() {
        if (tabAll.isSelected()) {
            loadAuctionsToDashboard(allAuctionsDisplayInfo);
            return;
        }

        List<String> activeCategories = new ArrayList<>();
        if (tabElectronnics.isSelected()) activeCategories.add("ELECTRONIC");
        if (tabArts.isSelected()) activeCategories.add("ART");
        if (tabVehicles.isSelected()) activeCategories.add("VEHICLE");

        List<AuctionDisplayInfoDTO> filteredList = allAuctionsDisplayInfo.stream()
            .filter(auctioncard -> {
                if (auctioncard.getItemType() == null) return false;
                return activeCategories.contains(String.valueOf(auctioncard.getItemType()));
            })
            .toList();

        loadAuctionsToDashboard(filteredList);
    }

    public void loadAuctionsToDashboard(List<AuctionDisplayInfoDTO> auctionsFromDB) {
        // Xóa sạch dữ liệu cũ trước khi nạp mới
        auctionContainer.getChildren().clear();

        for (AuctionDisplayInfoDTO auction : auctionsFromDB) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-card.fxml"));
                Node card = loader.load();
                
                AuctionCardController cardCtrl = loader.getController();
                cardCtrl.setAuctionDisplayData(auction, this.onOpenBidRoomHandler); 

                auctionContainer.getChildren().add(card);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

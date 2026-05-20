package com.ssscloud.auction.client.controller.bidder;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.GetAuctionsRequest;
import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.controller.shared.LoadingController;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.ServerResponse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.Node;
import javafx.scene.Parent;

public class BidderDashboardController {

    @FXML private FlowPane auctionContainer;
    @FXML private Label lblPageTitle;
    @FXML private ToggleButton tabAll;
    @FXML private ToggleButton tabArts;
    @FXML private ToggleButton tabElectronnics;
    @FXML private ToggleButton tabVehicles;
    @FXML private Parent loading; // Giao diện của khung loading
    @FXML private LoadingController loadingController;

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();
    private List<BidderDisplayDTO> allAuctionsDisplayInfo = new ArrayList<>(); // phá json ra để lấy
    private Consumer<BidderDisplayDTO> onOpenBidRoomHandler;

    public void setOnOpenBidRoom(Consumer<BidderDisplayDTO> handler) {
        this.onOpenBidRoomHandler = handler;
    }


    @FXML
    public void initialize() {
        fetchActiveAuctions();
    }
    public void fetchActiveAuctions() {
        String json = JsonUtils.toJson(ClientMessage.request("GET_ACTIVE_AUCTIONS", new GetAuctionsRequest()));

        dispatcher.request(json, raw -> {
            List<BidderDisplayDTO> list = ServerResponse.unwrapList(raw, "GET_ACTIVE_AUCTIONS_RESPONSE", BidderDisplayDTO.class);
            if (list != null) {
                initData(list);
            } else {
                lblPageTitle.setText("Khong co phong dau gia nao ca");  
            }
        });
    }

    public void initData(List<BidderDisplayDTO> dataFromServer) {
        this.allAuctionsDisplayInfo = dataFromServer;
        handleTabSelection("ALL"); // Mặc định mở lên là hiện tất cả
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

        List<BidderDisplayDTO> filteredList = allAuctionsDisplayInfo.stream()
            .filter(auctioncard -> {
                if (auctioncard.getItemType() == null) return false;
                return activeCategories.contains(String.valueOf(auctioncard.getItemType()));
            })
            .toList();

        loadAuctionsToDashboard(filteredList);
    }

    public void loadAuctionsToDashboard(List<BidderDisplayDTO> auctionsFromDB) {
        // Xóa sạch dữ liệu cũ trước khi nạp mới
        auctionContainer.getChildren().clear();

        for (BidderDisplayDTO auction : auctionsFromDB) {
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

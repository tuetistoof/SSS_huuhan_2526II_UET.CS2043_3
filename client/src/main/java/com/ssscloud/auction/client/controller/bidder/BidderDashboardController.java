package com.ssscloud.auction.client.controller.bidder;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.request.GetAuctionsRequest;
import com.ssscloud.auction.common.payload.response.DTO.BidderDisplayDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.controller.shared.LoadingController;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.networking.MessageListener;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.ServerResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

public class BidderDashboardController implements MessageListener {

    @FXML private FlowPane auctionContainer;
    @FXML private Label lblPageTitle;
    @FXML private ToggleButton tabAll;
    @FXML private ToggleButton tabArts;
    @FXML private ToggleButton tabElectronics;
    @FXML private ToggleButton tabVehicles;
    @FXML private Parent loading; // Giao diện của khung loading
    @FXML private LoadingController loadingController;
    @FXML private StackPane bannerContainer;
    @FXML private Pane bannerPane;

    private int currentBannerIndex = 0;
    private final String[] bannerUrls = {
        "https://images.unsplash.com/photo-1593640408182-31c70c8268f5?w=1920&h=400&fit=crop", 
        "https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=1920&h=400&fit=crop", 
        "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=1920&h=400&fit=crop", 
        "https://images.unsplash.com/photo-1583121274602-3e2820c69888?w=1920&h=400&fit=crop"  
    };

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();
    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private List<BidderDisplayDTO> allAuctionsDisplayInfo = new ArrayList<>();
    private Consumer<BidderDisplayDTO> onOpenBidRoomHandler;

    public void setOnOpenBidRoom(Consumer<BidderDisplayDTO> handler) {
        this.onOpenBidRoomHandler = handler;
    }


    @FXML
    public void initialize() {
        socket.addListener(this);
        fetchActiveAuctions();
        for (String url : bannerUrls) {
            new Image(url, true);
        }
        loadBanner(0);
    }

    public void cleanup() {
        socket.removeListener(this);
    }

    @Override
    public void onMessageReceived(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String action = root.has("action") ? root.get("action").getAsString() : "";
            if ("AUCTION_CANCELED".equals(action)) {
                // Lấy auctionId từ push và xóa card tương ứng khỏi list + UI
                if (root.has("data") && root.get("data").isJsonObject()) {
                    String canceledId = root.get("data").getAsJsonObject()
                            .has("auctionId") ? root.get("data").getAsJsonObject()
                            .get("auctionId").getAsString() : null;
                    if (canceledId != null) {
                        final String id = canceledId;
                        Platform.runLater(() -> removeCanceledCard(id));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[BidderDashboard] onMessageReceived error: " + e.getMessage());
        }
    }

    /**
     * Xóa auction card bị cancel khỏi list và re-render.
     * Không cần re-fetch server — loại bỏ ngay trên RAM.
     */
    private void removeCanceledCard(String auctionId) {
        allAuctionsDisplayInfo.removeIf(dto -> auctionId.equals(dto.getId()));
        applyFilters();
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
                tabElectronics.setSelected(false);
                tabArts.setSelected(false);
                tabVehicles.setSelected(false);
                break;

            case "CATEGORY":
                tabAll.setSelected(false);
                
                if (!tabElectronics.isSelected() && !tabArts.isSelected() && !tabVehicles.isSelected()) {
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
        if (tabElectronics.isSelected()) activeCategories.add("ELECTRONIC");
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

    @FXML
    void handlePrevBanner(javafx.event.ActionEvent event) {
        currentBannerIndex = (currentBannerIndex - 1 + bannerUrls.length) % bannerUrls.length;
        loadBanner(currentBannerIndex);
    }

    @FXML
    void handleNextBanner(javafx.event.ActionEvent event) {
        currentBannerIndex = (currentBannerIndex + 1) % bannerUrls.length;
        loadBanner(currentBannerIndex);
    }

    private void loadBanner(int index) {
        String css = "-fx-background-image: url('" + bannerUrls[index] + "'); " +
                    "-fx-background-size: cover; " +
                    "-fx-background-position: center bottom;";
        bannerPane.setStyle(css);
    }
}
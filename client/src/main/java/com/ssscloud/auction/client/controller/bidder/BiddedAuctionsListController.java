package com.ssscloud.auction.client.controller.bidder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.reflect.TypeToken;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class BiddedAuctionsListController {

    @FXML private VBox emptyState, listContainer, spinnerPane;
    @FXML private Label lblSubtitle, lblTotalCount;
    @FXML private ScrollPane scrollPane;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private Consumer<BidderDisplayDTO> onOpenAuction;

    public void setOnOpenAuction(Consumer<BidderDisplayDTO> onOpenAction) {
        this.onOpenAuction = onOpenAction;
    }

    @FXML
    public void initialize() {
        loadWatchlist();
    }

    public void loadWatchlist() {
    new Thread(() -> {
        try {
            String requestJson = JsonUtils.toJson(ClientMessage.request("GET_BIDDED_AUCTIONS", null));
            String responseJson = socket.sendAndReceive(requestJson);
            if (responseJson == null) return;

            ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
            if (serverMsg == null || serverMsg.getData() == null) return;

            String innerJson = JsonUtils.toJson(serverMsg.getData());
            ApiResponse<?> apiResp = JsonUtils.fromJson(innerJson, ApiResponse.class);
            if (apiResp == null || !apiResp.isSuccess()) {
                renderUI(new ArrayList<>());
                return;
            }

            String listJson = JsonUtils.toJson(apiResp.getData());
            Type listRespType = new TypeToken<ListResponse<BidderDisplayDTO>>(){}.getType();
            ListResponse<BidderDisplayDTO> listResp = JsonUtils.fromJsonGeneric(listJson, listRespType);

            List<BidderDisplayDTO> auctions =
                    (listResp != null && listResp.getData() != null)
                    ? listResp.getData() : new ArrayList<>();

            renderUI(auctions);

        } catch (Exception e) {
            System.err.println("[Bidded Auctions List] Lỗi load: " + e.getMessage());
            e.printStackTrace();
            renderUI(new ArrayList<>());
        }
    }).start();
}

    private void renderUI(List<BidderDisplayDTO> auctions) {
        Platform.runLater(() -> {
            
            listContainer.getChildren().clear();
            lblTotalCount.setText(String.valueOf(auctions.size()));

            if (auctions.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                // spinnerPane.setVisible(true);
                // spinnerPane.setManaged(true);
                scrollPane.setVisible(false);
                scrollPane.setManaged(false);
            } else {
            // Hiện ScrollPane chứa danh sách
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                // spinnerPane.setVisible(false);
                // spinnerPane.setManaged(false);

                scrollPane.setVisible(true);
                scrollPane.setManaged(true);

            // 3. Lặp qua danh sách để tạo Row FXML
                for (BidderDisplayDTO auction : auctions) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bidded-auction-list-row.fxml"));
                        Parent rowNode = loader.load();

                        BiddedAuctionsListRowController rowCtrl = loader.getController();
                        rowCtrl.setData(auction);
                        rowCtrl.setOnViewRoom(() -> {
                            if (onOpenAuction != null) onOpenAuction.accept(auction);
                        });

                        listContainer.getChildren().add(rowNode);
                        System.out.println("[Bidded Auctions List] Successfully added row: " + auction.getAuctionName());

                    } catch (IOException e) {
                        System.err.println("Lỗi nạp hàng cho ID: " + auction.getId());
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}

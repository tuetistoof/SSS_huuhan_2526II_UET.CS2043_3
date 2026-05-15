package com.ssscloud.auction.client.controller;

import com.google.gson.reflect.TypeToken;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.util.JsonUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WatchlistController {
    @FXML private VBox listContainer;
    @FXML private VBox emptyState;
    @FXML private ScrollPane scrollPane;
    @FXML private Label lblTotalCount;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private Consumer<AuctionDisplayInfoDTO> onOpenAuction;

    public void setOnOpenAuction(Consumer<AuctionDisplayInfoDTO> onOpenAuction) {
        this.onOpenAuction = onOpenAuction;
    }

    @FXML
    public void initialize() {
        loadWatchlist();
    }

    public void loadWatchlist() {
    new Thread(() -> {
        try {
            String requestJson = JsonUtils.toJson(ClientMessage.request("GET_WATCHLIST", null));
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
            Type listRespType = new TypeToken<ListResponse<AuctionDisplayInfoDTO>>(){}.getType();
            ListResponse<AuctionDisplayInfoDTO> listResp = JsonUtils.fromJsonGeneric(listJson, listRespType);

            List<AuctionDisplayInfoDTO> auctions =
                    (listResp != null && listResp.getData() != null)
                    ? listResp.getData() : new ArrayList<>();

            renderUI(auctions);

        } catch (Exception e) {
            System.err.println("[WatchlistController] parse error: " + e.getMessage());
            renderUI(new ArrayList<>());
        }
    }).start();
}
    private void renderUI(List<AuctionDisplayInfoDTO> auctions) {
        Platform.runLater(() -> {
            listContainer.getChildren().clear();
            lblTotalCount.setText(String.valueOf(auctions.size()));

            if (auctions.isEmpty()) { // Hiện giao diện empty state
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                scrollPane.setVisible(false);
                scrollPane.setManaged(false);
            } else { // Hiện giao diện danh sách
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                scrollPane.setVisible(true);
                scrollPane.setManaged(true);

                for (AuctionDisplayInfoDTO auction : auctions) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/watchlist-row.fxml"));
                        Parent rowNode = loader.load();

                        WatchlistRowController rowCtrl = loader.getController();
                        rowCtrl.setData(auction);
                        rowCtrl.setOnViewRoom(() -> {
                            if (onOpenAuction != null) onOpenAuction.accept(auction);
                        });
                        rowCtrl.setOnUnfollowSuccess(this::loadWatchlist);
                        listContainer.getChildren().add(rowNode);
                    } catch (IOException e) {
                        System.err.println("[WatchlistController] Error loading row for ID: " + auction.getId());
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
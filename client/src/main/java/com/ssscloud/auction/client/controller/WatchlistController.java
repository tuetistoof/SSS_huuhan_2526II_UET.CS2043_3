package com.ssscloud.auction.client.controller;

import com.google.gson.reflect.TypeToken;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.util.JsonUtils;
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

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();
    private Consumer<BidderDisplayDTO> onOpenAuction;

    public void setOnOpenAuction(Consumer<BidderDisplayDTO> onOpenAuction) {
        this.onOpenAuction = onOpenAuction;
    }

    @FXML
    public void initialize() {
        loadWatchlist();
    }

    public void loadWatchlist() {
    String json = JsonUtils.toJson(ClientMessage.request("GET_WATCHLIST", null));
        Type type = new TypeToken<ListResponse<BidderDisplayDTO>>() {}.getType();

        dispatcher.request(json, raw -> {
            ListResponse<BidderDisplayDTO> listResp = ServerResponse.unwrapGeneric(raw, null, type);
            List<BidderDisplayDTO> auctions = (listResp != null && listResp.getData() != null)
                    ? listResp.getData() : new ArrayList<>();
            renderUI(auctions);
        });
    }

    private void renderUI(List<BidderDisplayDTO> auctions) {
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

            for (BidderDisplayDTO auction : auctions) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/watchlist-row.fxml"));
                    Parent rowNode = loader.load();

                    WatchlistRowController rowCtrl = loader.getController();
                    rowCtrl.setData(auction);
                    rowCtrl.setOnViewRoom(() -> { if (onOpenAuction != null) onOpenAuction.accept(auction);});
                    rowCtrl.setOnUnfollowSuccess(this::loadWatchlist);
                    listContainer.getChildren().add(rowNode);
                } catch (IOException e) {
                    System.err.println("[WatchlistController] Error loading row for ID: " + auction.getId());
                    e.printStackTrace();
                }
            }
        }
    }
}
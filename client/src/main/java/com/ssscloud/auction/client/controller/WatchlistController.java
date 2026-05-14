package com.ssscloud.auction.client.controller;

import com.google.gson.reflect.TypeToken;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
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
            System.out.println("[Watchlist] Đang tải danh sách chi tiết từ Server...");
            
            String json = JsonUtils.toJson(ClientMessage.request("GET_WATCHLIST", null));
            String responseJson = socket.sendAndReceive(json);
            
            if (responseJson == null) return;

            ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                    String finalJson = getFinalJson(serverMsg.getData());

            // Parse thẳng ra List DTO
            Type type = new TypeToken<ApiResponse<List<AuctionDisplayInfoDTO>>>() {}.getType();
            ApiResponse<List<AuctionDisplayInfoDTO>> resp = JsonUtils.fromJsonGeneric(finalJson, type);

            if (resp != null && resp.isSuccess()) {
                List<AuctionDisplayInfoDTO> auctions = resp.getData();
                System.out.println("[Watchlist] Đã nhận " + (auctions != null ? auctions.size() : 0) + " mục.");

                renderUI(auctions != null ? auctions : new ArrayList<>());
            } else {
                System.err.println("[Watchlist] Server trả về lỗi: " + (resp != null ? resp.getMessage() : "null"));
                renderUI(new ArrayList<>());
            }

        } catch (Exception e) {
            System.err.println("[Watchlist] Lỗi load: " + e.getMessage());
            e.printStackTrace();
            renderUI(new ArrayList<>());
        }
    }).start();
}

// Hàm bổ trợ xử lý JSON (Giữ lại để đảm bảo an toàn)
private String getFinalJson(Object data) {
    if (data == null) return "[]"; // Trả về mảng rỗng nếu null
    if (data instanceof String) return (String) data;
    return JsonUtils.toJson(data);
}
    private void renderUI(List<AuctionDisplayInfoDTO> auctions) {
    // Luôn chạy trên UI Thread khi cập nhật giao diện
        Platform.runLater(() -> {
            System.out.println("[Watchlist] Rendering UI with " + auctions.size() + " items.");
            
            listContainer.getChildren().clear();
            lblTotalCount.setText(String.valueOf(auctions.size()));

            if (auctions.isEmpty()) {
            // Hiện trạng thái trống "Feeling Empty"
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                scrollPane.setVisible(false);
                scrollPane.setManaged(false);
                System.out.println("[Watchlist] UI set to EMPTY state.");
            } else {
            // Hiện ScrollPane chứa danh sách
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                scrollPane.setVisible(true);
                scrollPane.setManaged(true);

            // 3. Lặp qua danh sách để tạo Row FXML
                for (AuctionDisplayInfoDTO auction : auctions) {
                    try {
                    // Nạp file fxml của từng dòng
                        System.out.println("[DEBUG JSON] " + JsonUtils.toJson(auction));
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/watchlist-row.fxml"));
                        Parent rowNode = loader.load();

                        WatchlistRowController rowCtrl = loader.getController();
                        rowCtrl.setData(auction);
                        rowCtrl.setOnViewRoom(() -> {
                            if (onOpenAuction != null) onOpenAuction.accept(auction);
                        });

                        rowCtrl.setOnUnfollowSuccess(this::loadWatchlist);

                        listContainer.getChildren().add(rowNode);
                        System.out.println("[Watchlist] Successfully added row: " + auction.getAuctionName());

                    } catch (IOException e) {
                        System.err.println("Lỗi nạp hàng cho ID: " + auction.getId());
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
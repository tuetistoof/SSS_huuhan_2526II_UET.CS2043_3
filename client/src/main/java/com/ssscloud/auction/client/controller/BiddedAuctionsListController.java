package com.ssscloud.auction.client.controller;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

public class BiddedAuctionsListController {

    @FXML private VBox emptyState, listContainer, spinnerPane;
    @FXML private Label lblSubtitle, lblTotalCount;
    @FXML private ScrollPane scrollPane;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private Consumer<AuctionDisplayInfoDTO> onOpenAuction;

    public void setOnOpenAuction(Consumer<AuctionDisplayInfoDTO> onOpenAction) {
        this.onOpenAuction = onOpenAction;
    }

    @FXML
    public void initialize() {
        loadWatchlist();
    }

    public void loadWatchlist() {
    new Thread(() -> {
        try {
            System.out.println("[Bidded Auctions List] Đang tải danh sách chi tiết từ Server...");
            
            String json = JsonUtils.toJson(ClientMessage.request("GET_BIDDED_AUCTIONS", null));
            String responseJson = socket.sendAndReceive(json);
            
            if (responseJson == null) return;

            ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                    String finalJson = getFinalJson(serverMsg.getData());

            // Parse thẳng ra List DTO
            Type type = new TypeToken<ApiResponse<List<AuctionDisplayInfoDTO>>>() {}.getType();
            ApiResponse<List<AuctionDisplayInfoDTO>> resp = JsonUtils.fromJsonGeneric(finalJson, type);

            if (resp != null && resp.isSuccess()) {
                List<AuctionDisplayInfoDTO> auctions = resp.getData();
                System.out.println("[Bidded Auctions List] Đã nhận " + (auctions != null ? auctions.size() : 0) + " mục.");

                renderUI(auctions != null ? auctions : new ArrayList<>());
            } else {
                System.err.println("[Bidded Auctions List] Server trả về lỗi: " + (resp != null ? resp.getMessage() : "null"));
                renderUI(new ArrayList<>());
            }

        } catch (Exception e) {
            System.err.println("[Bidded Auctions List] Lỗi load: " + e.getMessage());
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
            System.out.println("[Bidded Auctions List] Rendering UI with " + auctions.size() + " items.");
            
            listContainer.getChildren().clear();
            lblTotalCount.setText(String.valueOf(auctions.size()));

            if (auctions.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                spinnerPane.setVisible(true);
                spinnerPane.setManaged(true);
                scrollPane.setVisible(false);
                scrollPane.setManaged(false);
                System.out.println("[Bidded Auctions List] UI set to EMPTY state.");
            } else {
            // Hiện ScrollPane chứa danh sách
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                spinnerPane.setVisible(false);
                spinnerPane.setManaged(false);

                scrollPane.setVisible(true);
                scrollPane.setManaged(true);

            // 3. Lặp qua danh sách để tạo Row FXML
                for (AuctionDisplayInfoDTO auction : auctions) {
                    try {
                    // Nạp file fxml của từng dòng
                        System.out.println("[DEBUG JSON] " + JsonUtils.toJson(auction));
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

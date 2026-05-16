package com.ssscloud.auction.client.controller;

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.networking.MessageListener;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.NotificationDTO;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class NotificationController implements MessageListener {
    @FXML private Label  lblBadge; // badge hiển thị số lượng thông báo chưa đọc
    @FXML private ListView<NotifItem> listNotifs; 
    private final AuctionClientSocket           socket  = AuctionClientSocket.getInstance();
    private final ObservableList<NotifItem>     notifs  = FXCollections.observableArrayList();
    private Consumer<String> onNavigateToAuction; //callback về MainLayout để navigate vào BiddingRoom
    private IntConsumer badgeListener; 
    private Runnable onClosePopup;

    public void setBadgeListener(IntConsumer listener) {
        this.badgeListener = listener;
    }

    public void setOnClosePopup(Runnable onClosePopup) {
        this.onClosePopup = onClosePopup;
    }

    public void init(Consumer<String> navigateCallback) {
        this.onNavigateToAuction = navigateCallback;
        socket.addListener(this);   // add 1 lần, tồn tại xuyên suốt session
        setupList();
    }
    public void fetchPending() {
        new Thread(() -> {
            try {
                String json = JsonUtils.toJson(ClientMessage.request("GET_PENDING_NOTIFICATIONS", null));
                String responseJson = socket.sendAndReceive(json);
                if (responseJson == null) return;

            // Bước 1: parse ClientMessage
                ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                if (serverMsg == null || serverMsg.getData() == null) return;
                if (!"GET_PENDING_NOTIFICATIONS_RESPONSE".equals(serverMsg.getAction())) return;

            // Bước 2: data là ApiResponse<List<NotificationDTO>>
                String dataJson = JsonUtils.toJson(serverMsg.getData());
                Type type = new TypeToken<ApiResponse<List<NotificationDTO>>>() {}.getType();
                ApiResponse<List<NotificationDTO>> apiResp = JsonUtils.fromJsonGeneric(dataJson, type);
                System.out.println("[fetchPending] parsed " + (apiResp != null ? apiResp.getData() : null));


                if (apiResp == null || !apiResp.isSuccess() || apiResp.getData() == null) return;

            // Bước 3: lấy List thực sự rồi xử lý
                List<NotificationDTO> list = apiResp.getData();
                Platform.runLater(() -> handlePendingList(list));

            } catch (Exception e) {
                System.err.println("[NotificationController] fetchPending lỗi: " + e.getMessage());
            }
        }).start();
    }
    private void handlePendingList(List<NotificationDTO> list) {
        if (list == null || list.isEmpty()) return;
        for (NotificationDTO dto : list) {
            LocalDateTime time = dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now();
            NotifItem item;
            if ("OUTBID".equals(dto.getType())) {
                item = new NotifItem("OUTBID", "Outbid",
                    dto.getAuctionName() + "\n" + String.format("%,d ₫", dto.getPrice()),
                    dto.getAuctionId(), time);
            } else {
                item = new NotifItem("ENDED", "Ended auction",
                    dto.getAuctionName() + "\nWinner: " + dto.getWinner()
                        + " — " + String.format("%,d ₫", dto.getPrice()),
                    dto.getAuctionId(), time);
            }
            notifs.add(item);
        }
        notifs.sort((a, b) -> b.getTime().compareTo(a.getTime()));
        updateBadge();
    }

    public void destroy() {
        socket.removeListener(this); // chỉ gọi khi logout
        notifs.clear();
    }

    @Override
    public void onMessageReceived(String json) {
        Platform.runLater(() -> {
            try {
                JsonObject root   = JsonParser.parseString(json).getAsJsonObject();
                String     action = root.has("action") ? root.get("action").getAsString() : "";
                switch (action) {
                    case "OUTBID_NOTIFICATION"        -> handleOutbid(root.getAsJsonObject("data"));
                    case "AUCTION_ENDED_NOTIFICATION" -> handleEnded(root.getAsJsonObject("data"));
                }
            } catch (Exception ignored) {}
        });
    }


    private void handleOutbid(JsonObject data) {
        String auctionId   = data.get("auctionId").getAsString();
        String auctionName = data.get("auctionName").getAsString();
        long   price       = data.get("currentPrice").getAsLong();
 
        NotifItem item = new NotifItem(
                "OUTBID",
                "Outbid",
                auctionName + "\n" + String.format("%,d ₫", price),
                auctionId,
                LocalDateTime.now()
        );
        notifs.add(0, item);
        updateBadge();
    }
    private void handleEnded(JsonObject data) {
        System.out.println("Received auction ended notification: " + data);
        String auctionId   = data.get("auctionId").getAsString();
        String auctionName = data.get("auctionName").getAsString();
        String winner      = data.get("winner").getAsString();
        long   finalPrice  = data.get("finalPrice").getAsLong();
 
        NotifItem item = new NotifItem(
                "ENDED",
                "Ended auction",
                auctionName + "\nWinner: " + winner
                        + " — " + String.format("%,d ₫", finalPrice),
                auctionId,
                LocalDateTime.now()
        );
        notifs.add(0, item);
        updateBadge();
    }

    private void updateBadge() {
        long unread = notifs.stream().filter(n -> !n.isRead()).count();
        if (lblBadge != null) {
            lblBadge.setText(unread > 0 ? String.valueOf(unread) : "");
            lblBadge.setVisible(unread > 0);
            lblBadge.setManaged(unread > 0);
        }
        if (badgeListener != null) badgeListener.accept((int) unread); 
    }
    private void setupList() {
        if (listNotifs == null) return;
        listNotifs.setItems(notifs);
        listNotifs.setPlaceholder(new Label("No notifications available."));
        listNotifs.setCellFactory(lv -> new NotifCell());
    }
    @FXML
    private void handleMarkAllRead() {
        notifs.forEach(n -> n.setRead(true));
        listNotifs.refresh();
        updateBadge();
    }
    private void onClickNotif(NotifItem item) {
        item.setRead(true);
        listNotifs.refresh();
        updateBadge();
        if (onClosePopup != null) onClosePopup.run();
        if (onNavigateToAuction != null) onNavigateToAuction.accept(item.getAuctionId());
    }



     public static class NotifItem {
        private final String        type;       // OUTBID | ENDED
        private final String        title;
        private final String        body;
        private final String        auctionId;
        private final LocalDateTime time;
        private boolean read = false;
 
        public NotifItem(String type, String title, String body,
                         String auctionId, LocalDateTime time) {
            this.type      = type;
            this.title     = title;
            this.body      = body;
            this.auctionId = auctionId;
            this.time      = time;
        }
 
        public String        getType()      { return type; }
        public String        getTitle()     { return title; }
        public String        getBody()      { return body; }
        public String        getAuctionId() { return auctionId; }
        public LocalDateTime getTime()      { return time; }
        public boolean       isRead()       { return read; }
        public void          setRead(boolean r) { this.read = r; }
    }

    private class NotifCell extends ListCell<NotifItem> {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");
 
        private final HBox   root      = new HBox(8);
        private final Label  iconLabel = new Label();
        private final VBox   textBox   = new VBox(2);
        private final Label  titleLbl  = new Label();
        private final Label  bodyLbl   = new Label();
        private final Region spacer    = new Region();
        private final Label  timeLbl   = new Label();
 
        NotifCell() {
            root.setPadding(new Insets(8, 12, 8, 12));
            textBox.getChildren().addAll(titleLbl, bodyLbl);
            titleLbl.getStyleClass().add("notif-title");
            bodyLbl.getStyleClass().add("notif-body");
            bodyLbl.setWrapText(true);
            timeLbl.getStyleClass().add("notif-time");
            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(iconLabel, textBox, spacer, timeLbl);
 
            // Click vào cell → navigate
            root.setOnMouseClicked(e -> {
                NotifItem item = getItem();
                if (item != null) onClickNotif(item);
            });
        }
         @Override
        protected void updateItem(NotifItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); return; }
 
            iconLabel.setText("OUTBID".equals(item.getType()) ? "⚡" : "🏁");
            titleLbl.setText(item.getTitle());
            bodyLbl.setText(item.getBody());
            timeLbl.setText(item.getTime().format(FMT));
 
            // Chưa đọc → highlight
            root.getStyleClass().removeAll("notif-read", "notif-unread");
            root.getStyleClass().add(item.isRead() ? "notif-read" : "notif-unread");
 
            setGraphic(root);
        }
    }
}

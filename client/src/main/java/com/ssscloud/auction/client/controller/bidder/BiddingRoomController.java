package com.ssscloud.auction.client.controller.bidder;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javafx.util.Duration;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.request.AutoBidRequest;
import com.ssscloud.auction.common.payload.request.GetAuctionDetailsRequest;
import com.ssscloud.auction.common.payload.request.PlaceBidRequest;
import com.ssscloud.auction.common.payload.response.DTO.AuctionDTO;
import com.ssscloud.auction.common.payload.response.DTO.BidDTO;
import com.ssscloud.auction.common.payload.response.DTO.UserDTO;
import com.ssscloud.auction.common.util.JsonUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.networking.MessageListener;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.AuctionCountdownTimer;
import com.ssscloud.auction.client.util.PriceChartManager;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.client.util.ThemeManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import javafx.stage.Modality;


public class BiddingRoomController implements MessageListener {
    @FXML private Button btnAutoToggle;
    @FXML private Button btnChangeMaxBidToggle;
    @FXML private Button btnBack;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnTabAuto;
    @FXML private Button btnTabManual;
    @FXML private Button btnBackImage;
    @FXML private Button btnFrontImage;
    @FXML private Button btnFollow;

    @FXML private NumberAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;
    @FXML private VBox formAuto;
    @FXML private VBox formManual;
    @FXML private VBox bidForm;
    @FXML private StackPane containerImage;
    @FXML private ImageView imgBiddingRoom;

    @FXML private Label infoAntiSnipe;
    @FXML private Label infoEndTime;
    @FXML private Label infoItemType;
    @FXML private Label infoMinIncrement;
    @FXML private Label infoName;
    @FXML private Label infoSeller;
    @FXML private Label infoStartPrice;
    @FXML private Label infoStartTime;
    @FXML private Label infoDescription;
    @FXML private Label lblHowToAutoBid;

    @FXML private Label lblAuctionName;
    @FXML private Label lblBidCount;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeaderName;
    @FXML private Label lblMinHint;
    @FXML private Label lblMinIncrement;
    @FXML private Label lblStartPrice;
    @FXML private Label lblStatusBadge;
    @FXML private Label lblTimer;
    @FXML private Label lblOutbid;
    @FXML private Label lblAutoBidMinPrice;

    @FXML private ListView<BidDTO> listViewBidHistory;
    @FXML private VBox panelChart;
    @FXML private VBox panelHistory;
    @FXML private VBox panelInfo;
    @FXML private LineChart<Number, Number> priceLineChart;

    @FXML private Button tabBtnChart;
    @FXML private Button tabBtnHistory;
    @FXML private Button tabBtnInfo;
    @FXML private TextField txtManualBid;
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtNewMaxBid;

    private AuctionCountdownTimer timer;
    private PriceChartManager chartManager;

    private String txt = """
            Auto Bid: allows you to set a maximum bid  ammount, and the system will automatically place bids on your behalf to keep you in the lead, up to your specified maximum. It's a convenient way to stay competitive without having to monitor the auction constantly.

            Update Max Bid: allows you to change your maximum bid during the auction and place a new bid when activated.

            Those features can not be canceled once activated until the auction ends or your max bid is exceeded by others.
            """;

    private boolean isFollowing = false;
    private boolean isAutoBidding = false;
    private long autoBidMaxBid = 0;

    private long maxBid = 0;
    private long increment = 0;
    private long lastSeenVersion = 0;
    private boolean snapshotSyncInProgress = false;

    private AuctionDTO currentAuction;
    private List<String> itemUrls;
    private int currentImageIndex = 0;

    private UserDTO currentUser = SessionManager.getInstance().getCurrentUser();
    private String currentUserName = currentUser != null ? currentUser.getUsername() : null;

    private Runnable onSuccessCallback;
    private volatile boolean roomCanceled = false;

    private final ObservableList<BidDTO> bidHistory = FXCollections.observableArrayList();
    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();

    public void setOnSuccessCallback(Runnable cb) {
        this.onSuccessCallback = cb;
    }

    public void initialize() {
        socket.addListener(this);
        timer = new AuctionCountdownTimer(lblTimer);
        chartManager = new PriceChartManager(priceLineChart, chartXAxis, chartYAxis);
        setupBidHistoryList();
    }

    public void setAuction(AuctionDTO auction) {
        this.currentAuction = auction;
        this.lastSeenVersion = auction != null ? auction.getVersion() : 0;
        this.snapshotSyncInProgress = false;
        itemUrls = auction.getItemDTO().getImageUrls();

        boolean isActive = auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING;
        boolean isFinished = auction.getStatus() == AuctionStatus.FINISHED;
        boolean isCancelled = auction.getStatus() == AuctionStatus.CANCELED;

        if (isCancelled && currentUser != null && "BIDDER".equals(currentUser.getRole().toString())) {
            Platform.runLater(() -> {
                roomCanceled = true;
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Auction Canceled");
                alert.setHeaderText("\"" + auction.getName() + "\" has been canceled by Admin");
                alert.setContentText("This auction is no longer available.\nYou will be redirected to the dashboard automatically.");
                alert.initModality(Modality.APPLICATION_MODAL);

                DialogPane dialogPane = alert.getDialogPane();
                dialogPane.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());
                dialogPane.getStylesheets().add(getClass().getResource("/css/tokens-dark.css").toExternalForm());

                if (ThemeManager.getSavedTheme() == ThemeManager.Theme.DARK) {
                    dialogPane.getStyleClass().add("theme-dark");
                }

                alert.setOnHidden(e -> {
                    cleanup();
                    if (onSuccessCallback != null) onSuccessCallback.run();
                });

                Timeline autoKick = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
                    if (alert.isShowing()) alert.close();
                }));
                autoKick.play();
                alert.show();
            });
            return;
        }

        Platform.runLater(() -> {
            bidHistory.clear();
            populateUI();
            setUpItemImage(itemUrls);
            loadBidHistoryAsync(auction.getBidDto());
            if (btnFollow != null) {
                btnFollow.setDisable(true);
                btnFollow.setText("...");
            }
            if (!isActive) disableAllBidUI(isCancelled);
        });

        checkFollowStatus();
        if (isActive) {
            String subJson = JsonUtils.toJson(ClientMessage.request("SUBSCRIBE_AUCTION", auction.getId()));
            dispatcher.request(subJson, subRaw -> {
                Platform.runLater(() -> {
                    BidDTO highest = findHighestBid();
                    boolean wasMyAuto = highest != null
                            && highest.getBidderId() != null
                            && highest.getBidderId().equals(currentUser.getId())
                            && "AUTO".equalsIgnoreCase(highest.getBidType());
                    applyAutoBidState(wasMyAuto);

                    if (!isFinished && !isCancelled) btnPlaceBid.setDisable(false);
                    timer.start(auction.getEndTime());
                });
            });
        }
    }

    public void cleanup() {
        timer.stop();
        if (currentAuction != null)
            socket.send(JsonUtils.toJson(ClientMessage.request("UNSUBSCRIBE_AUCTION", currentAuction.getId())));
        socket.removeListener(this);
    }

    @FXML
    void handleFollowRoom(ActionEvent event) {
        if (currentAuction == null) return;
        String action = isFollowing ? "UNFOLLOW_AUCTION" : "FOLLOW_AUCTION";
        if (btnFollow != null) {
            btnFollow.setDisable(true);
            btnFollow.setText("...");
        }
        String json = JsonUtils.toJson(ClientMessage.request(action, currentAuction.getId()));
        dispatcher.request(json, raw -> {
            if (ServerResponse.isSuccess(raw)) isFollowing = !isFollowing;
            updateFollowButton();
        }, this::updateFollowButton);
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        if (isAutoBidding) {
            showError("Cannot place manual bids while Auto Bid is running.");
            return;
        }

        String amountText = txtManualBid.getText().trim();
        if (amountText.isEmpty()) {
            showError("Please enter your bid amount");
            return;
        }
        
        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            showError("Invalid bid amount");
            return;
        }
        
        if (amount <= 0) {
            showError("Bid amount needs to be larger than 0");
            return;
        }

        long alreadyLocked = 0;
        BidDTO currentLeaderBid = findHighestBid();
        if (currentLeaderBid != null && currentUserName != null
                && currentUserName.equals(currentLeaderBid.getBidderUsername())) {
            alreadyLocked = currentLeaderBid.getBidAmount();
        }
        long availableBalance = currentUser.getAccountBalance() - currentUser.getUnsettledBalance();
        if (amount - alreadyLocked > availableBalance) {
            showError("Insufficient balance.\nYour available balance is not enough to continue this action.");
            return;
        }

        if (!hasBids()) {
            if (amount < currentAuction.getStartPrice()) {
                showError("Bid amount can not be lower than start price: "
                        + String.format("%,d ₫", currentAuction.getStartPrice()));
                return;
            }
        } else {
            if (amount <= getCurrentPrice()) {
                showError("Bid amount must be higher than current price");
                return;
            }
            if (amount < getCurrentPrice() + currentAuction.getMinIncrement()) {
                showError("Bid amount needs to pass min increment ("
                        + String.format("%,d ₫", currentAuction.getMinIncrement()) + ")");
                return;
            }
        }
        
        txtManualBid.clear();
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("Progressing...");
        socket.send(JsonUtils.toJson(ClientMessage.request("PLACE_BID",
                new PlaceBidRequest(currentAuction.getId(), amount))));
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (onSuccessCallback != null) onSuccessCallback.run();
    }

    @FXML
    void handleSwitchToAuto(ActionEvent event) {
        formManual.setVisible(false);
        formManual.setManaged(false);
        formAuto.setVisible(true);
        formAuto.setManaged(true);
        lblHowToAutoBid.setVisible(true);
        lblHowToAutoBid.setText(txt);

        if (isAutoBidding) {
            if (btnChangeMaxBidToggle != null) {
                btnChangeMaxBidToggle.setVisible(true);
                btnChangeMaxBidToggle.setManaged(true);
            }
            if (txtNewMaxBid != null) {
                txtNewMaxBid.setVisible(true);
                txtNewMaxBid.setManaged(true);
            }
            if (btnAutoToggle != null) {
                btnAutoToggle.setVisible(false);
                btnAutoToggle.setManaged(false);
            }
        } else {
            if (btnChangeMaxBidToggle != null) {
                btnChangeMaxBidToggle.setVisible(false);
                btnChangeMaxBidToggle.setManaged(false);
            }
            if (txtNewMaxBid != null) {
                txtNewMaxBid.setVisible(false);
                txtNewMaxBid.setManaged(false);
            }
            if (btnAutoToggle != null) {
                btnAutoToggle.setVisible(true);
                btnAutoToggle.setManaged(true);
            }
        }

        btnTabAuto.getStyleClass().setAll("br-tab-active");
        btnTabManual.getStyleClass().setAll("br-tab");
    }

    @FXML
    void handleSwitchToManual(ActionEvent event) {
        if (isAutoBidding) return;
        formAuto.setVisible(false);
        formAuto.setManaged(false);
        formManual.setVisible(true);
        formManual.setManaged(true);
        btnTabManual.getStyleClass().setAll("br-tab-active");
        btnTabAuto.getStyleClass().setAll("br-tab");
    }

    @FXML
    void handleTabChart(ActionEvent event) {
        switchTab(panelChart, tabBtnChart);
        chartManager.rebuild(bidHistory, currentAuction);
    }

    @FXML
    void handleTabHistory(ActionEvent event) {
        switchTab(panelHistory, tabBtnHistory);
    }

    @FXML
    void handleTabInfo(ActionEvent event) {
        switchTab(panelInfo, tabBtnInfo);
        if (currentAuction == null) return;
        
        infoName.setText(currentAuction.getName() != null ? currentAuction.getName() : "-");
        infoSeller.setText(currentAuction.getSellerDTO().getUsername() != null
                ? currentAuction.getSellerDTO().getUsername() : "-");
        infoStartPrice.setText(String.format("%,d ₫", currentAuction.getStartPrice()));
        infoMinIncrement.setText(String.format("%,d ₫", currentAuction.getMinIncrement()));
        
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        infoItemType.setText(currentAuction.getItemDTO().getItemType() != null
                ? currentAuction.getItemDTO().getItemType() : "-");
        infoStartTime.setText(currentAuction.getStartTime() != null
                ? currentAuction.getStartTime().format(dtFmt) : "-");
        infoEndTime.setText(currentAuction.getEndTime() != null
                ? currentAuction.getEndTime().format(dtFmt) : "-");
        infoDescription.setText(currentAuction.getItemDTO().getDescription() != null
                ? currentAuction.getItemDTO().getDescription() : "-");
        infoAntiSnipe.setText("36 s");
    }

    @FXML
    void handleToggleAutoBid(ActionEvent event) {
        String rawText = txtMaxBid.getText() == null ? "" : txtMaxBid.getText().trim();
        if (rawText.isEmpty()) {
            showError("Please enter your max bid amount.");
            return;
        }

        long tempMaxBid;
        try {
            tempMaxBid = Long.parseLong(rawText);
        } catch (NumberFormatException e) {
            showError("Invalid max bid amount.");
            return;
        }

        if (tempMaxBid <= 0) {
            showError("Max bid must be larger than 0.");
            return;
        }

        if (tempMaxBid <= getCurrentPrice()) {
            showError("Max bid must be higher than current price ("+
                    String.format("%,d ₫", getCurrentPrice()) + ").");
            return;
        }

        long availableBalance = currentUser.getAccountBalance() - currentUser.getUnsettledBalance();
        if (tempMaxBid > availableBalance) {
            showError("Insufficient balance.\nAvailable: " + String.format("%,d ₫", availableBalance));
            return;
        }

        btnAutoToggle.setDisable(true);
        btnAutoToggle.setText("Registering...");

        final long finalMaxBid = tempMaxBid;
        String json = JsonUtils.toJson(ClientMessage.request("AUTO_BID",
                new AutoBidRequest(currentAuction.getId(), finalMaxBid)));

        dispatcher.request(json, raw -> {
            if (ServerResponse.isSuccess(raw)) {
                autoBidMaxBid = finalMaxBid;
                applyAutoBidState(true);
            } else {
                showError(ServerResponse.errorMessage(raw));
                resetAutoBidButton();
            }
        }, () -> {
            showError("Connection error.");
            resetAutoBidButton();
        });
    }

    @FXML
    void handleChangeAutoBidMax(ActionEvent event) {
        String rawText = txtNewMaxBid.getText() == null ? "" : txtNewMaxBid.getText().trim();
        if (rawText.isEmpty()) {
            showError("Please enter the new max bid.");
            return;
        }

        long newMax;
        try {
            newMax = Long.parseLong(rawText);
        } catch (NumberFormatException e) {
            showError("Invalid max bid format.");
            return;
        }

        if (newMax <= 0) {
            showError("Max bid must be larger than 0.");
            return;
        }

        if (newMax <= getCurrentPrice()) {
            showError("New max bid must be higher than current price ("+
                    String.format("%,d ₫", getCurrentPrice()) + ").");
            return;
        }

        if (autoBidMaxBid > 0 && newMax == autoBidMaxBid) {
            showError("New max bid is the same as current max bid.");
            return;
        }

        if (newMax > autoBidMaxBid) {
            long availableBalance = currentUser.getAccountBalance() - currentUser.getUnsettledBalance();
            long netRequired = newMax - autoBidMaxBid;
            if (netRequired > availableBalance) {
                showError("Insufficient balance.\nAvailable: " + String.format("%,d ₫", availableBalance));
                return;
            }
        }

        Button btnSource = (Button) event.getSource();
        btnSource.setDisable(true);
        btnSource.setText("Updating...");

        final long finalNewMax = newMax;
        String json = JsonUtils.toJson(ClientMessage.request("AUTO_BID",
                new AutoBidRequest(currentAuction.getId(), finalNewMax)));

        dispatcher.request(json, raw -> {
            Platform.runLater(() -> {
                btnSource.setDisable(false);
                btnSource.setText("Update Max");

                if (ServerResponse.isSuccess(raw)) {
                    autoBidMaxBid = finalNewMax;
                    txtMaxBid.setText(String.format("%,d", finalNewMax));
                    txtNewMaxBid.clear();
                    showBanner("Auto bid max updated to " + String.format("%,d ₫", finalNewMax), 4);
                } else {
                    showError(ServerResponse.errorMessage(raw));
                }
            });
        }, () -> {
            Platform.runLater(() -> {
                btnSource.setDisable(false);
                btnSource.setText("Update Max");
                showError("Connection error. Please try again.");
            });
        });
    }

    @FXML
    void navBackImage(ActionEvent event) {
        if (itemUrls == null || itemUrls.isEmpty()) return;
        currentImageIndex = currentImageIndex > 0 ? currentImageIndex - 1 : itemUrls.size() - 1;
        updateImageView();
    }

    @FXML
    void navFrontImage(ActionEvent event) {
        if (itemUrls == null || itemUrls.isEmpty()) return;
        currentImageIndex = currentImageIndex < itemUrls.size() - 1 ? currentImageIndex + 1 : 0;
        updateImageView();
    }

    @Override
    public void onMessageReceived(String jsonMessage) {
        Platform.runLater(() -> handleServerPush(jsonMessage));
    }

    private void handleServerPush(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String action = root.has("action") ? root.get("action").getAsString() : "";
            switch (action.toUpperCase()) {
                case "BID_UPDATE" -> handleBidUpdate(root);
                case "BID_ERROR" -> handleBidError(root);
                case "AUCTION_ENDED" -> handleAuctionEnded(root);
                case "AUTO_BID_STOPPED" -> handleAutoBidStopped(root);
                case "AUCTION_CANCELED" -> handleAuctionCanceled(root);
                default -> { /* Ignore */ }
            }
        } catch (Exception e) {
            System.err.println("[BiddingRoom] Push error: " + e.getMessage());
        }
    }

    private void handleBidUpdate(JsonObject root) {
        BidDTO bid = JsonUtils.fromJson(JsonUtils.toJson(root.get("data")), BidDTO.class);
        if (bid == null || currentAuction == null) return;
        if (bid.getAuctionId() != null && currentAuction.getId() != null
                && !bid.getAuctionId().equals(currentAuction.getId())) return;
        if (!acceptIncomingVersion(bid.getVersion())) return;

        String prevLeader = getCurrentLeader();
        long prevPrice = getCurrentPrice();

        mergeBidIntoHistory(bid);
        lblBidCount.setText(String.valueOf(bidHistory.size()));

        long newPrice = getCurrentPrice();
        String newLeader = getCurrentLeader();

        boolean isNewLeader = bid.getBidAmount() >= prevPrice;
        if (!isNewLeader) return;

        try {
            if (currentUserName != null && currentUserName.equals(bid.getBidderUsername())) {
                Window window = btnBack.getScene().getWindow();
                if (window != null)
                    BidSuccessToastController.show(currentAuction.getName(), bid.getBidAmount(), window);
            } else if (prevLeader != null && prevLeader.equals(currentUserName)
                    && !currentUserName.equals(bid.getBidderUsername())) {
                showOutbidAlert(bid.getBidderUsername(), bid.getBidAmount());
            }
        } catch (Exception ignored) {}

        lblCurrentPrice.setText(String.format("%,d ₫", newPrice));
        lblLeaderName.setText("Leading: " + (newLeader != null ? newLeader : "-"));
        if (lblMinHint != null)
            lblMinHint.setText("Min: " + String.format("%,d ₫", newPrice + currentAuction.getMinIncrement()));

        if (bid.getAntiSnipingEndTime() != null
                && (currentAuction.getEndTime() == null || bid.getAntiSnipingEndTime().isAfter(currentAuction.getEndTime()))) {
            currentAuction.setEndTime(bid.getAntiSnipingEndTime());
            timer.extendTo(bid.getAntiSnipingEndTime());
            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            if (infoEndTime != null) infoEndTime.setText(bid.getAntiSnipingEndTime().format(dtFmt));
        }
        
        if (chartManager.isReady() && panelChart.isVisible()) chartManager.append(bid);
        
        resetPlaceBidButton();
    }

    private void handleBidError(JsonObject root) {
        if (roomCanceled) return;
        String message = "Bid failed.";
        if (root.has("data") && root.get("data").isJsonObject()) {
            JsonObject data = root.get("data").getAsJsonObject();
            if (data.has("message")) message = data.get("message").getAsString();
        }
        showBanner(message, 4);
        resetPlaceBidButton();
    }

    private void handleAuctionEnded(JsonObject root) {
        JsonObject data = root.has("data") ? root.get("data").getAsJsonObject() : new JsonObject();
        long incomingVersion = data.has("version") ? data.get("version").getAsLong() : 0;
        
        if (incomingVersion > 0 && incomingVersion <= lastSeenVersion) return;
        if (incomingVersion > 0) {
            lastSeenVersion = incomingVersion;
            if (currentAuction != null) currentAuction.setVersion(incomingVersion);
        }
        
        String winner = data.has("winnerName") ? data.get("winnerName").getAsString()
                : data.has("winner") ? data.get("winner").getAsString() : "Unknown";
        long finalPrice = data.has("finalPrice") ? data.get("finalPrice").getAsLong() : 0;
        
        timer.stop();
        disableAllBidUI(false);
        
        if (lblStatusBadge != null) {
            lblStatusBadge.setText("Finished");
            lblStatusBadge.getStyleClass().add("br-badge-finished");
        }
        if (finalPrice > 0) lblCurrentPrice.setText(String.format("%,d ₫", finalPrice));
        if (lblTimer != null) lblTimer.setText("Finished");
        
        String myUsername = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getUsername() : "";
        String resultMsg = myUsername.equals(winner)
                ? "🎉 Congrats! You won " + String.format("%,d ₫", finalPrice)
                : "Winner: " + winner + " (" + String.format("%,d ₫", finalPrice) + ")";
                
        showBanner(resultMsg, 0);
    }

    private void handleAuctionCanceled(JsonObject root) {
        JsonObject data = root.has("data") ? root.get("data").getAsJsonObject() : new JsonObject();
        String reason = data.has("reason") ? data.get("reason").getAsString() : "No reason provided";
        String auctionName = data.has("auctionName") ? data.get("auctionName").getAsString() : "";
        roomCanceled = true;

        if (timer != null) timer.stop();
        if (isAutoBidding) {
            isAutoBidding = false;
            autoBidMaxBid = 0;
        }

        disableAllBidUI(true);
        if (btnFollow != null) btnFollow.setDisable(true);

        if (isFollowing && currentAuction != null) {
            isFollowing = false;
            socket.send(JsonUtils.toJson(ClientMessage.request("UNFOLLOW_AUCTION", currentAuction.getId())));
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Auction Canceled");
        alert.setHeaderText("\"" + auctionName + "\" has been canceled by Admin");
        alert.setContentText("Reason: " + reason + "\n\nYou will be redirected to the dashboard automatically.");
        alert.initModality(Modality.APPLICATION_MODAL);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource("/css/tokens-dark.css").toExternalForm());

        if (ThemeManager.getSavedTheme() == ThemeManager.Theme.DARK) {
            dialogPane.getStyleClass().add("theme-dark");
        }
        alert.setOnHidden(e -> {
            cleanup();
            if (onSuccessCallback != null) onSuccessCallback.run();
        });

        Timeline autoKick = new Timeline(new KeyFrame(javafx.util.Duration.seconds(5), e -> {
            if (alert.isShowing()) alert.close();
        }));
        autoKick.play();
        alert.show();
    }

    private void handleAutoBidStopped(JsonObject root) {
        autoBidMaxBid = 0;
        applyAutoBidState(false);
        showBanner("⚠ Auto Bid stopped - current price exceeded your maximum.", 5);
    }

    private void populateUI() {
        if (currentAuction == null) return;
        long currentPrice = getCurrentPrice();
        String leader = getCurrentLeader();

        if (lblAuctionName != null)
            lblAuctionName.setText(currentAuction.getName() != null ? currentAuction.getName() : "-");
        if (lblCurrentPrice != null)
            lblCurrentPrice.setText(String.format("%,d ₫", currentPrice));
        if (lblLeaderName != null)
            lblLeaderName.setText("Leading: " + (leader != null ? leader : "-"));
            
        btnPlaceBid.setDisable(true);
        
        if (lblMinIncrement != null)
            lblMinIncrement.setText(currentAuction.getMinIncrement() > 0
                    ? String.format("%,d ₫", currentAuction.getMinIncrement()) : "-");
        if (lblStartPrice != null)
            lblStartPrice.setText(String.format("%,d ₫", currentAuction.getStartPrice()));
            
        if (lblMinHint != null) {
            lblMinHint.setText(!hasBids() 
                ? "At least: " + String.format("%,d ₫", currentAuction.getStartPrice()) + " (start price)"
                : "At least: " + String.format("%,d ₫", currentPrice + currentAuction.getMinIncrement()));
        }
        
        if (lblStatusBadge != null && currentAuction.getStatus() != null) {
            switch (currentAuction.getStatus()) {
                case RUNNING -> {
                    lblStatusBadge.setText("Running");
                    lblStatusBadge.getStyleClass().setAll("br-badge-running");
                }
                case FINISHED -> {
                    lblStatusBadge.setText("Finished");
                    lblStatusBadge.getStyleClass().setAll("br-badge-ended");
                }
                case CANCELED -> {
                    lblStatusBadge.setText("Canceled");
                    lblStatusBadge.getStyleClass().setAll("br-badge-ended");
                }
                default -> lblStatusBadge.setText(currentAuction.getStatus().toString());
            }
        }
    }

private void applyAutoBidState(boolean active) {
        isAutoBidding = active;
        if (active) {
            Platform.runLater(() -> {
                // Chuyển sang Auto tab
                formAuto.setVisible(true);
                formAuto.setManaged(true);
                formManual.setVisible(false);
                formManual.setManaged(false);
                lblHowToAutoBid.setVisible(true);
                lblHowToAutoBid.setText(txt);

                if (autoBidMaxBid > 0) {
                    txtMaxBid.setText(String.format("%,d", autoBidMaxBid));
                } else {
                    txtMaxBid.setPromptText("Active on server - enter new max to update");
                    txtMaxBid.clear();
                }
                txtMaxBid.setDisable(true);

                if (txtNewMaxBid != null) {
                    txtNewMaxBid.setVisible(true);
                    txtNewMaxBid.setManaged(true);
                    txtNewMaxBid.clear();
                }
                if (btnChangeMaxBidToggle != null) {
                    btnChangeMaxBidToggle.setVisible(true);
                    btnChangeMaxBidToggle.setManaged(true);
                    btnChangeMaxBidToggle.setDisable(false);
                    btnChangeMaxBidToggle.setText("Update Max");
                }

                BidDTO latest = findHighestBid();
                if (latest != null && lblAutoBidMinPrice != null) {
                    lblAutoBidMinPrice.setText("Min: " + String.format("%,d ₫",
                            latest.getBidAmount() + currentAuction.getMinIncrement()));
                }

                btnTabAuto.getStyleClass().setAll("br-tab-active");
                btnTabManual.getStyleClass().setAll("br-tab");
                btnTabManual.setDisable(true);
                btnPlaceBid.setDisable(true);
                txtManualBid.setDisable(true);

                btnAutoToggle.setVisible(false);
                btnAutoToggle.setManaged(false);
            });
        } else {
            Platform.runLater(() -> {
                formAuto.setVisible(false);
                formAuto.setManaged(false);
                formManual.setVisible(true);
                formManual.setManaged(true);

                btnTabAuto.getStyleClass().setAll("br-tab");
                btnTabManual.getStyleClass().setAll("br-tab-active");
                btnTabManual.setDisable(false);
                txtManualBid.setDisable(false);

                txtMaxBid.setDisable(false);
                txtMaxBid.clear();

                if (txtNewMaxBid != null) {
                    txtNewMaxBid.setVisible(false);
                    txtNewMaxBid.setManaged(false);
                    txtNewMaxBid.clear();
                }
                if (btnChangeMaxBidToggle != null) {
                    btnChangeMaxBidToggle.setVisible(false);
                    btnChangeMaxBidToggle.setManaged(false);
                }

                btnAutoToggle.setVisible(true);
                btnAutoToggle.setManaged(true);
                btnAutoToggle.setDisable(false);
                btnAutoToggle.setText("Start Auto Bid");
                btnAutoToggle.getStyleClass().remove("br-btn-auto-active");
                btnAutoToggle.getStyleClass().add("br-btn-secondary");

                btnPlaceBid.setDisable(false);
                btnPlaceBid.setText("Place Bid");
            });
        }
    }

    private void disableAllBidUI(boolean isCancelled) {
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText(isCancelled ? "Canceled" : "Finished");
        btnAutoToggle.setDisable(true);
        btnAutoToggle.setVisible(false);
        btnAutoToggle.setManaged(false);
        txtManualBid.setDisable(true);
        if (txtMaxBid != null) txtMaxBid.setDisable(true);

        if (txtNewMaxBid != null) {
            txtNewMaxBid.setVisible(false);
            txtNewMaxBid.setManaged(false);
        }
        if (btnChangeMaxBidToggle != null) {
            btnChangeMaxBidToggle.setVisible(false);
            btnChangeMaxBidToggle.setManaged(false);
        }
        if (lblTimer != null) lblTimer.setText(isCancelled ? "Canceled" : "Finished");
    }

    private void updateFollowButton() {
        if (btnFollow == null) return;
        btnFollow.setDisable(false);
        if (isFollowing) {
            btnFollow.setText("Following");
            btnFollow.getStyleClass().setAll("br-btn-follow-active");
        } else {
            btnFollow.setText("Follow");
            btnFollow.getStyleClass().setAll("br-btn-follow");
        }
    }

    public void enableSellerViewMode() {
        bidForm.setVisible(false);
        bidForm.setManaged(false);
        if (btnFollow != null) {
            btnFollow.setVisible(false);
            btnFollow.setManaged(false);
        }
        btnPlaceBid.setDisable(true);
    }

    private void setUpItemImage(List<String> urls) {
        this.itemUrls = urls;
        this.currentImageIndex = 0;
        updateImageView();
    }

    private void updateImageView() {
        if (itemUrls != null && !itemUrls.isEmpty())
            imgBiddingRoom.setImage(new Image(itemUrls.get(currentImageIndex), true));
    }

    private void resetPlaceBidButton() {
        if (roomCanceled) return;
        if (!isAutoBidding) btnPlaceBid.setDisable(false);
        btnPlaceBid.setText("Place Bid");
    }

    private void resetAutoBidButton() {
        txtMaxBid.clear();
        txtMaxBid.setDisable(false);
        btnAutoToggle.setVisible(true);
        btnAutoToggle.setManaged(true);
        btnAutoToggle.setDisable(false);
        btnAutoToggle.setText("Start Auto Bid");
        btnAutoToggle.getStyleClass().remove("br-btn-auto-active");
        btnAutoToggle.getStyleClass().add("br-btn-secondary");
    }

    private void showBanner(String message, int seconds) {
        if (lblOutbid == null) return;
        lblOutbid.setText(message);
        lblOutbid.setVisible(true);
        lblOutbid.setManaged(true);
        if (seconds > 0)
            new Timeline(new KeyFrame(javafx.util.Duration.seconds(seconds),
                    e -> {
                        lblOutbid.setVisible(false);
                        lblOutbid.setManaged(false);
                    })).play();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showOutbidAlert(String newLeader, long newPrice) {
        showBanner(String.format("You have been outbid by %s (%,d ₫)", newLeader, newPrice), 5);
    }

    private void switchTab(VBox panelToShow, Button activeTabBtn) {
        panelHistory.setVisible(false);
        panelHistory.setManaged(false);
        panelInfo.setVisible(false);
        panelInfo.setManaged(false);
        panelChart.setVisible(false);
        panelChart.setManaged(false);
        
        tabBtnHistory.getStyleClass().setAll("br-tab");
        tabBtnInfo.getStyleClass().setAll("br-tab");
        tabBtnChart.getStyleClass().setAll("br-tab");

        panelToShow.setVisible(true);
        panelToShow.setManaged(true);
        activeTabBtn.getStyleClass().setAll("br-tab-active");
    }

    private void checkFollowStatus() {
        if (currentAuction == null) return;
        String json = JsonUtils.toJson(ClientMessage.request("CHECK_FOLLOWING", currentAuction.getId()));
        dispatcher.request(json, raw -> {
            Boolean following = ServerResponse.unwrap(raw, null, Boolean.class);
            if (following != null) isFollowing = following;
            updateFollowButton();
        }, this::updateFollowButton);
    }

    private void loadBidHistoryAsync(List<BidDTO> list) {
        if (list == null) return;
        List<BidDTO> copiedList = new ArrayList<>(list);
        mergeBidHistory(copiedList);
        BidDTO latest = findHighestBid();
        if (latest != null) {
            lblCurrentPrice.setText(String.format("%,d ₫", latest.getBidAmount()));
            lblLeaderName.setText("Leading: " + latest.getBidderUsername());
            if (lblMinHint != null)
                lblMinHint.setText("Min: " + String.format("%,d ₫", latest.getBidAmount() + currentAuction.getMinIncrement()));
        }
        lblBidCount.setText(String.valueOf(bidHistory.size()));
    }

    private void mergeBidHistory(List<BidDTO> incoming) {
        if (incoming == null || incoming.isEmpty()) return;
        List<BidDTO> merged = new ArrayList<>(bidHistory);
        for (BidDTO bid : incoming) {
            if (!containsBid(merged, bid)) merged.add(bid);
        }
        merged.sort(Comparator.comparingLong(BidDTO::getBidAmount).reversed()
                .thenComparing(BidDTO::getBidTime, Comparator.nullsLast(Comparator.reverseOrder())));
        bidHistory.setAll(merged);
        if (!bidHistory.isEmpty()) listViewBidHistory.scrollTo(0);
        if (chartManager.isReady() && panelChart.isVisible())
            chartManager.rebuild(bidHistory, currentAuction);
    }

    private void mergeBidIntoHistory(BidDTO bid) {
        if (containsBid(bidHistory, bid)) return;
        int i = 0;
        while (i < bidHistory.size() && bidHistory.get(i).getBidAmount() > bid.getBidAmount())
            i++;
        bidHistory.add(i, bid);
        listViewBidHistory.scrollTo(Math.max(0, i - 1));
    }

    private boolean acceptIncomingVersion(long incomingVersion) {
        if (incomingVersion <= 0) return true;
        if (incomingVersion <= lastSeenVersion) return false;
        if (lastSeenVersion > 0 && incomingVersion > lastSeenVersion + 1) {
            requestSnapshotResync(incomingVersion);
            return false;
        }
        lastSeenVersion = incomingVersion;
        currentAuction.setVersion(incomingVersion);
        return true;
    }

    private void requestSnapshotResync(long incomingVersion) {
        if (snapshotSyncInProgress || currentAuction == null || currentAuction.getId() == null) return;
        snapshotSyncInProgress = true;
        String json = JsonUtils.toJson(ClientMessage.request("GET_AUCTION_DETAILS",
                new GetAuctionDetailsRequest(currentAuction.getId())));
        dispatcher.request(json, raw -> {
            AuctionDTO snapshot = ServerResponse.unwrap(raw, "GET_AUCTION_DETAILS_RESPONSE", AuctionDTO.class);
            if (snapshot != null && Objects.equals(snapshot.getId(), currentAuction.getId())) {
                applySnapshot(snapshot);
            } 
            else {
                showBanner("Auction state changed. Please reopen this room.", 4);
            }
            snapshotSyncInProgress = false;
        }, () -> {
            snapshotSyncInProgress = false;
            showBanner("Could not sync latest auction state.", 4);
        });
    }

    private void applySnapshot(AuctionDTO snapshot) {
        currentAuction = snapshot;
        lastSeenVersion = Math.max(snapshot.getVersion(), lastSeenVersion);
        bidHistory.clear();
        loadBidHistoryAsync(snapshot.getBidDto());
        populateUI();
        if (snapshot.getEndTime() != null) timer.extendTo(snapshot.getEndTime());
        if (chartManager.isReady() && panelChart.isVisible())
            chartManager.rebuild(bidHistory, currentAuction);
            
        boolean isActive = snapshot.getStatus() == AuctionStatus.OPEN || snapshot.getStatus() == AuctionStatus.RUNNING;
        if (isActive) {
            resetPlaceBidButton();
        } else {
            timer.stop();
            disableAllBidUI(snapshot.getStatus() == AuctionStatus.CANCELED);
        }
    }

    private boolean hasBids() {
        return !bidHistory.isEmpty();
    }

    private long getCurrentPrice() {
        BidDTO lastBid = findHighestBid();
        return lastBid != null ? lastBid.getBidAmount() : currentAuction.getStartPrice();
    }

    private String getCurrentLeader() {
        BidDTO lastBid = findHighestBid();
        return lastBid != null ? lastBid.getBidderUsername() : null;
    }

    private BidDTO findHighestBid() {
        return bidHistory.stream().max(Comparator.comparingLong(BidDTO::getBidAmount)).orElse(null);
    }

    private boolean containsBid(List<BidDTO> bids, BidDTO target) {
        if (target == null) return false;
        return bids.stream().anyMatch(e -> sameBid(e, target));
    }

    private boolean sameBid(BidDTO a, BidDTO b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.getAuctionId(), b.getAuctionId())
                && Objects.equals(a.getBidderUsername(), b.getBidderUsername())
                && a.getBidAmount() == b.getBidAmount()
                && Objects.equals(a.getBidTime(), b.getBidTime())
                && Objects.equals(a.getBidType(), b.getBidType());
    }

    private void setupBidHistoryList() {
        listViewBidHistory.setItems(bidHistory);
        listViewBidHistory.setPlaceholder(new Label("There is no bid transaction"));
        listViewBidHistory.setCellFactory(lv -> new BidHistoryCell());
    }

    private static class BidHistoryCell extends ListCell<BidDTO> {
        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
        private final HBox root = new HBox(10);
        private final Label badgeLabel = new Label();
        private final VBox nameTimeBox = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label timeLabel = new Label();
        private final Region spacer = new Region();
        private final Label amountLabel = new Label();

        BidHistoryCell() {
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(4, 0, 4, 0));
            nameTimeBox.getChildren().addAll(nameLabel, timeLabel);
            nameLabel.getStyleClass().add("br-bid-name");
            timeLabel.getStyleClass().add("br-bid-time");
            amountLabel.getStyleClass().add("br-bid-amount");
            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(badgeLabel, nameTimeBox, spacer, amountLabel);
        }

        @Override
        protected void updateItem(BidDTO bid, boolean empty) {
            super.updateItem(bid, empty);
            if (empty || bid == null) {
                setGraphic(null);
                return;
            }
            if ("AUTO".equalsIgnoreCase(bid.getBidType())) {
                badgeLabel.setText("Auto");
                badgeLabel.getStyleClass().setAll("br-bid-type-auto");
            } else {
                badgeLabel.setText("Manual");
                badgeLabel.getStyleClass().setAll("br-bid-type-manual");
            }
            nameLabel.setText(bid.getBidderUsername() != null ? bid.getBidderUsername() : "-");
            timeLabel.setText(bid.getBidTime() != null ? bid.getBidTime().format(TIME_FMT) : "");
            amountLabel.setText(String.format("%,d ₫", bid.getBidAmount()));
            setGraphic(root);
        }
    }
}
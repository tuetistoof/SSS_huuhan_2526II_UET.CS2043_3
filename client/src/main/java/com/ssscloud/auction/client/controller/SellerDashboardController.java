package com.ssscloud.auction.client.controller;
 
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
 
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
 
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
 
public class SellerDashboardController implements Initializable {
 
    // ── Topbar ──
    @FXML private Label lblShopName;
    @FXML private Label lblAvatar;
 
    // ── Metrics ──
    @FXML private Label lblRunning;
    @FXML private Label lblRunningToday;
    @FXML private Label lblTotal;
    @FXML private Label lblBidCount;
    @FXML private Label lblBidToday;
    @FXML private Label lblRevenue;
 
    // ── Tabs ──
    @FXML private ToggleButton tabAll;
    @FXML private ToggleButton tabRunning;
    @FXML private ToggleButton tabOpen;
    @FXML private ToggleButton tabDone;
 
    // ── Table ──
    @FXML private TableView<AuctionDTO> tblAuctions;
    @FXML private TableColumn<AuctionDTO, String> colTitle;
    @FXML private TableColumn<AuctionDTO, String> colCurrentPrice;
    @FXML private TableColumn<AuctionDTO, String> colBidCount;
    @FXML private TableColumn<AuctionDTO, String> colTimeLeft;
    @FXML private TableColumn<AuctionDTO, String> colStatus;
    @FXML private TableColumn<AuctionDTO, Void>   colActions;

    
}

package com.ssscloud.auction.client.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

public class AdminDashboardController {

    @FXML private TableColumn<?, ?> aColAction;
    @FXML private TableColumn<?, ?> aColEnd;
    @FXML private TableColumn<?, ?> aColName;
    @FXML private TableColumn<?, ?> aColPrice;
    @FXML private TableColumn<?, ?> aColSeller;
    @FXML private TableColumn<?, ?> aColStatus;

    @FXML private TableView<?> tblAuctions;
    @FXML private TableView<?> tblUsers;

    @FXML private TableColumn<?, ?> uColAction;
    @FXML private TableColumn<?, ?> uColEmail;
    @FXML private TableColumn<?, ?> uColJoined;
    @FXML private TableColumn<?, ?> uColRole;
    @FXML private TableColumn<?, ?> uColStatus;
    @FXML private TableColumn<?, ?> uColUsername;

    @FXML private ToggleButton aFilterAll;
    @FXML private ToggleButton aFilterDone;
    @FXML private ToggleButton aFilterOpen;
    @FXML private ToggleButton aFilterRunning;

    @FXML private Label lblMetricEnded;
    @FXML private Label lblMetricRunning;
    @FXML private Label lblMetricUsers;

    @FXML private VBox panelAuctions;
    @FXML private VBox panelUsers;

    @FXML private ToggleButton tabAuction;
    @FXML private ToggleButton tabUser;
    @FXML private ToggleButton uFilterAll;
    @FXML private ToggleButton uFilterBidder;
    @FXML private ToggleButton uFilterSeller;

    @FXML private TextField txtAuctionSearch;
    @FXML private TextField txtUserSearch;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private List<BidderDisplayDTO> allAuctionsDisplayInfo = new ArrayList<>();
    private Consumer<BidderDisplayDTO> onOpenBidRoomHandler;

    public void setOnOpenBidRoom(Consumer<BidderDisplayDTO> handler) {
        this.onOpenBidRoomHandler = handler;
    }

    @FXML
    void filterAuctionAll(ActionEvent event) {

    }

    @FXML
    void filterAuctionDone(ActionEvent event) {

    }

    @FXML
    void filterAuctionOpen(ActionEvent event) {

    }

    @FXML
    void filterAuctionRunning(ActionEvent event) {

    }

    @FXML
    void filterUserAll(ActionEvent event) {

    }

    @FXML
    void filterUserBidder(ActionEvent event) {

    }

    @FXML
    void filterUserSeller(ActionEvent event) {

    }

    @FXML
    void searchAuctions(KeyEvent event) {

    }

    @FXML
    void searchUsers(KeyEvent event) {

    }

    @FXML
    void switchToAuctions(ActionEvent event) {

    }

    @FXML
    void switchToUsers(ActionEvent event) {

    }

    // public void applyFilters() {
    //     if (tabAll.isSelected()) {
    //         loadAuctionsToDashboard(allAuctionsDisplayInfo);
    //         return;
    //     }

    //     List<String> activeCategories = new ArrayList<>();
    //     if (tabElectronnics.isSelected()) activeCategories.add("ELECTRONIC");
    //     if (tabArts.isSelected()) activeCategories.add("ART");
    //     if (tabVehicles.isSelected()) activeCategories.add("VEHICLE");

    //     List<BidderDisplayDTO> filteredList = allAuctionsDisplayInfo.stream()
    //         .filter(auctioncard -> {
    //             if (auctioncard.getItemType() == null) return false;
    //             return activeCategories.contains(String.valueOf(auctioncard.getItemType()));
    //         })
    //         .toList();

    //     loadAuctionsToDashboard(filteredList);
    // }

}

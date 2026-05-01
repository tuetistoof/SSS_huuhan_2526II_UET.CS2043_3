package com.ssscloud.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;

public class DashboardController {

    @FXML
    private TableColumn<?, ?> colActions;

    @FXML
    private TableColumn<?, ?> colBidCount;

    @FXML
    private TableColumn<?, ?> colCurrentPrice;

    @FXML
    private TableColumn<?, ?> colStatus;

    @FXML
    private TableColumn<?, ?> colTimeLeft;

    @FXML
    private TableColumn<?, ?> colTitle;

    @FXML
    private Label lblBidCount;

    @FXML
    private Label lblBidToday;

    @FXML
    private Label lblPageTitle;

    @FXML
    private Label lblRevenue;

    @FXML
    private Label lblRunning;

    @FXML
    private Label lblRunningToday;

    @FXML
    private Label lblTotal;

    @FXML
    private ToggleButton tabAll;

    @FXML
    private ToggleButton tabDone;

    @FXML
    private ToggleButton tabOpen;

    @FXML
    private ToggleButton tabRunning;

    @FXML
    private TableView<?> tblAuctions;

    @FXML
    void filterAll(ActionEvent event) {

    }

    @FXML
    void filterFinished(ActionEvent event) {

    }

    @FXML
    void filterOpen(ActionEvent event) {

    }

    @FXML
    void filterRunning(ActionEvent event) {

    }

    @FXML
    void openCreateDialog(ActionEvent event) {

    }

}

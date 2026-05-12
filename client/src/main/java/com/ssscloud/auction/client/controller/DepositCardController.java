package com.ssscloud.auction.client.controller;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.networking.AuctionClientSocket;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class DepositCardController {

    @FXML private Label lblError;
    @FXML private TextField txtDepositValue;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private MainLayoutController mainLayoutController;

    @FXML
    public void intialize() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    @FXML
    void handelConfirm(ActionEvent event) {
        lblError.setVisible(false);
        lblError.setManaged(false);
        String amountTxt = txtDepositValue.getText();
        int amount = Integer.parseInt(amountTxt.trim());
        if (amount < 5000) {
            lblError.setStyle("error-label");
            lblError.setText("Inavlid amount! Minium deposit is 5.000");
        } else {
            new Thread(() -> {
                try {
                    String json = JsonUtils.toJson(ClientMessage.request("DEPOSIT", amount));
                    String responseJson = socket.sendAndReceive(json);
                    if (responseJson != null && !responseJson.isEmpty()) {
                        ClientMessage serverMsg = JsonUtils.fromJson(responseJson, ClientMessage.class);
                        if ("DEPOSIT_RESPONSE".equals(serverMsg.getAction())) {
                            String rawData = JsonUtils.toJson(serverMsg.getData());
                            ApiResponse<?> resp = JsonUtils.fromJson(rawData, ApiResponse.class);
                            
                            if (resp != null && resp.isSuccess()) {
                                String dataJsonStr = JsonUtils.toJson(resp.getData());
                                long newBalance = Long.parseLong(dataJsonStr);
                                Platform.runLater(() -> {
                                    mainLayoutController.updateBalance(newBalance);
                                });
                            }
                            
                        }
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Unsuccess");
                        alert.setHeaderText(null);
                        alert.setContentText("Deposit unsuccessfully! Please try again!");
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }

    @FXML
    void handleCancle(ActionEvent event) {
        Stage stage = (Stage) lblError.getScene().getWindow();
        stage.close();
    }

    public void setMainLayoutController(MainLayoutController ctrl) {
        this.mainLayoutController = ctrl;
    }

}

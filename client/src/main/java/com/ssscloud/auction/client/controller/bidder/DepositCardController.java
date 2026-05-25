package com.ssscloud.auction.client.controller.bidder;

import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.client.controller.shared.MainLayoutController;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.ServerResponse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
public class DepositCardController {

    @FXML private Label lblError;
    @FXML private TextField txtDepositValue;

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();
    private MainLayoutController mainLayoutController;

    @FXML
    public void initialize() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    @FXML
    void handleConfirm(ActionEvent event) {
        lblError.setVisible(false);
        lblError.setManaged(false);
        String amountTxt = txtDepositValue.getText().trim();

        int amount = 0;
        try {
            amount = Integer.parseInt(amountTxt);
        } catch (NumberFormatException e) {
            lblError.setText("Please enter a valid number!");
            lblError.setVisible(true);
            lblError.setManaged(true);
            return;
        }

        if (amount < 5000) {
            lblError.setText("Invalid amount! Minimum deposit is 5,000");
            lblError.setVisible(true);
            lblError.setManaged(true);
            return;
        } 

        String json = JsonUtils.toJson(ClientMessage.request("DEPOSIT", amount));
        dispatcher.request(json, raw -> {
            Double newBalance = ServerResponse.unwrap(raw, "DEPOSIT_RESPONSE", Double.class);
            if (newBalance != null && mainLayoutController != null) {
                // JavaFX cần chạy UI update trên luồng chính (Platform.runLater) nếu dispatcher là luồng khác
                javafx.application.Platform.runLater(() -> {
                    mainLayoutController.updateBalance(newBalance.longValue());
                });
            }
        }, () -> {
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Unsuccess");
                alert.setHeaderText(null);
                alert.setContentText("Deposit unsuccessfully! Please try again!");
                alert.showAndWait();
            });
        });

        Stage stage = (Stage) lblError.getScene().getWindow();
        stage.close();
    }


    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) lblError.getScene().getWindow();
        stage.close();
    }

    public void setMainLayoutController(MainLayoutController ctrl) {
        this.mainLayoutController = ctrl;
    }
}



package com.ssscloud.auction.client.controller.shared;

import com.ssscloud.auction.common.enums.UserRole;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.text.DecimalFormat;

/**
 * Controller for the balance popup panel.
 * Shows total balance, locked/pending, and available amount.
 * Mirrors the notification popup pattern used in MainLayoutController.
 */
public class BalancePopupController {

    @FXML private Label lblTotalAmount;
    @FXML private Label lblBalanceValue;
    @FXML private Label lblLockLabel;
    @FXML private Label lblLockValue;
    @FXML private Label lblAvailableValue;

    private final DecimalFormat formatter = new DecimalFormat("#,###");
    
    public void update(UserRole role, long balance, long unsettled) {
        String balStr = formatter.format(balance) + " ₫";
        String lockStr = formatter.format(unsettled) + " ₫";

        lblBalanceValue.setText(balStr);
        lblTotalAmount.setText(balStr);

        if (role == UserRole.SELLER) {
            lblLockLabel.setText("Pending");
            lblLockValue.setText(lockStr);
            lblAvailableValue.setText(balStr);
        } else {
            long available = balance - unsettled;
            lblLockLabel.setText("Locked");
            lblLockValue.setText(lockStr);
            lblAvailableValue.setText(formatter.format(available) + " ₫");
        }
    }
}
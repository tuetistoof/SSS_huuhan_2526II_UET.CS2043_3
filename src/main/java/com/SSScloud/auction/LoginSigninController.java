package com.SSScloud.auction;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class LoginSigninController {

    @FXML
    private Label welcomeText;

    @FXML
    protected void onLoginButtonClick() {
        welcomeText.setText("LOGIN SUCCESS!");
    }
}
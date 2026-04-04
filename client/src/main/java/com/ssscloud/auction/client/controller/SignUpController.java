package com.ssscloud.auction.client.controller;

import java.io.IOException;

import com.ssscloud.auction.client.util.SceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;

public class SignUpController {

    @FXML
    private Button testbtn;

    @FXML
    void handlereturn(ActionEvent event) {
        testbtn.getScene().setRoot(SceneManager.loginScene);
    }

}

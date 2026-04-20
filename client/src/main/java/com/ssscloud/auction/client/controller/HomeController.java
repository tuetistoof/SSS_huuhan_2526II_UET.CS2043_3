package com.ssscloud.auction.client.controller;

import com.ssscloud.auction.client.util.SceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.Scene;


public class HomeController {

    @FXML
    private Button btnReturn;

    @FXML
    void handleReturn(ActionEvent event) {
        Scene currentScene = btnReturn.getScene();
        currentScene.setRoot(SceneManager.loginScene);
        Stage stage = (Stage) currentScene.getWindow();
        stage.sizeToScene();
        stage.centerOnScreen();
    }

}

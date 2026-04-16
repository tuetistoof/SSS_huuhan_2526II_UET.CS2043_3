package com.ssscloud.auction.client; // Cấu trúc thư mục của sếp

import com.ssscloud.auction.client.util.SceneManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ClientApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager.loginScene = FXMLLoader.load(getClass().getResource("/fxml/login-signup.fxml"));
        SceneManager.registerScene = FXMLLoader.load(getClass().getResource("/fxml/signup.fxml"));
        
        Scene scene = new Scene(SceneManager.loginScene);
        //primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("Cloud Bidding");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {

        launch(args);
    }
}
package com.ssscloud.auction.client; // Cấu trúc thư mục của sếp

import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.util.SceneManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.InputStream;
import java.util.Properties;

public class ClientApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Đọc host/port từ client.properties thay vì hardcode "localhost"
        // → mỗi máy chỉ cần sửa server.host trong file đó rồi build lại
        Properties clientProps = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/client.properties")) {
            if (is != null) {
                clientProps.load(is);
            }
        }
        String host = clientProps.getProperty("server.host", "localhost");
        int port    = Integer.parseInt(clientProps.getProperty("server.port", "5000"));

        AuctionClientSocket.getInstance().connect(host, port);
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
//code test
package com.ssscloud.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApplication extends Application {
	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("/fxml/login-signin.fxml"));
		Scene scene = new Scene(fxmlLoader.load(), 800, 800);

		stage.setTitle("Phần Mềm Đấu Giá");
		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
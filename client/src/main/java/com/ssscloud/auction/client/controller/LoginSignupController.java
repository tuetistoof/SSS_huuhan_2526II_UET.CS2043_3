package com.ssscloud.auction.client.controller;

import java.io.IOException;

import com.ssscloud.auction.client.controller.shared.LoadingController;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.SceneManager;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.request.LoginRequest;
import com.ssscloud.auction.common.payload.response.DTO.UserDTO;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


public class LoginSignupController {

    @FXML private Button btnLogin;
    @FXML private CheckBox chkPassword;
    @FXML private Label lblError;
    @FXML private Hyperlink linkForgetPassword;
    @FXML private Hyperlink linkSignUp;
    @FXML private TextField txtUsername;
    @FXML private TextField txtPassword;
    @FXML private PasswordField txtPasswordHidden;
    @FXML private Parent loading; 
    @FXML private LoadingController loadingController;

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();

    @FXML
    public void initialize() {
        txtPassword.textProperty().bindBidirectional(txtPasswordHidden.textProperty());
        txtPassword.visibleProperty().bind(chkPassword.selectedProperty());
        txtPasswordHidden.visibleProperty().bind(chkPassword.selectedProperty().not());
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
        clearLoginForm();
    }

    @FXML
    private void handleForgetPassword(ActionEvent event) {
        showErrorMsg("Sorry we can't help you now. Try to recall your password or create a new one");
        
    }
    @FXML
    private void handleLogin(ActionEvent event) {
        clearErrorStyles();
        String username = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        boolean hasError = false;

        if (username.isEmpty()) {
            txtUsername.getStyleClass().add("input-error");
            hasError = true;
        }
        if (pass.isEmpty()) {
            txtPasswordHidden.getStyleClass().add("input-error");
            txtPassword.getStyleClass().add("input-error");
            hasError = true;
        }
        if (hasError) {
            showErrorMsg("Missing required infomation.");
            return;
        }
        btnLogin.setDisable(true);
        sendLoginRequest(username, pass);
    }

    public void sendLoginRequest(String username, String pass) {
        if (loading != null && loadingController != null) {
            loading.setVisible(true);
            loadingController.playAnimation();
        } else {
            btnLogin.setDisable(false);
            return;
        }

        String json = JsonUtils.toJson(ClientMessage.request("LOGIN", new LoginRequest(username, pass)));
        dispatcher.request(json, raw -> {
            loadingController.stopAnimation();
            loading.setVisible(false);
            btnLogin.setDisable(false);

            UserDTO userDTO = ServerResponse.unwrap(raw, "LOGIN_RESPONSE", UserDTO.class);
            if (userDTO != null) {
                SessionManager.getInstance().setCurrentUser(userDTO);
                try {
                    Parent homeRoot = FXMLLoader.load(getClass().getResource("/fxml/main-layout.fxml"));
                    Stage stage = (Stage) btnLogin.getScene().getWindow();
                    stage.setMaximized(true);
                    stage.getScene().setRoot(homeRoot);
                    stage.centerOnScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                    showErrorMsg("Khong the tai man hinh chinh.");
                }
            } else {
                showErrorMsg(ServerResponse.errorMessage(raw));
            }
        }, () -> {
            loadingController.stopAnimation();
            loading.setVisible(false);
            btnLogin.setDisable(false);
            lblError.setText("Khong the ket noi toi Server!");
            lblError.setVisible(true);
            lblError.setManaged(true);
        });
}

    @FXML
    private void clearLoginForm() {
        txtUsername.clear();
        txtPassword.clear();
        txtPasswordHidden.clear();
        chkPassword.setSelected(false);
        clearErrorStyles();
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        clearLoginForm();
        Scene currentScene = btnLogin.getScene();
        currentScene.setRoot(SceneManager.registerScene);
        Stage stage = (Stage) currentScene.getWindow();
        stage.sizeToScene();
    }

    private void clearErrorStyles() {
        txtUsername.getStyleClass().remove("input-error");
        txtPassword.getStyleClass().remove("input-error");
        txtPasswordHidden.getStyleClass().remove("input-error");
        
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    public void showErrorMsg(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}

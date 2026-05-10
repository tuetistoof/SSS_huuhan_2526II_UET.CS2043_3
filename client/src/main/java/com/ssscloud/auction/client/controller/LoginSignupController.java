package com.ssscloud.auction.client.controller;

import java.io.IOException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.util.SceneManager;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.LoginRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.UserDTO;
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
import javafx.application.Platform;


public class LoginSignupController {

    @FXML
    private Button btnLogin;

    @FXML
    private CheckBox chkPassword;

    @FXML
    private Label lblError;

    @FXML
    private Hyperlink linkForgetPassword;

    @FXML
    private Hyperlink linkSignUp;

    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtPassword;

    @FXML
    private PasswordField txtPasswordHidden;

    @FXML
    private Parent loading; // Giao diện của khung loading

    @FXML
    private LoadingController loadingController;

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
        try {
            Parent homeRoot = FXMLLoader.load(getClass().getResource("/fxml/forget-pass.fxml"));
            btnLogin.getScene().setRoot(homeRoot);
        }
        catch (IOException e) {
            throw new RuntimeException("Where is my forget-pass.fxml?", e);
        }
    }
    @FXML
    private void handleLogin(ActionEvent event) {
        clearErrorStyles();
        
        //lấy dữ liệu từ UI
        String username = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        
        //validate thông tin (không gửi server nếu thông tin rỗng)
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
            System.out.println("Chưa sửa chèn thêm fxml vào");
            btnLogin.setDisable(false);
            return;
        }

        new Thread(() -> {
            try {
                boolean isSuccess = false;
                String errorMessage = "Unexpected Error";
                UserDTO userDTO = null;

                LoginRequest loginData = new LoginRequest(username, pass);
                ClientMessage msg = ClientMessage.request("LOGIN", loginData);

                String jsonRequest = JsonUtils.toJson(msg);
                String jsonResponse = AuctionClientSocket.getInstance().sendAndReceive(jsonRequest);
                
                if (jsonResponse != null && !jsonResponse.isEmpty()) {
                    ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);

                    if ("LOGIN_RESPONSE".equals(serverMsg.getAction())) {
                        String responseRawData = JsonUtils.toJson(serverMsg.getData());
                        Type type = new TypeToken<ApiResponse<UserDTO>>(){}.getType();
                        ApiResponse<UserDTO> response = JsonUtils.fromJsonGeneric(responseRawData, type);
                        isSuccess = response.isSuccess();
                        
                        if (isSuccess) {
                            SessionManager.getInstance().setCurrentUser(response.getData());
                            userDTO = response.getData();
                            System.out.println("CHECK SESSION ID: " + userDTO.getId());
                        }
                        else {
                            errorMessage = response.getMessage(); // Lấy câu chửi từ server+

                        }
                    }
                    else {
                        errorMessage = "Invalid response from server";
                    }
                }
                else {
                    errorMessage = "No response from server";
                }

                // quay lại UI thread để chuyển cảnh
                final boolean finalSuccess = isSuccess;
                final String finalErrorMessage = errorMessage;
                final UserDTO finalUser = userDTO;

                javafx.application.Platform.runLater(() -> {
                    // Tắt hoạt cảnh
                    loadingController.stopAnimation();
                    loading.setVisible(false);
                    btnLogin.setDisable(false);

                    //login vào home
                    if (finalSuccess) {
                        SessionManager.getInstance().setCurrentUser(finalUser);
                        try {
                            Parent homeRoot = FXMLLoader.load(getClass().getResource("/fxml/MainLayout.fxml"));
                            Stage stage = (Stage) btnLogin.getScene().getWindow();
                             stage.setMaximized(true);

                            stage.getScene().setRoot(homeRoot);
                            stage.centerOnScreen();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            showErrorMsg("Không thể tải màn hình chính.");
                        }
                    }
                    else {
                        showErrorMsg(finalErrorMessage);
                    }
                });
            }
            catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loadingController.stopAnimation();
                    loading.setVisible(false);
                    btnLogin.setDisable(false);
                    lblError.setText("Không thể kết nối tới Server!");
                    lblError.setVisible(true);
                    lblError.setManaged(true);
                });
            }
        }).start();
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

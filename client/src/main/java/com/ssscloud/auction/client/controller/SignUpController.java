package com.ssscloud.auction.client.controller;

import java.io.IOException;

import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.util.SceneManager;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.LoginRequest;
import com.ssscloud.auction.common.dto.request.RegisterRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SignUpController {

    @FXML
    private Button btnSignUp;

    @FXML
    private CheckBox chkPassword;

    @FXML
    private Label lblError;

    @FXML
    private ComboBox<String> cbRoles;

    @FXML
    private Hyperlink linkLogin;

    @FXML
    private TextField txtFirstName;

    @FXML
    private TextField txtLastName;

    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtUserEmail;
    
    @FXML
    private TextField txtUserPassword;

    @FXML
    private TextField txtCFUserPassword;

    @FXML
    private PasswordField txtCFUserPasswordHidden;

    @FXML
    private PasswordField txtUserPasswordHidden;

    @FXML
    private Parent loading; // Giao diện của khung loading

    @FXML
    private LoadingController loadingController;

    @FXML
    public void initialize() {

        cbRoles.getItems().addAll("Bidder", "Seller");

        txtUserPasswordHidden.textProperty().bindBidirectional(txtUserPassword.textProperty());
        txtCFUserPasswordHidden.textProperty().bindBidirectional(txtCFUserPassword.textProperty());

        txtUserPassword.visibleProperty().bind(chkPassword.selectedProperty());
        txtUserPasswordHidden.visibleProperty().bind(chkPassword.selectedProperty().not());
        txtCFUserPassword.visibleProperty().bind(chkPassword.selectedProperty());
        txtCFUserPasswordHidden.visibleProperty().bind(chkPassword.selectedProperty().not());

        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    @FXML
    void handleLogin(ActionEvent event) {
        Scene currentScene = btnSignUp.getScene();
        currentScene.setRoot(SceneManager.loginScene);
        Stage stage = (Stage) currentScene.getWindow();
        stage.sizeToScene();
    }

    @FXML
    void handleSignUp(ActionEvent event) {
        // mỗi một cục này nên cho vào 1 class khác nhau
        // Check pass
        boolean passwordCf = false;

        // Giấu password + error msg
        txtUserPasswordHidden.getStyleClass().remove("input-error");
        txtUserPassword.getStyleClass().remove("input-error");

        txtCFUserPasswordHidden.getStyleClass().remove("input-error");
        txtCFUserPassword.getStyleClass().remove("input-error");

        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);

        if (txtUserPassword.getText().equals(txtCFUserPassword.getText())) {
            passwordCf = true;
        }
        else {
            lblError.setText("The entered passwords do not match. Please try again.");
            lblError.setVisible(true);
            lblError.setManaged(true);

            txtUserPasswordHidden.getStyleClass().add("input-error");
            txtCFUserPasswordHidden.getStyleClass().add("input-error");
            return;
        }
        // Hết check pass

        // Lấy dữ liệu từ UI
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String name = firstName + " " + lastName;
        String username = txtUsername.getText().trim();
        String email = txtUserEmail.getText().trim();
        String password = txtUserPassword.getText().trim();
        String cfPassword = txtCFUserPassword.getText().trim();
        String role = cbRoles.getValue();

        boolean hasError = false;

        if (firstName.isEmpty()) {
            txtUsername.getStyleClass().add("input-error");
            hasError = true;
        }
        if (lastName.isEmpty()) {
            txtUsername.getStyleClass().add("input-error");
            hasError = true;
        }
        if (username.isEmpty()) {
            txtUsername.getStyleClass().add("input-error");
            hasError = true;
        }
        if (email.isEmpty()) {
            txtUserEmail.getStyleClass().add("input-error");
            hasError = true;
        }
        if (username.isEmpty()) {
            txtUsername.getStyleClass().add("input-error");
            hasError = true;
        }
        if (password.isEmpty()) {
            txtUserPassword.getStyleClass().add("input-error");
            txtUserPassword.getStyleClass().add("input-error");
            hasError = true;
        }
        if (cfPassword.isEmpty()) {
            txtCFUserPassword.getStyleClass().add("input-error");
            txtUserPassword.getStyleClass().add("input-error");
            hasError = true;
        }
        if (role.isEmpty()) {
            hasError = true;
        }
        if (hasError == true) {
            lblError.setText("Missing required infomation.");
            lblError.setVisible(true);
            lblError.setManaged(true);
            return;
        }

        if (loading != null && loadingController != null) {
            loading.setVisible(true);
            loadingController.playAnimation();

            new Thread(() -> {
                try {
                    boolean isSuccess = false;
                    String errorMessage = "Unexpected Error";
                    UserDTO userDTO = null;
                    UserRole roleSelected = UserRole.valueOf(role.toUpperCase());

                    RegisterRequest registerData = new RegisterRequest(name, username, password, email, roleSelected);

                    ClientMessage msg = ClientMessage.request("REGISTER", registerData);

                    String jsonRequest = JsonUtils.toJson(msg);
                    String jsonResponse = AuctionClientSocket.getInstance().sendAndReceive(jsonRequest);
                    
                    if (jsonResponse != null && !jsonResponse.isEmpty()) {
                        ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);

                        if ("REGISTER_RESPONS".equals(serverMsg.getAction())) {
                            String responseRawData = JsonUtils.toJson(serverMsg.getData());
                            ApiResponse<UserDTO> response = JsonUtils.fromJsonGeneric(responseRawData, ApiResponse.class);
                            isSuccess = response.isSuccess();
                            if (isSuccess) {
                                userDTO = response.getData();
                                // Trả về chưa hợp lí
                            }
                            else {
                                errorMessage = response.getMessage(); // Lấy câu chửi từ server
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
//                    final boolean finalSuccess = true;
//                    final String finalErrorMessage = "";

                    javafx.application.Platform.runLater(() -> {
                        // Tắt hoạt cảnh
                        loadingController.stopAnimation();
                        loading.setVisible(false);

                        //login vào home
                        if (finalSuccess) {
                            try {
                                SessionManager.getInstance().setCurrentUser(finalUser);

                                Parent homeRoot = FXMLLoader.load(getClass().getResource("/fxml/MainLayout.fxml"));
                                btnSignUp.getScene().setRoot(homeRoot);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            lblError.setText(finalErrorMessage);
                            lblError.setVisible(true);
                            lblError.setManaged(true);
                        }
                    });
                }
                catch (Exception e) {
                    e.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        loadingController.stopAnimation();
                        loading.setVisible(false);
                        lblError.setText("Không thể kết nối tới Server!");
                        lblError.setVisible(true);
                        lblError.setManaged(true);
                    });
                }
            }).start();
        } else {
            // Nếu nó nhảy vào đây thì m phải check lại fx:id trong file fxml và biến controller
            System.out.println("Chưa sửa chèn thêm fxml vào");
        }
    }
}

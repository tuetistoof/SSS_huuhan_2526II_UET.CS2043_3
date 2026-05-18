package com.ssscloud.auction.client.controller;

import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.SceneManager;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.RegisterRequest;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

// Cần thêm check xem đúng định dạng gmail hay ko

public class SignUpController {

    @FXML private Button btnSignUp;
    @FXML private CheckBox chkPassword;
    @FXML private Label lblError;
    @FXML private Label lblBankAccount;
    @FXML private ComboBox<String> cbRoles;
    @FXML private Hyperlink linkLogin;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtUsername;
    @FXML private TextField txtUserEmail;
    @FXML private TextField txtUserPassword;
    @FXML private TextField txtCFUserPassword;
    @FXML private TextField txtBankAccount;
    @FXML private PasswordField txtCFUserPasswordHidden;
    @FXML private PasswordField txtUserPasswordHidden;
    @FXML private Parent loading; 
    @FXML private LoadingController loadingController;

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();


    @FXML
    public void initialize() {
        lblBankAccount.managedProperty().bind(lblBankAccount.visibleProperty());
        txtBankAccount.managedProperty().bind(txtBankAccount.visibleProperty());
        lblBankAccount.setVisible(false);
        txtBankAccount.setVisible(false);

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
        boolean passwordCf = false;
        boolean hasError = false;

        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
        //xóa hiệu ứng nhập sai
        TextField[] allFields = {txtFirstName, txtLastName, txtUsername, txtUserEmail, txtUserPassword, txtCFUserPassword, txtBankAccount, txtUserPasswordHidden, txtCFUserPasswordHidden};
        for (TextField field : allFields) {
            field.getStyleClass().remove("input-error");
        }

        //check có thiếu dữ kiện ko
        TextField[] requiredFields = {txtFirstName, txtLastName, txtUsername, txtUserEmail, txtUserPassword, txtCFUserPassword};
        for (TextField field : requiredFields) {
            if (field.getText().trim().isEmpty()) {
                field.getStyleClass().add("input-error");
                hasError = true;
            }
        }

        String role = cbRoles.getValue();
        String bankAccount = txtBankAccount.getText().trim();

        if (role == null || role.isEmpty()) {
            hasError = true;
        } else if ("Seller".equals(role) && bankAccount.isEmpty()) {
            txtBankAccount.getStyleClass().add("input-error");
            hasError = true;
        }

        if (hasError) {
            showErrorMsg("Missing required information.");
        return;
        }

        //check pass
        String password = txtUserPassword.getText().trim();
        String cfPassword = txtCFUserPassword.getText().trim();

        if (password.equals(cfPassword)) {
            passwordCf = true;
        } else {
            showErrorMsg("The entered passwords do not match. Please try again.");

            txtUserPasswordHidden.getStyleClass().add("input-error");
            txtCFUserPasswordHidden.getStyleClass().add("input-error");
            txtUserPassword.getStyleClass().add("input-error");
            txtCFUserPassword.getStyleClass().add("input-error");
            return;
        }

        // Lấy dữ liệu từ UI
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String name = firstName + " " + lastName;
        String username = txtUsername.getText().trim();
        String email = txtUserEmail.getText().trim();

        UserRole roleSelected = UserRole.valueOf(role.toUpperCase());
        RegisterRequest registerData = (roleSelected == UserRole.SELLER)
            ? new RegisterRequest(name, username, password, email, roleSelected, bankAccount)
            : new RegisterRequest(name, username, password, email, roleSelected);

        sendRegisterRequest(registerData);
    }

    public void sendRegisterRequest(RegisterRequest registerData) {
        if (loading != null && loadingController != null) {
            loading.setVisible(true);
            loadingController.playAnimation();
        } else {
            System.out.println("chưa chèn thêm loading");
        }

        String json = JsonUtils.toJson(ClientMessage.request("REGISTER", registerData));
        dispatcher.request(json, raw -> {
            loadingController.stopAnimation();
            loading.setVisible(false);

            if (ServerResponse.isSuccess(raw, "REGISTER_RESPONSE")) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Registration successful! You can now log in with your new account.");
                alert.showAndWait();
                Scene currentScene = btnSignUp.getScene();
                currentScene.setRoot(SceneManager.loginScene);
                Stage stage = (Stage) currentScene.getWindow();
                stage.sizeToScene();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Registration Failed");
                alert.setHeaderText(null);
                alert.setContentText(ServerResponse.errorMessage(raw));
                alert.showAndWait();
            }
        }, () -> {
            loadingController.stopAnimation();
            loading.setVisible(false);
            showErrorMsg("Khong the ket noi toi Server!");
        });
    }

    public void handleRoleChange(ActionEvent event) {
         switch(cbRoles.getValue()) {
            case "Bidder":
                lblBankAccount.setVisible(false);
                txtBankAccount.setVisible(false);
                break;
            case "Seller":
                lblBankAccount.setVisible(true);
                txtBankAccount.setVisible(true);
                break;
        }
    }

    private void showErrorMsg(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}

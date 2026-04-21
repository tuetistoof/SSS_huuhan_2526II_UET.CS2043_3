package com.ssscloud.auction.client.controller;

import com.ssscloud.auction.client.util.SceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
        boolean passwordCf = false;

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
    }
}

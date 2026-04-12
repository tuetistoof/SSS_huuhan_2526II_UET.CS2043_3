package com.ssscloud.auction.client.controller;

import java.io.IOException;

import com.ssscloud.auction.client.util.SceneManager;

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
    private TextField txtEmail;

    @FXML
    private TextField txtPassword;

    @FXML
    private PasswordField txtPasswordHidden;

    @FXML
    public void initialize() {
        txtPassword.textProperty().bindBidirectional(txtPasswordHidden.textProperty());

        txtPassword.visibleProperty().bind(chkPassword.selectedProperty());
        txtPasswordHidden.visibleProperty().bind(chkPassword.selectedProperty().not());
        
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
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
        txtEmail.getStyleClass().remove("input-error");
        txtPassword.getStyleClass().remove("input-error");
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
        
        String email = txtEmail.getText().trim();
        String pass = txtPassword.getText().trim();
        
        boolean loginSuccess = false;
        boolean hasError = false;

        if (email.isEmpty()) {
            txtEmail.getStyleClass().add("input-error");
            hasError = true;
        }
        if (pass.isEmpty()) {
            txtPasswordHidden.getStyleClass().add("input-error");
            hasError = true;
        }
        if (hasError == true) {
            lblError.setText("Missing required infomation.");
            lblError.setVisible(true);
            lblError.setManaged(true);
            return;
        }
        
        // if (!pass.equals("MK trong db")) {
        //     loginSuccess = false;
        // }
        
        loginSuccess = true;
        if (loginSuccess) {
            try {
                Parent homeRoot = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
                btnLogin.getScene().setRoot(homeRoot);
            }
            catch (IOException e) {
                throw new RuntimeException("Where is my home.fxml?", e);
            }
        }
        else {
            lblError.setText("Incorrect email or password");
            lblError.setVisible(true);
            lblError.setManaged(true);
            txtEmail.getStyleClass().add("input-error");
            txtPasswordHidden.getStyleClass().add("input-error");
        }
    }
    @FXML
    private void clearLoginForm() {
        txtEmail.clear(); 
        txtPassword.clear();
        txtPasswordHidden.clear();

        txtEmail.getStyleClass().remove("input-error");
        txtPassword.getStyleClass().remove("input-error");
        txtPasswordHidden.getStyleClass().remove("input-error");
        
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);

        chkPassword.setSelected(false); 
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        clearLoginForm();
        Scene currentScene = btnLogin.getScene();
        currentScene.setRoot(SceneManager.registerScene);
        Stage stage = (Stage) currentScene.getWindow();
        stage.sizeToScene();
    }
}

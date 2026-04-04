package com.ssscloud.auction.client.controller;

import java.io.IOException;

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
    private PasswordField txtPassword;

    @FXML
    public void initialize() {
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
            txtPassword.getStyleClass().add("input-error");
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
            txtPassword.getStyleClass().add("input-error");
        }
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        try {
                Parent homeRoot = FXMLLoader.load(getClass().getResource("/fxml/signup.fxml"));
                btnLogin.getScene().setRoot(homeRoot);
            }
            catch (IOException e) {
                throw new RuntimeException("Where is my signup.fxml?", e);
            }
    }

    @FXML
    private void handleChkPassword(ActionEvent event) {
    }  
}

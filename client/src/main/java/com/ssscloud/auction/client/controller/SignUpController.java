package com.ssscloud.auction.client.controller;

import java.io.IOException;

import com.ssscloud.auction.client.controller.shared.LoadingController;
import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.SceneManager;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.request.RegisterRequest;
import com.ssscloud.auction.common.util.JsonUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class SignUpController {

    @FXML private Button btnSignUp;
    @FXML private CheckBox chkPassword;
    @FXML private CheckBox chkTerms;
    @FXML private Hyperlink linkTerms;
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
        try {
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/fxml/login-signup.fxml"));
            Scene currentScene = btnSignUp.getScene();
            if (currentScene != null) {
                Stage stage = (Stage) currentScene.getWindow();
                
                double width = stage.getWidth();
                double height = stage.getHeight();
                boolean isMaximized = stage.isMaximized();

                currentScene.setRoot(registerRoot);

                if (isMaximized) {
                    stage.setMaximized(true);
                } else {
                    stage.setWidth(width);
                    stage.setHeight(height);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleSignUp(ActionEvent event) {
        boolean hasError = false;

        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);

        if (!chkTerms.isSelected()) {
            showErrorMsg("Please read and agree to the Terms & Conditions to continue.");
            return;
        }

        TextField[] allFields = {
            txtFirstName, txtLastName, txtUsername, txtUserEmail, 
            txtUserPassword, txtCFUserPassword, txtBankAccount, 
            txtUserPasswordHidden, txtCFUserPasswordHidden
        };
        for (TextField field : allFields) {
            if (field != null) {
                field.getStyleClass().remove("input-error");
            }
        }

        TextField[] requiredFields = {
            txtFirstName, txtLastName, txtUsername, txtUserEmail, 
            txtUserPassword, txtUserPasswordHidden, 
            txtCFUserPassword, txtCFUserPasswordHidden
        };
        for (TextField field : requiredFields) {
            if (field != null && field.isVisible()) {
                if (field.getText().trim().isEmpty()) {
                    field.getStyleClass().add("input-error");
                    hasError = true;
                }
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

        String password = txtUserPassword.getText().trim();
        String cfPassword = txtCFUserPassword.getText().trim();

        if (!password.equals(cfPassword)) {
            showErrorMsg("The entered passwords do not match. Please try again.");
            txtUserPassword.getStyleClass().add("input-error");
            txtUserPasswordHidden.getStyleClass().add("input-error");
            txtCFUserPassword.getStyleClass().add("input-error");
            txtCFUserPasswordHidden.getStyleClass().add("input-error");
            return;
        }

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

            if (ServerResponse.isSuccess(raw)) {
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

    @FXML
    void handleOpenTerms(ActionEvent event) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Terms & Conditions — CloudBid");
        dialog.setResizable(false);

        String termsText =
            "TERMS & CONDITIONS — CloudBid\n\n" +
            "Last updated: May 2026\n\n" +
            "1. ACCEPTANCE\n" +
            "By creating an account you agree to these Terms. If you do not agree, do not register.\n\n" +
            "2. ELIGIBILITY\n" +
            "You must be at least 18 years old and legally capable of entering binding contracts.\n\n" +
            "3. ACCOUNT & SECURITY\n" +
            "You are responsible for maintaining the confidentiality of your credentials. " +
            "Notify us immediately of any unauthorised access.\n\n" +
            "4. BIDDING SYSTEM\n" +
            "Users fully accept the operating mechanism of the bidding system provided by the platform. " +
            "You have no right to complain, object, or speak out against the algorithms and results decided by this system.\n\n" +
            "5. PROHIBITED CONDUCT\n" +
            "Users may not: list counterfeit items, harass other users, manipulate auction outcomes, " +
            "or use automated bots without prior written consent.\n\n" +
            "6. INTELLECTUAL PROPERTY\n" +
            "All content on CloudBid is the property of CloudBid or its licensors. " +
            "You may not reproduce or redistribute any content without permission.\n\n" +
            "7. LIMITATION OF LIABILITY\n" +
            "CloudBid is not liable for any indirect, incidental, or consequential damages " +
            "arising from your use of the platform.\n\n" +
            "8. TERMINATION\n" +
            "We reserve the right to suspend or terminate accounts that violate these Terms at any time.\n\n" +
            "9. CHANGES\n" +
            "We may update these Terms at any time. Continued use after changes constitutes acceptance.\n\n" +
            "By checking the box, you confirm you have read, understood, and agree to these Terms.";

        TextArea ta = new TextArea(termsText);
        ta.setWrapText(true);
        ta.setEditable(false);
        ta.setPrefSize(560, 440);
        ta.setStyle(
            "-fx-font-family: Arial, sans-serif; -fx-font-size: 13px;" +
            "-fx-control-inner-background: #fffafa; -fx-text-fill: #333333;"
        );

        Button btnAccept = new Button("I Agree");
        btnAccept.setStyle(
            "-fx-background-color: #d4537e; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 6px; -fx-padding: 10 40 10 40; -fx-cursor: hand;"
        );
        btnAccept.setOnAction(e -> {
            chkTerms.setSelected(true);
            dialog.close();
        });

        Button btnDecline = new Button("Decline");
        btnDecline.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #888888;" +
            "-fx-font-size: 13px; -fx-background-radius: 6px;" +
            "-fx-border-color: #cccccc; -fx-border-radius: 6px;" +
            "-fx-padding: 9 30 9 30; -fx-cursor: hand;"
        );
        btnDecline.setOnAction(e -> {
            chkTerms.setSelected(false);
            dialog.close();
        });

        javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(12, btnDecline, btnAccept);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-padding: 10 0 0 0;");

        VBox root = new VBox(10, ta, btnRow);
        root.setStyle("-fx-padding: 20; -fx-background-color: #ffffff;");

        dialog.setScene(new Scene(root, 600, 520));
        dialog.showAndWait();
    }
}
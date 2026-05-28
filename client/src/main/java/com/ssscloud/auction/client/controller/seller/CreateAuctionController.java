package com.ssscloud.auction.client.controller.seller;

import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.client.util.ServerResponse;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.request.CreateAuctionRequest;
import com.ssscloud.auction.common.payload.request.ItemData;
import com.ssscloud.auction.common.payload.response.DTO.AuctionDTO;
import com.ssscloud.auction.common.util.JsonUtils;
 
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CreateAuctionController{
    @FXML private TextField        txtTitle;
    @FXML private TextArea         txtDescription;
    @FXML private TextField        txtStartingPrice;
    @FXML private TextField        txtMinIncrement;
    @FXML private DatePicker       dpStartDate;
    @FXML private TextField        txtEndTime;

    @FXML private ComboBox<Integer>cbDuration;
    @FXML private HBox step1Indicator;
    @FXML private HBox step2Indicator;
    @FXML private HBox step3Indicator;
    @FXML private VBox step1Form;
    @FXML private ScrollPane step2Form;
    @FXML private VBox step3Form;

    @FXML private Label lblNum1;
    @FXML private Label lblNum2;
    @FXML private Label lblNum3;

    @FXML private TextField        txtItemName;
    @FXML private ComboBox<String> cmbItemType;
    @FXML private TextField        txtCreator;
    
    @FXML private TextField        txtUrl1;
    @FXML private TextField        txtUrl2;
    @FXML private TextField        txtUrl3;
    @FXML private TextField        txtUrl4;
    @FXML private TextField        txtUrl5;

    @FXML private DatePicker       dpManufacturingDate;
 
    @FXML private VBox     sectionArt;
    @FXML private CheckBox chkCertificate;
 
    @FXML private VBox       sectionVehicleElectronic;
    @FXML private CheckBox   chkIsRepaired;
    @FXML private TextField  txtWarrantyPeriod;
    @FXML private DatePicker dpPurchaseDate;
 
    @FXML private Label  lblError;
    @FXML private Button btnSubmit;
    @FXML private Button btnNext;
    @FXML private Button btnCancel;
    
    private int currentStep = 1;

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();
    private final SessionManager      session = SessionManager.getInstance();
 
    private Consumer<AuctionDTO> onSuccessCallback;
    private Runnable onCancelCallback;
 
    public void setOnSuccessCallback(Consumer<AuctionDTO> callback) {
        this.onSuccessCallback = callback;
    }

    public void setOnCancelCallback(Runnable callback) {
        this.onCancelCallback = callback;
    }

    @FXML
    public void initialize() {
        updateWizard();
        updateStepIndicators();
        clearError();
        cbDuration.getItems().addAll( 0, 1, 3, 5, 7, 14, 30);
        cmbItemType.getItems().addAll(
                "Art",
                "Vehicle",
                "Electronic"
        );

        cmbItemType.setPromptText("-- Select item type --");
 
        hideDynamicSections();

        cmbItemType.valueProperty().addListener((obs, old, newVal) -> {
            hideDynamicSections();
            if (newVal == null) return;
            switch (toItemType(newVal)) {       
                case "ART" -> show(sectionArt);
                case "VEHICLE", "ELECTRONIC" -> show(sectionVehicleElectronic);
            }
        });
        txtEndTime.setPromptText("HH:mm  (e.g., 18:30)");
        txtStartingPrice.setPromptText("e.g., 500000");
        txtMinIncrement.setPromptText("e.g., 50000 (optional)");
        txtWarrantyPeriod.setPromptText("Months, e.g., 12");
    }

    @FXML
    public void handleNavStep(MouseEvent event) {
        Object source = event.getSource();
        if (source == step1Indicator) {
            currentStep = 1;
            updateWizard();
        } else if (source == step2Indicator) {
            if (currentStep == 1 && validateCurrentStep()) {
                currentStep = 2;
                updateWizard();
            } else if (currentStep > 2) {
                currentStep = 2;
                updateWizard();
            }
        } else if (source == step3Indicator) {
            if (currentStep == 2 && validateCurrentStep()) {
                currentStep = 3;
                updateWizard();
            }
        }
    }

    @FXML
    public void handleNextStep(ActionEvent event) {
        if (!validateCurrentStep()) return;
        if (currentStep < 4) {
            currentStep++;
            updateWizard();
        }
    }

    @FXML 
    private void handleSubmit(){
        clearError();

        String name = txtTitle.getText().trim();
        String priceStr = txtStartingPrice.getText().trim().replace(".", "").replace(",", "");
        String incrStr = txtMinIncrement.getText().trim().replace(".", "").replace(",", "");
        String timeStr = txtEndTime.getText().trim();
        String typeStr = cmbItemType.getValue();

        if (dpStartDate.getValue() == null) {
            showError("Please select a start date.");
            return;
        }
        LocalDate startDate = dpStartDate.getValue();
        if (cbDuration.getValue() == null) {
            showError("Please select the auction duration.");
            return;
        }
        LocalDate endDate = dpStartDate.getValue().plusDays(cbDuration.getValue());
        
        if (name.isEmpty()) {
            showError("Auction name cannot be empty.");
            txtTitle.requestFocus();
            return;
        }
        if (priceStr.isEmpty()) {
            showError("Starting price cannot be empty.");
            txtStartingPrice.requestFocus();
            return;
        }
        if (typeStr == null || typeStr.isEmpty()) {
            showError("Please select an item type.");
            cmbItemType.requestFocus();
            return;
        }

        long startPrice;
        try {
            startPrice = Long.parseLong(priceStr);
            if (startPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Starting price must be a positive integer.");
            txtStartingPrice.requestFocus();
            return;
        }

        long minIncrement = 0;
        if (!incrStr.isEmpty()) {
            try {
                minIncrement = Long.parseLong(incrStr);
                if (minIncrement < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showError("Minimum increment must be a non-negative number.");
                txtMinIncrement.requestFocus();
                return;
            }
        }
        
        LocalDateTime startTime;
        if (startDate.isEqual(LocalDate.now())) {
            startTime = LocalDateTime.now();
        } else if (startDate.isBefore(LocalDate.now())) {
            showError("Start date cannot be in the past.");
            return;
        } else {
            startTime = startDate.atStartOfDay();
        }

        LocalDateTime endTime;
        try {
            LocalTime lt = timeStr.isEmpty()
                    ? LocalTime.of(23, 59)
                    : LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:mm"));
            endTime = LocalDateTime.of(endDate, lt);
        } catch (Exception e) {
            showError("Invalid time format. Use HH:mm (e.g., 18:30).");
            txtEndTime.requestFocus();
            return;
        }

        if (Duration.between(startTime, endTime).toMinutes() < 5) {
            showError("The auction duration must be at least 5 minutes.");
            txtEndTime.requestFocus();
            return;
        }

        String urlIfNone = "https://cdn.donmai.us/original/b1/a8/b1a861a2321d635e7a0d6e452730f9d5.jpg";
        List<String> urls = new ArrayList<>(Arrays.asList());
        TextField[] urlFields = {txtUrl1, txtUrl2, txtUrl3, txtUrl4, txtUrl5};
        for (TextField tf : urlFields) {
            String link = tf.getText().trim();
            if(!link.isEmpty()) {
                urls.add(link);
            }
        }
        if (urls.isEmpty()) {
            urls.add(urlIfNone); 
        }
   
        String itemType = toItemType(typeStr);
        ItemData itemData = new ItemData();
        itemData.setName(txtItemName.getText().trim());
        itemData.setCreator(txtCreator.getText().trim());
        itemData.setDescription(txtDescription.getText().trim());
        itemData.setItemType(itemType);
        itemData.setImageUrls(urls);

        switch (itemType) {
            case "ART" -> {
                itemData.setHasCertificate(chkCertificate.isSelected());
            }
            case "VEHICLE", "ELECTRONIC" -> {
                itemData.setIsRepaired(chkIsRepaired.isSelected());
                String wpStr = txtWarrantyPeriod.getText().trim();
                if (!wpStr.isEmpty()) {
                    try {
                        int warrantyPeriod = Integer.parseInt(wpStr);
                        if (warrantyPeriod < 0) throw new NumberFormatException();
                        itemData.setWarrantyPeriod(warrantyPeriod);
                    } catch (NumberFormatException e) {
                        showError("Warranty period must be a non-negative number of months."); 
                        txtWarrantyPeriod.requestFocus(); 
                        return;
                    }
                }
            }
        }

        btnSubmit.setDisable(true);
        btnSubmit.setText("Creating...");

        CreateAuctionRequest reqDTO = new CreateAuctionRequest();
        reqDTO.setName(name);
        reqDTO.setStartPrice(startPrice);
        reqDTO.setMinIncrement(minIncrement);
        reqDTO.setStartTime(startTime);
        reqDTO.setEndTime(endTime);
        reqDTO.setItemData(itemData);
        reqDTO.setSellerId(session.getCurrentUser().getId());

        String json = JsonUtils.toJson(new ClientMessage("CREATE_AUCTION", reqDTO));
        dispatcher.request(json, raw -> {
            btnSubmit.setDisable(false);
            btnSubmit.setText("Create");

            AuctionDTO newAuction = ServerResponse.unwrap(raw, "CREATE_AUCTION_RESPONSE", AuctionDTO.class);
            if (newAuction != null) {
                if (onSuccessCallback != null) onSuccessCallback.accept(newAuction);
            } else {
                showError(ServerResponse.errorMessage(raw));
            }
        }, () -> {
            btnSubmit.setDisable(false);
            btnSubmit.setText("Create");
            showError("No response from server.");
        });
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
    }

    // Helpers
    private String toItemType(String displayValue) {
        if (displayValue == null) return "";
        if (displayValue.contains("Art")) return "ART";
        if (displayValue.contains("Vehicle")) return "VEHICLE";
        if (displayValue.contains("Electronic")) return "ELECTRONIC";
        return displayValue.toUpperCase().trim();
    }

    private void show(Node section) { 
        section.setVisible(true);  
        section.setManaged(true); 
    }
    private void hide(Node section) { 
        section.setVisible(false); 
        section.setManaged(false); 
    }
 
    private void hideDynamicSections() {
        hide(sectionArt);
        hide(sectionVehicleElectronic);
    }
    
    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
 
    private void clearError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void updateWizard() {
        clearError();
        hide(step1Form);
        hide(step2Form);
        hide(step3Form);

        updateStepIndicators();

        switch (currentStep) {
            case 1 -> {
                show(step1Form);
                if (btnNext != null) btnNext.setVisible(true);
                if (btnSubmit != null) btnSubmit.setVisible(false);
            }
            case 2 -> {
                show(step2Form);
                if (btnNext != null) btnNext.setVisible(true);
                if (btnSubmit != null) btnSubmit.setVisible(false);
            }
            case 3 -> {
                show(step3Form);
                if (btnNext != null) {
                    btnNext.setVisible(false);
                    btnNext.setManaged(false);
                }
                if (btnSubmit != null) btnSubmit.setVisible(true);
            }
        }
    }

    private void updateStepIndicators() { 
        if (step1Indicator != null) step1Indicator.getStyleClass().remove("step-row-active");
        if (step2Indicator != null) step2Indicator.getStyleClass().remove("step-row-active");
        if (step3Indicator != null) step3Indicator.getStyleClass().remove("step-row-active");

        if (lblNum1 != null) lblNum1.setText("1");
        if (lblNum2 != null) lblNum2.setText("2");
        if (lblNum3 != null) lblNum3.setText("3");

        if (currentStep >= 1 && step1Indicator != null) {
            step1Indicator.getStyleClass().add("step-row-active");
            if (currentStep > 1 && lblNum1 != null) lblNum1.setText("✓");
        }
        if (currentStep >= 2 && step2Indicator != null) {
            step2Indicator.getStyleClass().add("step-row-active");
            if (currentStep > 2 && lblNum2 != null) lblNum2.setText("✓");
        }
        if (currentStep >= 3 && step3Indicator != null) {
            step3Indicator.getStyleClass().add("step-row-active");
        }
    }

    private boolean validateCurrentStep() {
        clearError();
        switch (currentStep) {
            case 1 -> {
                if (txtTitle.getText().trim().isEmpty()) {
                    showError("Auction name cannot be empty.");
                    txtTitle.requestFocus();
                    return false;
                }
                if (dpStartDate.getValue() == null) {
                    showError("Please select a start date.");
                    return false;
                }
                if (cbDuration.getValue() == null) {
                    showError("Please select the auction duration.");
                    return false;
                }
            }
            case 2 -> {
                if (cmbItemType.getValue() == null) {
                    showError("Please select an item type.");
                    return false;
                }
            }
        }
        return true;
    }
}
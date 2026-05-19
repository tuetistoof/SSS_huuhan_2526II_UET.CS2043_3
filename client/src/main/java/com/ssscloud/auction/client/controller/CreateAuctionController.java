package com.ssscloud.auction.client.controller;

import com.ssscloud.auction.client.networking.SocketDispatcher;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.client.util.ServerResponse;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.request.ItemData;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.util.JsonUtils;
 
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


//TO_DO: chưa làm handleCancle với chuyển màn
//To_DO: start time chưa làm.

public class CreateAuctionController{
    @FXML private TextField        txtTitle;
    @FXML private TextArea         txtDescription;
    @FXML private TextField        txtStartingPrice;
    @FXML private TextField        txtMinIncrement;
    @FXML private DatePicker       dpStartDate;
    @FXML private TextField        txtEndTime;           // "HH:mm"

    @FXML private ComboBox<Integer>cbDuration;
    @FXML private HBox step1Indicator;
    @FXML private HBox step2Indicator;
    @FXML private HBox step3Indicator;
    @FXML private VBox step1Form;
    @FXML private VBox step2Form;
    @FXML private VBox step3Form;

    @FXML private Label lblNum1;
    @FXML private Label lblNum2;
    @FXML private Label lblNum3;

    @FXML private TextField        txtItemName;
    @FXML private ComboBox<String> cmbItemType;          // ART / VEHICLE / ELECTRONIC
    @FXML private TextField        txtCreator;           // tác giả / hãng sản xuất
    
    @FXML private TextField        txtUrl1;
    @FXML private TextField        txtUrl2;
    @FXML private TextField        txtUrl3;
    @FXML private TextField        txtUrl4;
    @FXML private TextField        txtUrl5;

    @FXML private DatePicker       dpManufacturingDate;  // ngày sản xuất (tuỳ chọn)
 
    
    @FXML private VBox     sectionArt;
    @FXML private CheckBox chkCertificate;
 
    @FXML private VBox       sectionVehicleElectronic;
    @FXML private CheckBox   chkIsRepaired;
    @FXML private TextField  txtWarrantyPeriod;
    @FXML private DatePicker dpPurchaseDate;
 
    @FXML private Label  lblError;
    @FXML private Button btnSubmit;
    @FXML private Button btnNext;
    // @FXML private Button btnCancel;
    
    private int currentStep = 1;

    private final SocketDispatcher dispatcher = SocketDispatcher.getInstance();
    private final SessionManager      session = SessionManager.getInstance();
 
    private Consumer<AuctionDTO> onSuccessCallback;
 
    public void setOnSuccessCallback(Consumer<AuctionDTO> callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    public void initialize() {
        updateWizard();
        updateStepIndicators();
        clearError();
        cbDuration.getItems().addAll( 0, 1, 3, 5, 7, 14, 30);
        cmbItemType.getItems().addAll(
                "Nghệ thuật (Art)",
                "Phương tiện (Vehicle)",
                "Điện tử (Electronic)"
        );

        cmbItemType.setPromptText("-- Chọn loại sản phẩm --");
 
        hideDynamicSections();
 
        // dùng toItemType() để map tiếng Việt → key 
        cmbItemType.valueProperty().addListener((obs, old, newVal) -> {
            hideDynamicSections();
            if (newVal == null) return;
            switch (toItemType(newVal)) {       
                case "ART" -> show(sectionArt);
                case "VEHICLE", "ELECTRONIC" -> show(sectionVehicleElectronic);
            }
        });
        txtEndTime.setPromptText("HH:mm  (vd: 18:30)");
        txtStartingPrice.setPromptText("VD: 500000");
        txtMinIncrement.setPromptText("VD: 50000 (tuỳ chọn)");
        txtWarrantyPeriod.setPromptText("Số tháng, vd: 12");
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
       
        //1.lấy thông tin
        String name = txtTitle.getText().trim();
        String priceStr = txtStartingPrice.getText().trim().replace(".", "").replace(",", "");
        String incrStr = txtMinIncrement.getText().trim().replace(".", "").replace(",", "");
        String timeStr = txtEndTime.getText().trim();
        String typeStr = cmbItemType.getValue();

        if (dpStartDate.getValue() == null) {
            showError("Vui lòng chọn ngày bắt đầu.");
            return;
        }
        LocalDate startDate = dpStartDate.getValue();
        if (cbDuration.getValue() == null) {
            showError("Vui lòng chọn thời lượng phiên đấu giá.");
            return;
        }
        LocalDate endDate = dpStartDate.getValue().plusDays(cbDuration.getValue());
        
        //2.validate
        if (name.isEmpty()) {
            showError("Tên phiên đấu giá không được để trống.");
            txtTitle.requestFocus();
            return;
        }
        if (priceStr.isEmpty()) {
            showError("Giá khởi điểm không được để trống.");
            txtStartingPrice.requestFocus();
            return;
        }
        if (typeStr == null || typeStr.isEmpty()) {
            showError("Vui lòng chọn loại sản phẩm.");
            cmbItemType.requestFocus();
            return;
        }
        //ktra giá bắt đầu
        long startPrice;
        try {
            startPrice = Long.parseLong(priceStr);
            if (startPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Giá khởi điểm phải là số nguyên dương.");
            txtStartingPrice.requestFocus();
            return;
        }
        //ktra bước giá
        long minIncrement = 0;
        if (!incrStr.isEmpty()) {
            try {
                minIncrement = Long.parseLong(incrStr);
                if (minIncrement < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showError("Bước giá tối thiểu phải là số không âm.");
                txtMinIncrement.requestFocus();
                return;
            }
        }
        
        //ktra ngày giờ
        LocalDateTime startTime;
        if (startDate.isEqual(LocalDate.now())) {
            startTime = LocalDateTime.now();
        } else if (startDate.isBefore(LocalDate.now())) {
            showError("Ngày bắt đầu không được nằm trong quá khứ.");
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
            showError("Định dạng giờ không hợp lệ. Dùng HH:mm (vd: 18:30).");
            txtEndTime.requestFocus();
            return;
        }
        if (endTime.isBefore(LocalDateTime.now().plusMinutes(5))) {
            showError("Thời gian kết thúc phải cách hiện tại ít nhất 5 phút.");
            return;
        }
        //set tam de demo
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
                        showError("Thời gian bảo hành phải là số tháng không âm."); txtWarrantyPeriod.requestFocus(); 
                        return;
                    }
                }
            }
        }


        //3.Disable nút bấm để tránh bấm nhiều lần
        btnSubmit.setDisable(true);
        btnSubmit.setText("Creating...");

        //4.Tạo request DTO
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
            showError("No response from server");
        });
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        if (onSuccessCallback != null) {
            onSuccessCallback.accept(null);  // null = user huỷ, không có auction
        }
    }

    //Helpers
    private String toItemType(String displayValue) {
        if (displayValue == null) return "";
        if (displayValue.contains("Art"))        return "ART";
        if (displayValue.contains("Vehicle")
                || displayValue.contains("Phương tiện")) return "VEHICLE";
        if (displayValue.contains("Electronic")
                || displayValue.contains("Điện tử"))      return "ELECTRONIC";
        return displayValue.toUpperCase().trim();
    }
    private void show(VBox section) { section.setVisible(true);  section.setManaged(true); }
    private void hide(VBox section) { section.setVisible(false); section.setManaged(false); }
 
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

        // Gọi hàm thắp sáng cột bên trái
        updateStepIndicators();

        // Xử lý Form ở giữa và Nút bấm
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
        // 1. Lột sạch class "step-row-active" để reset về màu xám
        if (step1Indicator != null) step1Indicator.getStyleClass().remove("step-row-active");
        if (step2Indicator != null) step2Indicator.getStyleClass().remove("step-row-active");
        if (step3Indicator != null) step3Indicator.getStyleClass().remove("step-row-active");

        // 2. Reset lại số (phòng khi user ấn lùi bước)
        if (lblNum1 != null) lblNum1.setText("1");
        if (lblNum2 != null) lblNum2.setText("2");
        if (lblNum3 != null) lblNum3.setText("3");

        // 3. Áp dụng màu đỏ và đổi thành dấu tích (✓) cho các bước đã qua
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
            // Bước 3 là cuối cùng nên con số 3 giữ nguyên, không biến thành tích nữa
        }
    }

    //ham nay dang loi vcl
    // private void closeView() {
    //     if (btnSubmit.getScene() != null && btnSubmit.getScene().getWindow() != null)
    //         btnSubmit.getScene().getWindow().hide();
    // }

    private boolean validateCurrentStep() {
        clearError();
        switch (currentStep) {
            case 1 -> {
                if (txtTitle.getText().trim().isEmpty()) {
                    showError("Tên phiên đấu giá không được để trống.");
                    txtTitle.requestFocus();
                    return false;
                }
                if (dpStartDate.getValue() == null) {
                    showError("Vui lòng chọn ngày bắt đầu.");
                    return false;
                }
                if (cbDuration.getValue() == null) {
                    showError("Vui lòng chọn thời lượng.");
                    return false;
                }
            }
            case 2 -> {
                if (cmbItemType.getValue() == null) {
                    showError("Vui lòng chọn loại sản phẩm.");
                    return false;
                }
            }
        }
        return true;
    }
}
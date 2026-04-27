package com.ssscloud.auction.client.controller;
 
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.request.ItemData;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.util.JsonUtils;
 
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


//TO_DO: chưa làm handleCancle với chuyển màn
//To_DO: start time chưa làm.

public class CreateAuctionController{
    @FXML private TextField        txtTitle;
    @FXML private TextArea         txtDescription;
    @FXML private TextField        txtStartingPrice;
    @FXML private TextField        txtMinIncrement;
    @FXML private DatePicker       dpEndDate;
    @FXML private TextField        txtEndTime;           // "HH:mm"
 
    
    @FXML private ComboBox<String> cmbItemType;          // ART / VEHICLE / ELECTRONIC
    @FXML private TextField        txtCreator;           // tác giả / hãng sản xuất
    @FXML private DatePicker       dpManufacturingDate;  // ngày sản xuất (tuỳ chọn)
 
    
    @FXML private VBox     sectionArt;
    @FXML private CheckBox chkCertificate;
 
    @FXML private VBox       sectionVehicleElectronic;
    @FXML private CheckBox   chkIsRepaired;
    @FXML private TextField  txtWarrantyPeriod;
    @FXML private DatePicker dpPurchaseDate;
 
    @FXML private Label  lblError;
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;
 
    private final AuctionClientSocket socket  = AuctionClientSocket.getInstance();
    private final SessionManager      session = SessionManager.getInstance();
 
    private Runnable onSuccessCallback;
 
    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    public void initialize() {
        clearError();
 
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
    private void handleSubmit(){
        clearError();
        
        //1.lấy thông tin
        String name = txtTitle.getText().trim();
        String priceStr = txtStartingPrice.getText().trim().replace(".", "").replace(",", "");
        String incrStr = txtMinIncrement.getText().trim().replace(".", "").replace(",", "");
        LocalDate endDate = dpEndDate.getValue();
        String timeStr = txtEndTime.getText().trim();
        String typeStr = cmbItemType.getValue();
 

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
        long startPrice; //ktra giá bắt đầu
        try {
            startPrice = Long.parseLong(priceStr);
            if (startPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Giá khởi điểm phải là số nguyên dương.");
            txtStartingPrice.requestFocus();
            return;
        }

        long minIncrement = 0; //ktra bước giá
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
        if (endDate == null) {
            showError("Vui lòng chọn ngày kết thúc.");
            return;
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

  
        ItemData itemData = new ItemData();
        itemData.setItemType(typeStr);
        itemData.setCreator(txtCreator.getText().trim());


        switch (typeStr) {
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
        btnSubmit.setText("Đang tạo...");

        //4.Tạo request DTO
        CreateAuctionRequest reqDTO = new CreateAuctionRequest();
        reqDTO.setName(name);
        reqDTO.setStartPrice(startPrice);
        reqDTO.setEndTime(endTime);
        reqDTO.setStartTime(null);      //chưa lấy start time
        reqDTO.setMinIncrement(minIncrement);
        reqDTO.setItemData(itemData);

        //5.Wrap trong client message
        ClientMessage msg = new ClientMessage("CREATE_AUCTION", reqDTO);
        String JsonRequest = JsonUtils.toJson(msg);
        //6. Gửi qua socket thì gửi thread riêng không gửi luông tong UI thread
        new Thread(() -> {
            boolean isSuccess = false;
            String errorMsg = "Không nhận được phản hồi từ Server";
            AuctionDTO newAuction = null;

            try{
                String jsonResponse = socket.sendAndReceive(JsonRequest);
                //nhận về, nhận cũng ở client message
                if (jsonResponse != null && !jsonResponse.isEmpty()){
                    ClientMessage serverMsg = JsonUtils.fromJson(jsonResponse, ClientMessage.class);
                    if ("CREATE_AUCTION_RESPONSE".equals(serverMsg.getAction())) {
                        String rawData = JsonUtils.toJson(serverMsg.getData());
                        ApiResponse<AuctionDTO> apiResp = JsonUtils.fromJsonGeneric(rawData, ApiResponse.class);
 
                        isSuccess = apiResp.isSuccess();
                        if (isSuccess) {
                            // Double-parse vì Gson đọc data thành LinkedTreeMap
                            String auctionJson = JsonUtils.toJson(apiResp.getData());
                            newAuction = JsonUtils.fromJson(auctionJson, AuctionDTO.class);
                        } else {
                            errorMsg = apiResp.getMessage();
                        }
                    } else {
                        errorMsg = "Action không khớp từ server: " + serverMsg.getAction();
                    }
                }
                
            } catch (Exception e){
                errorMsg = "Lỗi kết nối: " + e.getMessage();
                e.printStackTrace();
            }
            final boolean finalSuccess = isSuccess;
            final String  finalError   = errorMsg;

            //7. cập nhật UI
            Platform.runLater(() -> {
                // Re-enable button dù thành công hay thất bại
                btnSubmit.setDisable(false);
                btnSubmit.setText("Tạo phiên");
                if (finalSuccess) {
                    if (onSuccessCallback != null) onSuccessCallback.run();
                    closeView();
                } else {
                    showError(finalError);
                }
            });

        }).start();
    }

    @FXML
    private void handleCancel() { closeView(); }




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
    private void closeView() {
        if (btnCancel.getScene() != null && btnCancel.getScene().getWindow() != null)
            btnCancel.getScene().getWindow().hide();
    }
}
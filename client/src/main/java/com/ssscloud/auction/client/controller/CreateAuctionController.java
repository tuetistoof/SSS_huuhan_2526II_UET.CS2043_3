package com.ssscloud.auction.client.controller;
 
import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.util.SessionManager;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.util.JsonUtils;
 
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

//TO_DO: chưa làm handleCancle với chuyển màn

public class CreateAuctionController{
    @FXML private TextField  txtTitle;
    @FXML private TextArea   txtDescription;
    @FXML private TextField  txtStartingPrice;
    @FXML private TextField  txtMinIncrement;
    @FXML private DatePicker dpEndDate;
    @FXML private TextField  txtEndTime;          // "HH:mm"
 
    //Feedback 
    @FXML private Label  lblError;
 
    //Buttons 
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;

    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    @FXML
    public void initialize() {
        clearError();
        txtEndTime.setPromptText("HH:mm  (vd: 18:30)");
        txtStartingPrice.setPromptText("VD: 500000");
        txtMinIncrement.setPromptText("VD: 50000 (tuỳ chọn)");
    }

    @FXML 
    private void handleSubmit(){
        clearError();
        
        //1.lấy thông tin
        String title = txtTitle.getText().trim();
        String description = txtDescription.getText().trim();
        String priceStr = txtStartingPrice.getText().trim().replace(".", "").replace(",", "");
        String incrStr = txtMinIncrement.getText().trim().replace(".", "").replace(",", "");
        LocalDate endDate = dpEndDate.getValue();
        String timeStr  = txtEndTime.getText().trim();

        //2.validate
        if (title.isEmpty()) {
            showError("Tên phiên đấu giá không được để trống.");
            txtTitle.requestFocus();
            return;
        }
        if (priceStr.isEmpty()) {
            showError("Giá khởi điểm không được để trống.");
            txtStartingPrice.requestFocus();
            return;
        }
        long startingPrice; //ktra giá bắt đầu
        try {
            startingPrice = Long.parseLong(priceStr);
            if (startingPrice <= 0) throw new NumberFormatException();
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

        //3.Disable nút bấm để tránh bấm nhiều lần
        btnSubmit.setDisable(true);
        btnSubmit.setText("Đang tạo...");

        //4.Tạo request DTO
        CreateAuctionRequest reqDTO = new CreateAuctionRequest();
        reqDTO.setTitle(title);
        reqDTO.setStartingPrice(startingPrice);
        reqDTO.setEndTime(endTime);
        reqDTO.setMinIncrement(minIncrement);

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
                    if ("CREATE_AUCTION".equals(serverMsg.getAction())) {
                        String rawData = JsonUtils.toJson(serverMsg.getData());
                        ApiResponse<AuctionDTO> apiResp =
                                JsonUtils.fromJsonGeneric(rawData, ApiResponse.class);
 
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

            //7. cập nhật UI
            Platform.runLater(() -> {
                // Re-enable button dù thành công hay thất bại
                btnSubmit.setDisable(false);
                btnSubmit.setText("Tạo phiên");
            });

        }).start();

    }




    //Helpers
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
}
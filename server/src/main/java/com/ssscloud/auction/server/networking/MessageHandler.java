package com.ssscloud.auction.server.networking;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.UserController;

import netscape.javascript.JSObject;

public class MessageHandler {
    private Gson gson = new Gson();
    private BidController bidController;
    private UserController userController;
    private AuctionController auctionController;

    public MessageHandler(UserController userController, 
                          AuctionController auctionController, 
                          BidController bidController) {
        this.userController = userController;
        this.auctionController = auctionController;
        this.bidController = bidController;
    }

    public String handleMessage(String jsonMessage) {
        try {
            //Chuyển JSON thành ClientMessage object
            ClientMessage msg = JsonUtils.fromJson(jsonMessage, ClientMessage.class);

            if (msg == null || msg.getAction() == null) {
                System.err.println("Message không hợp lệ từ client ");
                return JsonUtils.toJson(ApiResponse.error("Message không hợp lệ"));
            }

            String action = msg.getAction().toUpperCase().trim();
            String responseJson = null;

            //Dựa vào action để gọi Controller phù hợp
            switch (action) {
                case "LOGIN":
                    responseJson = userController.login(msg.getData());
                    break;

                case "REGISTER":
                    responseJson = userController.register(msg.getData());
                    break;

                case "CREATE_AUCTION":
                    responseJson = auctionController.createAuction(msg.getData());
                    break;

                case "PLACE_BID":
                    responseJson = bidController.placeBid(msg.getData());
                    break;

                case "AUTO_BID":
                    responseJson = bidController.registerAutoBid(msg.getData());
                    break;

                default:
                    responseJson = JsonUtils.toJson(
                    ApiResponse.error("Action không được hỗ trợ: " + action)
                );
            }
            return responseJson != null ? responseJson : "{}";

        } catch (Exception e) {
            System.err.println("Lỗi xử lý message từ client: "  + e.getMessage());
            return JsonUtils.toJson(ApiResponse.error("Lỗi hệ thống: " + e.getMessage()));
        }
    }


}

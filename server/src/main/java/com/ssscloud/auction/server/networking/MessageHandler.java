package com.ssscloud.auction.server.networking;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.UserController;

import netscape.javascript.JSObject;

public class MessageHandler {
    private  Gson gson = new Gson();
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

    public void handleMessage(String clientId, String jsonMessage) {
        try {
            //Chuyển JSON thành ClientMessage object
            ClientMessage msg = JsonUtils.fromJson(jsonMessage, ClientMessage.class);

            if (msg == null || msg.getAction() == null) {
                System.err.println("Message không hợp lệ từ client " + clientId);
                return;
            }

            String action = msg.getAction().toUpperCase().trim();

            //Dựa vào action để gọi Controller phù hợp
            switch (action) {
                case "LOGIN":
                    userController.login(clientId, msg.getData());
                    break;

                case "REGISTER":
                    userController.register(clientId, msg.getData());
                    break;

                case "CREATE_AUCTION":
                    auctionController.createAuction(clientId, msg.getData());
                    break;

                case "PLACE_BID":
                    bidController.placeBid(clientId, msg.getData());
                    break;

                case "AUTO_BID":
                    bidController.registerAutoBid(clientId, msg.getData());
                    break;

                default:
                    System.err.println("Action không được hỗ trợ: " + action + " từ client " + clientId);
            }

        } catch (Exception e) {
            System.err.println("Lỗi xử lý message từ client " + clientId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


}

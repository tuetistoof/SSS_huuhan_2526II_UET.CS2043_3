package com.ssscloud.auction.server.networking;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import netscape.javascript.JSObject;

public class MessageHandler {
    private  Gson gson = new Gson();
    private BidController bidController = new BidController();
    private UserController userController = new UserController();

    public String proccessRequest(String jsonFromClient){
        try {
            JsonObject requestClient = JsonParser.parseString(jsonFromClient).getAsJsonObject();

            String action = requestClient.get("action").getAsString();
            String payload = requestClient.getAsJsonObject("payload");

            switch (action) {
                case "PLACE_BID":
                    BidDTO bidDTO = gson.tojson(payload, BidDTO.class);
                    return bidController.handle(bidDTO);
                case "LOGIN":
                    UserDTO userDTO = gson.tojson(payload, UserDTO.class);
                    return userController.handle(userDTO);
                case "GET_AUTION_LIST":
                    return bidController.getAuctionList();
                default:
                    return createErrorReponse("Hành động không hợp lệ");
            }   
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorReponse("Lỗi cú pháp Json hoặc lỗi hệ thống");
        }
    }

    private createErrorReponse(String errorMessage){
            JsonObject errorReponse = new JSObject();
            errorReponse.addProperty("status", "Error");
            errorMessage.addProperty("message", errorMessage);
            return errorMessage.toString();
    }
}

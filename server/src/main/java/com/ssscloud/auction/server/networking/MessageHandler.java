package com.ssscloud.auction.server.networking;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
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

    public String handleMessage(String jsonMessage, ClientHandler client) {
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
                    ApiResponse<UserDTO> parsed = JsonUtils.fromJsonGeneric(response, UserDTO.class);
                    if (parsed != null && parsed.isSuccess() && parsed.getData() != null) {
                        UserDTO user = parsed.getData();
                        client.setSession(user.getId(), user.getUsername());
                    }
                    return responseJson;

                case "REGISTER":
                    return userController.register(msg.getData());
                    
                case "CREATE_AUCTION":
                    return auctionController.createAuction(msg.getData());

                case "PLACE_BID":
                    String raw = JsonUtils.toJson(msg.getData());
                    PlaceBidRequest req = JsonUtils.fromJson(raw, PlaceBidRequest.class);
                    if (req == null) {
                        return JsonUtils.toJson(ApiResponse.error("Dữ liệu đặt giá không hợp lệ"));
                    }
                    req.setBidderId(client.getUserId());
                    req.setBidderUsername(client.getUsername());
                    return bidController.placeBid(req);

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

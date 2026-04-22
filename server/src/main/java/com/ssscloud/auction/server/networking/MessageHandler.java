package com.ssscloud.auction.server.networking;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.UserController;

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

            //Dựa vào action để gọi Controller phù hợp
            switch (action) {
                case "LOGIN": {
                    // Chay logic login trong server
                    String responseJson = userController.login(msg.getData());

                    Type apiUserType  = new TypeToken<ApiResponse<UserDTO>>(){}.getType();
                    ApiResponse<UserDTO> parsed = JsonUtils.fromJsonGeneric(responseJson, apiUserType );

                    if (parsed != null && parsed.isSuccess() && parsed.getData() != null) {
                        client.setSession(parsed.getData().getId(), parsed.getData().getUsername());
                    }
                    //wrap trong ClientMessage type=RESPONSE để AuctionClientSocket route đúng
                    return JsonUtils.toJson(ClientMessage.request("LOGIN_RESPONSE", parsed));
                }


                case "REGISTER": {
                    // Chay logic register trong server
                    String responseJson = userController.register(msg.getData());

                    Type apiUserType  = new TypeToken<ApiResponse<UserDTO>>(){}.getType();
                    ApiResponse<UserDTO> parsed = JsonUtils.fromJsonGeneric(responseJson, apiUserType );

                    return JsonUtils.toJson(ClientMessage.request("REGISTER_RESPONSE", parsed));
                    
                // case "CREATE_AUCTION":
                //     return JsonUtils.toJson(ClientMessage.request("CREATE_AUCTION_RESPONSE",
                //             JsonUtils.fromJson(auctionController.createAuction(msg.getData()), ApiResponse.class)));
                }

                case "PLACE_BID": {
                    String raw = JsonUtils.toJson(msg.getData());
                    PlaceBidRequest req = JsonUtils.fromJson(raw, PlaceBidRequest.class);
                    if (req == null) {
                        return JsonUtils.toJson(ApiResponse.error("Dữ liệu đặt giá không hợp lệ"));
                    }
                    return JsonUtils.toJson(ClientMessage.request("PLACE_BID_RESPONSE",
                            JsonUtils.fromJson(bidController.placeBid(req, client.getUserId(), client.getUsername()), ApiResponse.class)));
                }
                case "AUTO_BID":{
                    String raw = JsonUtils.toJson(msg.getData());
                    AutoBidRequest req = JsonUtils.fromJson(raw, AutoBidRequest.class);
                    if (req == null) {
                        return JsonUtils.toJson(ApiResponse.error("Dữ liệu đặt giá tự động không hợp lệ"));
                    }
                    return JsonUtils.toJson(ClientMessage.request("AUTO_BID",
                            JsonUtils.fromJson(bidController.placeBid(req, client.getUserId(), client.getUsername()), ApiResponse.class)));
                }
 
                default: {
                    return JsonUtils.toJson(ClientMessage.request("ERROR",
                            ApiResponse.error("Action không được hỗ trợ: " + action)));
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi xử lý message từ client: "  + e.getMessage());
            return JsonUtils.toJson(ClientMessage.request("ERROR",
                    ApiResponse.error("Lỗi hệ thống: " + e.getMessage())));
        }
    }


}

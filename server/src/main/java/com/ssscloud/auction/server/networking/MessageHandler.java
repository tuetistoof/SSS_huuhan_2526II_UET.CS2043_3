package com.ssscloud.auction.server.networking;

import com.google.gson.Gson;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidDTO;
import com.ssscloud.auction.common.dto.ClientMessage;
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
                    String responseJson = userController.login(msg.getData());
                    // Inject session nếu login thành công
                    ApiResponse<UserDTO> parsed = JsonUtils.fromJsonGeneric(responseJson, UserDTO.class);
                    if (parsed != null && parsed.isSuccess() && parsed.getData() != null) {
                        client.setSession(parsed.getData().getId(), parsed.getData().getUsername());
                    }
                    //wrap trong ClientMessage type=RESPONSE để AuctionClientSocket route đúng
                    return JsonUtils.toJson(ClientMessage.request("LOGIN_RESPONSE",
                            JsonUtils.fromJson(responseJson, ApiResponse.class)));
                }


                case "REGISTER":
                    String responseJson = userController.register(msg.getData());
                    ApiResponse<UserDTO> parsed = JsonUtils.fromJsonGeneric(responseJson, UserDTO.class);
                    if (parsed != null && parsed.isSuccess() && parsed.getData() != null) {
                        client.setSession(parsed.getData().getId(), parsed.getData().getUsername());
                    }
                    return JsonUtils.toJson(ClientMessage.request("REGISTER_RESPONSE",
                            JsonUtils.fromJson(userController.register(msg.getData()), ApiResponse.class)));
                    
                // case "CREATE_AUCTION":
                //     return JsonUtils.toJson(ClientMessage.request("CREATE_AUCTION_RESPONSE",
                //             JsonUtils.fromJson(auctionController.createAuction(msg.getData()), ApiResponse.class)));

                case "PLACE_BID":
                    String raw = JsonUtils.toJson(msg.getData());
                    PlaceBidRequest req = JsonUtils.fromJson(raw, PlaceBidRequest.class);
                    if (req == null) {
                        return JsonUtils.toJson(ApiResponse.error("Dữ liệu đặt giá không hợp lệ"));
                    }
                    req.setBidderId(client.getUserId());
                    req.setBidderUsername(client.getUsername());
                    return JsonUtils.toJson(ClientMessage.request("PLACE_BID_RESPONSE",
                            JsonUtils.fromJson(bidController.placeBid(req), ApiResponse.class)));

                // case "AUTO_BID":
                //     return JsonUtils.toJson(ClientMessage.request("AUTO_BID_RESPONSE",
                //             JsonUtils.fromJson(bidController.registerAutoBid(msg.getData()), ApiResponse.class)));
 
                default:
                    return JsonUtils.toJson(ClientMessage.request("ERROR",
                            ApiResponse.error("Action không được hỗ trợ: " + action)));
            }

        } catch (Exception e) {
            System.err.println("Lỗi xử lý message từ client: "  + e.getMessage());
            return JsonUtils.toJson(ClientMessage.request("ERROR",
                    ApiResponse.error("Lỗi hệ thống: " + e.getMessage())));
        }
    }


}

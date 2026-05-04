package com.ssscloud.auction.server.networking;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionListResponse;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.UserController;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.service.AuctionRegistry;

public class MessageHandler {
    private Gson gson = new Gson();
    private BidController bidController;
    private UserController userController;
    private AuctionController auctionController;
    private AuctionDAO auctionDAO;
    public MessageHandler(
            AuctionDAO auctionDAO,
            UserController userController,
            AuctionController auctionController,
            BidController bidController) {
        this.userController = userController;
        this.auctionController = auctionController;
        this.bidController = bidController;
        this.auctionDAO = auctionDAO;
    }

    public String handleMessage(String jsonMessage, ClientHandler client) {
        try {
            // Chuyển JSON thành ClientMessage object
            ClientMessage msg = JsonUtils.fromJson(jsonMessage, ClientMessage.class);

            if (msg == null || msg.getAction() == null) {
                System.err.println("Message không hợp lệ từ client ");
                return JsonUtils.toJson(ApiResponse.error("Message không hợp lệ"));
            }

            String action = msg.getAction().toUpperCase().trim();

            // Dựa vào action để gọi Controller phù hợp
            switch (action) {
                case "LOGIN": {
                    // Chay logic login trong server
                    String responseJson = userController.login(msg.getData());

                    Type apiUserType = new TypeToken<ApiResponse<UserDTO>>() {
                    }.getType();
                    ApiResponse<UserDTO> parsed = JsonUtils.fromJsonGeneric(responseJson, apiUserType);

                    if (parsed != null && parsed.isSuccess() && parsed.getData() != null) {
                        client.setSession(parsed.getData().getId(), parsed.getData().getUsername());
                    }
                    // wrap trong ClientMessage type=RESPONSE để AuctionClientSocket route đúng
                    return JsonUtils.toJson(ClientMessage.request("LOGIN_RESPONSE", parsed));
                }

                case "REGISTER": {
                    // Chay logic register trong server
                    String responseJson = userController.register(msg.getData());

                    Type apiUserType = new TypeToken<ApiResponse<UserDTO>>() {
                    }.getType();
                    ApiResponse<UserDTO> parsed = JsonUtils.fromJsonGeneric(responseJson, apiUserType);

                    return JsonUtils.toJson(ClientMessage.request("REGISTER_RESPONSE", parsed));
                }
                case "CREATE_AUCTION": {
                    // return JsonUtils.toJson(ClientMessage.request("CREATE_AUCTION_RESPONSE",
                    //         JsonUtils.fromJson(auctionController.createAuction(msg.getData(), client.getUserId()),
                    //                 ApiResponse.class)));
                    
                    // 1. Ép chuỗi jsonMessage gốc thành JSON Object, rồi moi cái ruột "data" ra dạng String
                    com.google.gson.JsonObject rootObj = com.google.gson.JsonParser.parseString(jsonMessage).getAsJsonObject();
                    String rawDataJson = rootObj.get("data").toString();

                    // 2. Truyền cái rawDataJson (kiểu String) đó vào controller thay vì msg.getData() (kiểu Object)
                    String controllerResponse = auctionController.createAuction(rawDataJson, client.getUserId());
                    // 3. Đóng gói trả lời lại cho Client
                    Type auctionResponseType = new TypeToken<ApiResponse<AuctionDTO>>() {}.getType();
                    ApiResponse<AuctionDTO> auctionResp = JsonUtils.fromJsonGeneric(controllerResponse, auctionResponseType);
                    return JsonUtils.toJson(ClientMessage.request("CREATE_AUCTION_RESPONSE",auctionResp));
                }

                case "PLACE_BID": {
                    String result = bidController.placeBid(msg.getData(), client.getUserId(), client.getUsername());
                    ApiResponse<?> resp = JsonUtils.fromJson(result, ApiResponse.class);
                    if (!resp.isSuccess()) {
                        // Push lỗi về riêng client này, không broadcast
                        client.getWriter().println(
                                JsonUtils.toJson(ClientMessage.push("BID_ERROR", resp)));
                    }
                    return null;
                }

                case "AUTO_BID": {
                    String raw = JsonUtils.toJson(msg.getData());
                    AutoBidRequest req = JsonUtils.fromJson(raw, AutoBidRequest.class);
                    if (req == null) {
                        return JsonUtils.toJson(ApiResponse.error("Dữ liệu đặt giá tự động không hợplệ"));
                    }
                    return JsonUtils.toJson(ClientMessage.request("AUTO_BID_RESPONSE",
                    JsonUtils.fromJson(bidController.registerAutoBid(req, client.getUserId(),
                    client.getUsername()), ApiResponse.class)));
                }

                case "GET_AUCTIONS": {
                    String result = auctionController.getActiveAuctions();
                    Type auctionListResponseType = new TypeToken<ApiResponse<AuctionListResponse>>() {}.getType();
                    ApiResponse<AuctionListResponse> auctionListResponse = JsonUtils.fromJsonGeneric(result, auctionListResponseType);
                    return JsonUtils.toJson(ClientMessage.request("GET_AUCTIONS_RESPONSE",auctionListResponse));
                }

                case "SUBSCRIBE_AUCTION": {
                    // Client vào BiddingRoom — đăng ký nhận push BID_UPDATE cho auction này
                    String auctionId = JsonUtils.toJson(msg.getData()).replace("\"", "").trim();
                    if (auctionId == null || auctionId.isBlank()) {
                        // Dùng .push() để lỗi đi vào listeners (handleServerPush),
                        client.getWriter().println(JsonUtils.toJson(
                                ClientMessage.push("SUBSCRIBE_ERROR",
                                        ApiResponse.error("Thiếu auctionId"))));
                        return null;
                    }

                    Auction auction = com.ssscloud.auction.server.util.AuctionRegistry.getInstance().getLiveAuction(auctionId);

                    if (auction == null) {
                        client.getWriter().println(JsonUtils.toJson(
                                ClientMessage.push("SUBSCRIBE_ERROR",
                                        ApiResponse.error("Phiên đấu giá không tồn tại: " + auctionId))));
                        return null;
                    }
                    ClientObserver observer = new ClientObserver(client.getWriter(), client.getUserId());
                    ChangeManager.getInstance().attach(auction, observer);
                    System.out.println("[Server] Client " + client.getUserId() + " đã vào phòng auction " + auctionId);
                    return null;
                }

                default: {
                    return JsonUtils.toJson(ClientMessage.request("ERROR",
                            ApiResponse.error("Action không được hỗ trợ: " + action)));
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi xử lý message từ client: " + e.getMessage());
            return JsonUtils.toJson(ClientMessage.request("ERROR",
                    ApiResponse.error("Lỗi hệ thống: " + e.getMessage())));
        }
    }

}

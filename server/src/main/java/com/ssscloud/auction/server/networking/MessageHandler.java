package com.ssscloud.auction.server.networking;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.ResourceBundle.Control;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.dto.response.ListResponse;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.exception.ControllerExceptions;
import com.ssscloud.auction.common.exception.DAOExceptions;
import com.ssscloud.auction.common.exception.ServiceExceptions;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.ItemController;
import com.ssscloud.auction.server.controller.UserController;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.WatchlistDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;
    
public class MessageHandler {
    private Gson gson = new Gson();
    private BidController bidController;
    private UserController userController;
    private AuctionController auctionController;
    private AuctionDAO auctionDAO;
    private ItemController itemController;
    private WatchlistDAO watchlistDAO;

    private static final Logger logger = Logger.getLogger(MessageHandler.class.getName());

    public MessageHandler(
            UserController userController,
            AuctionController auctionController,
            BidController bidController,
            ItemController itemController) {
        this.userController = userController;
        this.auctionController = auctionController;
        this.bidController = bidController;
        this.itemController = itemController;
    }

    public String handleMessage(String jsonMessage, ClientHandler client) {
        logger.info("Received data from client: " + jsonMessage);
        try {
            // Chuyển JSON thành ClientMessage object
            ClientMessage msg = JsonUtils.fromJson(jsonMessage, ClientMessage.class);

            if (msg == null || msg.getAction() == null) {
                logger.warning("Invalid message format from client");
                return JsonUtils.toJson(ApiResponse.error("Invalid message"));
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
                } /*
                case "CREATE_AUCTION": {
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
                    return JsonUtils.toJson(ClientMessage.request("AUTO_BID_RESPONSE",
                    JsonUtils.fromJson(bidController.registerAutoBid(msg.getData(), client.getUserId(),
                    client.getUsername()), ApiResponse.class)));
                }

                case "GET_MY_AUCTIONS": {
                    // Seller lấy danh sách auction của chính mình
                    String result = auctionController.getMyAuctions(client.getUserId());
                    return null;
                }

                case "GET_ACTIVE_AUCTIONS": {
                    String result = auctionController.getActiveAuctions();
                    return JsonUtils.toJson(ClientMessage.request("GET_ACTIVE_AUCTIONS_RESPONSE",
                        JsonUtils.fromJson(result, ApiResponse.class)));
                }

                case "GET_AUCTION_DETAILS": {
                    return JsonUtils.toJson(ClientMessage.request("GET_AUCTION_DETAILS_RESPONSE",
                    JsonUtils.fromJson(auctionController.getAuctionById(msg.getData()), ApiResponse.class)));
                }
                case "GET_BID_HISTORY": {   //lịch sử đặt bid của auction
                    String result = bidController.getBidHistory(msg.getData());
                    com.ssscloud.auction.common.dto.response.ApiResponse<?> resp =
                            com.ssscloud.auction.common.util.JsonUtils.fromJson(result,
                                    com.ssscloud.auction.common.dto.response.ApiResponse.class);
                    return JsonUtils.toJson(ClientMessage.request("GET_BID_HISTORY_RESPONSE", resp));
                }

                case "SUBSCRIBE_AUCTION": {
                    // Client vào BiddingRoom — đăng ký nhận push BID_UPDATE cho auction này
                    String auctionId = JsonUtils.toJson(msg.getData()).replace("\"", "").trim();
                    if (auctionId == null || auctionId.isBlank()) {
                        // Dùng .push() để lỗi đi vào listeners (handleServerPush),
                        client.getWriter().println(JsonUtils.toJson(ClientMessage.push("SUBSCRIBE_ERROR",ApiResponse.error("Thiếu auctionId"))));
                        return null;
                    }

                    Auction auction = AuctionRegistry.getInstance().getLiveAuction(auctionId);

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
                case "FOLLOW_AUCTION": {
                    String auctionId = JsonUtils.toJson(msg.getData()).replace("\"", "").trim();
                    boolean added = watchlistDAO.add(client.getUserId(), auctionId);
                    return JsonUtils.toJson(ClientMessage.request("FOLLOW_RESPONSE",
                        added ? ApiResponse.success(null, "Đã thêm vào Watch List")
                        : ApiResponse.error("Không thể thêm Watch List")));
                }
 
                case "UNFOLLOW_AUCTION": {
                    String auctionId = JsonUtils.toJson(msg.getData()).replace("\"", "").trim();
                    boolean removed = watchlistDAO.remove(client.getUserId(), auctionId);
                    return JsonUtils.toJson(ClientMessage.request("UNFOLLOW_RESPONSE",
                        removed ? ApiResponse.success(null, "Đã xóa khỏi Watch List")
                        : ApiResponse.error("Không thể xóa Watch List")));
                }
 
                case "GET_WATCHLIST": {
                    List<String> auctionIds = watchlistDAO.findAuctionIdsByUser(client.getUserId());
                    return JsonUtils.toJson(ClientMessage.request("GET_WATCHLIST_RESPONSE",
                        ApiResponse.success(auctionIds, "OK")));
                }
 
                case "CHECK_FOLLOWING": {
                    String auctionId = JsonUtils.toJson(msg.getData()).replace("\"", "").trim();
                    boolean following = watchlistDAO.isFollowing(client.getUserId(), auctionId);
                    return JsonUtils.toJson(ClientMessage.request("CHECK_FOLLOWING_RESPONSE",
                        ApiResponse.success(following, "OK")));  
                } */

                case "DEPOSIT": {
                    String result = userController.deposit(msg.getData(), client.getUserId());
                    ApiResponse<?> resp = JsonUtils.fromJson(result, ApiResponse.class);
                    return JsonUtils.toJson(ClientMessage.request("DEPOSIT_RESPONSE", resp));
                }

                default: {
                    return JsonUtils.toJson(ClientMessage.request("ERROR",
                            ApiResponse.error("Unsupported action: " + action)));
                }
            }
            
        } catch (ControllerExceptions e){
            logger.log(Level.WARNING, "Validation error: " + e.getMessage(), e);
            return JsonUtils.toJson(ClientMessage.request("VALIDATE_ERROR", ApiResponse.error(e.getMessage(), e.getErrorCode())));

        } catch (ServiceExceptions e) {
            logger.log(Level.INFO, "Business logic error: " + e.getMessage(), e);
            return JsonUtils.toJson(ClientMessage.request("BUSINESS_ERROR", ApiResponse.error(e.getMessage(), e.getErrorCode())));
            
        } catch (DAOExceptions e){
            logger.log(Level.SEVERE, "Database access error: " + e.getMessage(), e);
            return JsonUtils.toJson(ClientMessage.request("DAO_ERROR", ApiResponse.error("System DAO error. Please try again later.")));
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unknown system error", e);
            return JsonUtils.toJson(ClientMessage.request("UNDEFINED_ERROR", ApiResponse.error("Internal system error: " + e.getMessage())));
        }
    }
}
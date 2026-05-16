package com.ssscloud.auction.server.networking;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.NotificationDTO;
import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.NotificationController;
import com.ssscloud.auction.server.controller.UserController;
import com.ssscloud.auction.server.controller.WatchlistController;
import com.ssscloud.auction.server.controller.BiddedAuctionsListController;
import com.ssscloud.auction.server.util.AuctionRegistry;
    
/**
 * MessageHandler processes incoming JSON payloads from clients and routes them 
 * to the appropriate controllers based on the requested action.
 */
public class MessageHandler {
    private static final Logger logger = Logger.getLogger(MessageHandler.class.getName()); // Logging Standards: First attribute

    private final BidController bidController;
    private final UserController userController;
    private final AuctionController auctionController;
    private final WatchlistController watchlistController;
    private final BiddedAuctionsListController biddedAuctionsListController;
    private final NotificationController notificationController;


    public MessageHandler(
            UserController userController,
            AuctionController auctionController,
            BidController bidController,
            WatchlistController watchlistController,
            BiddedAuctionsListController biddedAuctionsListController,
            NotificationController notificationController) {
        this.userController = userController;
        this.auctionController = auctionController;
        this.bidController = bidController;
        this.watchlistController = watchlistController;
        this.biddedAuctionsListController = biddedAuctionsListController;
        this.notificationController = notificationController;
    }

    // --- PUBLIC METHODS ---

    public String handleMessage(String jsonPayload, ClientHandler clientHandler) {
        logger.log(Level.INFO, "Received data payload from client: " + jsonPayload);

        try {
            // Deserialize incoming JSON into a ClientMessage object
            ClientMessage clientMessage = JsonUtils.fromJson(jsonPayload, ClientMessage.class);

            if (clientMessage == null || clientMessage.getAction() == null) {
                logger.log(Level.WARNING, "Terminating request: Invalid message format received.");
                return JsonUtils.toJson(ApiResponse.error("Invalid message format."));
            }

            String messageAction = clientMessage.getAction().toUpperCase().trim();

            // Route to the appropriate controller based on the action identifier
            switch (messageAction) {
                case "LOGIN": {
                    String controllerResponse = userController.login(clientMessage.getData());

                    Type apiUserType = new TypeToken<ApiResponse<UserDTO>>() {}.getType();
                    ApiResponse<UserDTO> loginResult = JsonUtils.fromJsonGeneric(controllerResponse, apiUserType);

                    if (loginResult != null && loginResult.isSuccess() && loginResult.getData() != null) {
                        clientHandler.setSession(loginResult.getData().getId(), loginResult.getData().getUsername());
                    }
                    return JsonUtils.toJson(ClientMessage.request("LOGIN_RESPONSE", loginResult));
                }
                case "GET_PENDING_NOTIFICATIONS": {
                    String pendingJson = notificationController.getPendingNotifications(clientHandler.getUserId());
                    Type pendingType = new TypeToken<ApiResponse<List<NotificationDTO>>>() {}.getType();
                    ApiResponse<List<NotificationDTO>> pendingResult = JsonUtils.fromJsonGeneric(pendingJson, pendingType);
                    return JsonUtils.toJson(ClientMessage.request("GET_PENDING_NOTIFICATIONS_RESPONSE", pendingResult));
                }


                case "REGISTER": {
                    String controllerResponse = userController.register(clientMessage.getData());

                    Type apiUserType = new TypeToken<ApiResponse<UserDTO>>() {}.getType();
                    ApiResponse<UserDTO> registrationResult = JsonUtils.fromJsonGeneric(controllerResponse, apiUserType);

                    return JsonUtils.toJson(ClientMessage.request("REGISTER_RESPONSE", registrationResult));
                } 

                case "CREATE_AUCTION": {
                    // com.google.gson.JsonObject rootObject = com.google.gson.JsonParser.parseString(jsonPayload).getAsJsonObject();
                    // String internalJsonPayload = rootObject.get("data").toString(); // Parse inner data as raw JSON payload
                    
                    // String controllerResponse = auctionController.createAuction(internalJsonPayload, clientHandler.getUserId());
                    String controllerResponse = auctionController.createAuction(clientMessage.getData(), clientHandler.getUserId());

                    Type auctionResponseType = new TypeToken<ApiResponse<AuctionDTO>>() {}.getType();
                    ApiResponse<AuctionDTO> auctionResult = JsonUtils.fromJsonGeneric(controllerResponse, auctionResponseType);

                    return JsonUtils.toJson(ClientMessage.request("CREATE_AUCTION_RESPONSE", auctionResult));
                }

                case "PLACE_BID": {
                    String controllerResponse = bidController.placeBid(clientMessage.getData(), clientHandler.getUserId(), clientHandler.getUsername());
                    ApiResponse<?> bidResult = JsonUtils.fromJson(controllerResponse, ApiResponse.class);
                    if (!bidResult.isSuccess()) {
                        clientHandler.getWriter().println(JsonUtils.toJson(ClientMessage.push("BID_ERROR", bidResult)));
                    }
                    return null;
                }

                case "AUTO_BID": {
                    String controllerResponse = bidController.registerAutoBid(clientMessage.getData(), clientHandler.getUserId(), clientHandler.getUsername());
                    return JsonUtils.toJson(ClientMessage.request("AUTO_BID_RESPONSE", JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }

                case "GET_MY_AUCTIONS": {
                    String controllerResponse = auctionController.getMyAuctions(clientHandler.getUserId());
                    return JsonUtils.toJson(ClientMessage.request("GET_MY_AUCTIONS_RESPONSE", JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }

                case "GET_ACTIVE_AUCTIONS": {
                    String controllerResponse = auctionController.getActiveAuctions();
                    return JsonUtils.toJson(ClientMessage.request("GET_ACTIVE_AUCTIONS_RESPONSE", JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }

                case "GET_AUCTION_DETAILS": {
                    String controllerResponse = auctionController.getAuctionById(clientMessage.getData());
                    return JsonUtils.toJson(ClientMessage.request("GET_AUCTION_DETAILS_RESPONSE", JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }

                case "GET_BID_HISTORY": {
                    String controllerResponse = bidController.getBidHistory(clientMessage.getData());
                    return JsonUtils.toJson(ClientMessage.request("GET_BID_HISTORY_RESPONSE", JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }

                case "SUBSCRIBE_AUCTION": {
                    String auctionId = JsonUtils.toJson(clientMessage.getData()).replace("\"", "").trim();
                    if (auctionId == null || auctionId.isBlank()) {
                        clientHandler.getWriter().println(JsonUtils.toJson(ClientMessage.push("SUBSCRIBE_ERROR", ApiResponse.error("The auctionId identifier is missing."))));
                        return null;
                    }

                    Auction liveAuctionEntity = AuctionRegistry.getInstance().getLiveAuction(auctionId);
                    if (liveAuctionEntity == null) {
                        clientHandler.getWriter().println(JsonUtils.toJson(ClientMessage.push("SUBSCRIBE_ERROR", ApiResponse.error("The specified auction does not exist: " + auctionId))));
                        return null;
                    }

                    ClientObserver clientObserver = new ClientObserver(clientHandler.getWriter(), clientHandler.getUserId());
                    ChangeManager.getInstance().attach(liveAuctionEntity, clientObserver);
                    logger.log(Level.INFO, "[Server] ClientHandler for userId: " + clientHandler.getUserId() + " subscribed to auctionId: " + auctionId);
                    return null;
                }

                case "UNSUBSCRIBE_AUCTION": {
                    String auctionId = JsonUtils.toJson(clientMessage.getData()).replace("\"", "").trim();
                    if (auctionId != null && !auctionId.isBlank()) {
                        Auction liveAuctionEntity = AuctionRegistry.getInstance().getLiveAuction(auctionId);
                        if (liveAuctionEntity != null) {
                            ChangeManager.getInstance().detachByClientId(liveAuctionEntity, clientHandler.getUserId());
                            logger.log(Level.INFO, "[Server] ClientHandler for userId: " + clientHandler.getUserId() + " unsubscribed from auctionId: " + auctionId);
                        }
                    }
                    return null;
                }
                
                case "FOLLOW_AUCTION": {
                    String auctionId = JsonUtils.toJson(clientMessage.getData()).replace("\"", "").trim();
                    return JsonUtils.toJson(ClientMessage.request("FOLLOW_RESPONSE",
                        JsonUtils.fromJson(watchlistController.follow(auctionId, clientHandler.getUserId()), ApiResponse.class)));
                }
 
                case "UNFOLLOW_AUCTION": {
                    String auctionId = JsonUtils.toJson(clientMessage.getData()).replace("\"", "").trim();
                    return JsonUtils.toJson(ClientMessage.request("UNFOLLOW_RESPONSE",
                        JsonUtils.fromJson(watchlistController.unfollow(auctionId, clientHandler.getUserId()), ApiResponse.class)));
                }
 
                case "GET_WATCHLIST": {
                    return JsonUtils.toJson(ClientMessage.request("GET_WATCHLIST_RESPONSE",
                        JsonUtils.fromJson(watchlistController.getWatchlist(clientHandler.getUserId()), ApiResponse.class)));
                }

                case "GET_BIDDED_AUCTIONS": {
                    return JsonUtils.toJson(ClientMessage.request("GET_BIDDED_AUCTIONS_RESPONSE",
                        JsonUtils.fromJson(biddedAuctionsListController.getBiddedAuctionslist(clientHandler.getUserId()), ApiResponse.class)));
                }


                case "CHECK_FOLLOWING": {
                    String auctionId = JsonUtils.toJson(clientMessage.getData()).replace("\"", "").trim();
                    return JsonUtils.toJson(ClientMessage.request("CHECK_FOLLOWING_RESPONSE",
                        JsonUtils.fromJson(watchlistController.checkFollowing(auctionId, clientHandler.getUserId()), ApiResponse.class)));
                }

                case "DEPOSIT": {
                    String controllerResponse = userController.deposit(clientMessage.getData(), clientHandler.getUserId());
                    ApiResponse<?> depositResult = JsonUtils.fromJson(controllerResponse, ApiResponse.class);
                    return JsonUtils.toJson(ClientMessage.request("DEPOSIT_RESPONSE", depositResult));
                }

                default: {
                    return JsonUtils.toJson(ClientMessage.request("ERROR", ApiResponse.error("Unsupported network action: " + messageAction)));
                }
            }
            
        } catch (ControllerException controllerException) {
            logger.log(Level.WARNING, "Controller validation failure: " + controllerException.getMessage(), controllerException);
            return JsonUtils.toJson(ClientMessage.request("VALIDATE_ERROR", ApiResponse.error(controllerException.getMessage(), controllerException.getErrorCode())));

        } catch (ServiceException serviceException) {
            logger.log(Level.WARNING, "Service execution failure: " + serviceException.getMessage(), serviceException);
            return JsonUtils.toJson(ClientMessage.request("BUSINESS_ERROR", ApiResponse.error(serviceException.getMessage(), serviceException.getErrorCode())));
            
        } catch (DAOException daoException) {
            logger.log(Level.SEVERE, "Persistence layer failure: " + daoException.getMessage(), daoException);
            return JsonUtils.toJson(ClientMessage.request("DAO_ERROR", ApiResponse.error("Database access error. Please contact the administrator.")));
            
        } catch (Exception genericException) {
            logger.log(Level.SEVERE, "Unexpected system error: " + genericException.getMessage(), genericException);
            return JsonUtils.toJson(ClientMessage.request("UNDEFINED_ERROR", ApiResponse.error("Internal system error: " + genericException.getMessage())));
        }
    }
}
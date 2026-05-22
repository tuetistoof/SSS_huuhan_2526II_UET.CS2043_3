package com.ssscloud.auction.server.networking;

import com.google.gson.reflect.TypeToken;

import java.io.PrintWriter;
import java.lang.reflect.Type;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.response.DTO.AuctionDTO;
import com.ssscloud.auction.common.payload.response.DTO.UserDTO;
import com.ssscloud.auction.common.payload.response.request.ApiResponse;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.controller.AdminController;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.NotificationController;
import com.ssscloud.auction.server.controller.QueryController;
import com.ssscloud.auction.server.controller.UserController;
import com.ssscloud.auction.server.util.AuctionRegistry;
import com.ssscloud.auction.server.util.SessionRegistry;

/**
 * MessageHandler processes incoming JSON payloads from clients and routes them
 * to the appropriate controllers based on the requested action.
 */
public class MessageHandler {
    private static final Logger logger = Logger.getLogger(MessageHandler.class.getName()); // Logging Standards: First attribute

    private final BidController bidController;
    private final UserController userController;
    private final AuctionController auctionController;
    private final QueryController queryController;
    private final NotificationController notificationController;
    private final AdminController adminController;

    public MessageHandler(
            UserController userController,
            AuctionController auctionController,
            BidController bidController,
            QueryController queryController,
            NotificationController notificationController,
            AdminController adminController) {
        this.userController = userController;
        this.auctionController = auctionController;
        this.bidController = bidController;
        this.queryController = queryController;
        this.notificationController = notificationController;
        this.adminController = adminController;
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
                        String incomingUserId = loginResult.getData().getId();
                        if (SessionRegistry.getInstance().isOnline(incomingUserId)) {
                            PrintWriter oldWriter = SessionRegistry.getInstance().getWriter(incomingUserId);
                            if (oldWriter != null) {
                                synchronized (oldWriter) {
                                    oldWriter.println(JsonUtils.toJson(
                                        ClientMessage.push("SESSION_KICKED", "Tài khoản của bạn đã đăng nhập ở nơi khác.")
                                    ));
                                    oldWriter.flush();
                                }
                            }
                            SessionRegistry.getInstance().unregister(incomingUserId);
                        }
                        long unsettledBalance = 0L;
                        try {
                            UserRole role = loginResult.getData().getRole();
                            unsettledBalance = userController.getUnsettledBalance(incomingUserId, role);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Failed to load unsettledBalance for userId: " + incomingUserId, e);
                        }
                        
                        clientHandler.setSession(incomingUserId, loginResult.getData().getUsername(), unsettledBalance);
                    }
                    return JsonUtils.toJson(ClientMessage.request("LOGIN_RESPONSE", loginResult));
                }

                case "REGISTER": {
                    String controllerResponse = userController.register(clientMessage.getData());

                    Type apiUserType = new TypeToken<ApiResponse<UserDTO>>() {}.getType();
                    ApiResponse<UserDTO> registrationResult = JsonUtils.fromJsonGeneric(controllerResponse, apiUserType);

                    return JsonUtils.toJson(ClientMessage.request("REGISTER_RESPONSE", registrationResult));
                }

                case "GET_PENDING_NOTIFICATIONS": {
                    String controllerResponse = notificationController.getPendingNotifications(clientHandler.getUserId());
                    return JsonUtils.toJson(ClientMessage.request("GET_PENDING_NOTIFICATIONS_RESPONSE",
                        JsonUtils.fromJson(controllerResponse, ApiResponse.class)
                    ));
                }

                case "CREATE_AUCTION": {
                    String controllerResponse = auctionController.createAuction(clientMessage.getData(), clientHandler.getUserId());
                    Type auctionResponseType = new TypeToken<ApiResponse<AuctionDTO>>() {}.getType();
                    ApiResponse<AuctionDTO> auctionResult = JsonUtils.fromJsonGeneric(controllerResponse, auctionResponseType);

                    return JsonUtils.toJson(ClientMessage.request("CREATE_AUCTION_RESPONSE", auctionResult));
                }

                case "PLACE_BID": {
                    String controllerResponse = bidController.placeBid(clientMessage.getData(), clientHandler.getUserId(), clientHandler.getUsername());
                    ApiResponse<?> bidResult = JsonUtils.fromJson(controllerResponse, ApiResponse.class);
                    if (!bidResult.isSuccess()) {
                        clientHandler.write(JsonUtils.toJson(ClientMessage.push("BID_ERROR", bidResult)));
                    }
                    return null;
                }

                case "AUTO_BID": {
                    String controllerResponse = bidController.registerAutoBid(clientMessage.getData(), clientHandler.getUserId(), clientHandler.getUsername());
                    return JsonUtils.toJson(ClientMessage.request("AUTO_BID_RESPONSE", JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }

                case "CANCEL_AUTOBID": {
                    String controllerResponse = bidController.cancelAutoBid(clientMessage.getData(), clientHandler.getUserId(), clientHandler.getUsername());
                    return JsonUtils.toJson(ClientMessage.request("CANCEL_AUTOBID_RESPONSE", JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }
                
                // case "GET_AUTOBID_STATUS": {
                //     String controllerResponse = bidController.getAutoBidStatus(clientMessage.getData(), clientHandler.getUserId());
                //     return JsonUtils.toJson(ClientMessage.request("GET_AUTOBID_STATUS_RESPONSE", JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                // }


                case "GET_MY_AUCTIONS": {
                    String controllerResponse = queryController.getMyAuctions(clientHandler.getUserId());
                    return JsonUtils.toJson(ClientMessage.request("GET_MY_AUCTIONS_RESPONSE", JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }

                case "GET_ACTIVE_AUCTIONS": {
                    String controllerResponse = queryController.getActiveAuctions();
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
                        return JsonUtils.toJson(ClientMessage.request("SUBSCRIBE_ERROR", ApiResponse.error("The auctionId identifier is missing.")));
                    }
                    auctionController.ensureLiveAuctionLoaded(auctionId);
                    Auction liveAuctionEntity = AuctionRegistry.getInstance().getLiveAuction(auctionId);

                    if (liveAuctionEntity == null) {
                    // null sau ensureLiveAuctionLoaded = auction FINISHED/CANCELED hoặc không tồn tại
                        return JsonUtils.toJson(ClientMessage.request("SUBSCRIBE_ERROR", ApiResponse.error("Auction is not active: " + auctionId)));
                    }
                    
                    // Double-check trạng thái RAM — phòng closeAuction() chạy đúng lúc này
                    if (liveAuctionEntity.getStatus().isEnded()) {
                        return JsonUtils.toJson(ClientMessage.request("SUBSCRIBE_ERROR", ApiResponse.error("Auction has just ended: " + auctionId)));}

                    String userId = clientHandler.getUserId();
                    if (!ChangeManager.getInstance().hasObserver(liveAuctionEntity, userId)) {
                        ClientObserver clientObserver = new ClientObserver(clientHandler.getWriter(), userId);
                        ChangeManager.getInstance().attach(liveAuctionEntity, clientObserver);
                        logger.log(Level.INFO, "ClientHandler for userId: " + userId + " subscribed to auctionId: " + auctionId);
                    }
                    logger.log(Level.INFO, "[Server] ClientHandler for userId: " + clientHandler.getUserId() + " subscribed to auctionId: " + auctionId);
                    return JsonUtils.toJson(ClientMessage.request("SUBSCRIBE_OK", null));
                }
                case "UNSUBSCRIBE_AUCTION": {
                    String auctionId = JsonUtils.toJson(clientMessage.getData()).replace("\"", "").trim();
                    Auction liveAuctionEntity = AuctionRegistry.getInstance().getLiveAuction(auctionId);
                    if (liveAuctionEntity != null) {
                        ChangeManager.getInstance()
                                .detachByClientId(liveAuctionEntity, clientHandler.getUserId())
                                .forEach(o -> {
                                    if (o instanceof ClientObserver co) co.shutdown();
                                });
                        if (ChangeManager.getInstance().observerCount(liveAuctionEntity) == 0) {
                            AuctionRegistry.getInstance().remove(auctionId);
                        }
                        logger.log(Level.INFO, "ClientHandler for userId: " + clientHandler.getUserId()
                                + " unsubscribed from auctionId: " + auctionId);
                    }
                    return null;
                }


                case "FOLLOW_AUCTION": {
                    String auctionId = JsonUtils.toJson(clientMessage.getData()).replace("\"", "").trim();
                    return JsonUtils.toJson(ClientMessage.request("FOLLOW_RESPONSE",
                        JsonUtils.fromJson(queryController.follow(auctionId, clientHandler.getUserId()), ApiResponse.class)));
                }

                case "UNFOLLOW_AUCTION": {
                    String auctionId = JsonUtils.toJson(clientMessage.getData()).replace("\"", "").trim();
                    return JsonUtils.toJson(ClientMessage.request("UNFOLLOW_RESPONSE",
                        JsonUtils.fromJson(queryController.unfollow(auctionId, clientHandler.getUserId()), ApiResponse.class)));
                }

                case "GET_WATCHLIST": {
                    return JsonUtils.toJson(ClientMessage.request("GET_WATCHLIST_RESPONSE",
                        JsonUtils.fromJson(queryController.getWatchlist(clientHandler.getUserId()), ApiResponse.class)));
                }

                case "GET_BIDDED_AUCTIONS": {
                    return JsonUtils.toJson(ClientMessage.request("GET_BIDDED_AUCTIONS_RESPONSE",
                        JsonUtils.fromJson(queryController.getBiddedAuctionsList(clientHandler.getUserId()), ApiResponse.class)));
                }

                case "GET_WON_ITEMS": {
                    return JsonUtils.toJson(ClientMessage.request("GET_WON_ITEMS_RESPONSE",
                        JsonUtils.fromJson(queryController.getWonItemsList(clientHandler.getUserId()), ApiResponse.class)));
                }

                case "CHECK_FOLLOWING": {
                    String auctionId = JsonUtils.toJson(clientMessage.getData()).replace("\"", "").trim();
                    return JsonUtils.toJson(ClientMessage.request("CHECK_FOLLOWING_RESPONSE",
                        JsonUtils.fromJson(queryController.checkFollowing(auctionId, clientHandler.getUserId()), ApiResponse.class)));
                }

                case "DEPOSIT": {
                    String controllerResponse = userController.deposit(clientMessage.getData(), clientHandler.getUserId());
                    ApiResponse<?> depositResult = JsonUtils.fromJson(controllerResponse, ApiResponse.class);
                    return JsonUtils.toJson(ClientMessage.request("DEPOSIT_RESPONSE", depositResult));
                }

                // ══════════════════════════════════════════════════════
                // ADMIN ACTIONS
                // ══════════════════════════════════════════════════════

                case "ADMIN_GET_USERS": {
                    // data: String role filter ("BIDDER" | "SELLER" | null)
                    String controllerResponse = adminController.getUsers(clientMessage.getData());
                    return JsonUtils.toJson(ClientMessage.request("ADMIN_GET_USERS_RESPONSE",
                        JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }

                case "ADMIN_GET_AUCTIONS": {
                    // data: String status filter ("RUNNING" | "OPEN" | "FINISHED" | "CANCELED" | null)
                    String controllerResponse = adminController.getAuctions(clientMessage.getData());
                    return JsonUtils.toJson(ClientMessage.request("ADMIN_GET_AUCTIONS_RESPONSE",
                        JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }

                case "ADMIN_GET_METRICS": {
                    // data: (không cần)
                    String controllerResponse = adminController.getMetrics();
                    return JsonUtils.toJson(ClientMessage.request("ADMIN_GET_METRICS_RESPONSE",
                        JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
                }

                case "ADMIN_CANCEL_AUCTION": {
                    // data: { "auctionId": "...", "reason": "..." }
                    String controllerResponse = adminController.cancelAuction(clientMessage.getData());
                    return JsonUtils.toJson(ClientMessage.request("ADMIN_CANCEL_AUCTION_RESPONSE",
                        JsonUtils.fromJson(controllerResponse, ApiResponse.class)));
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
            return JsonUtils.toJson(ClientMessage.request("DAO_ERROR", ApiResponse.error("Database access error. Please contact the administrator.", daoException.getErrorCode())));

        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database connection/SQL execution failure: " + sqlException.getMessage(), sqlException);
            return JsonUtils.toJson(ClientMessage.request("SQL_ERROR", ApiResponse.error("Database error occurred. Please try again later.", ErrorCode.DATABASE_ERROR)));

        } catch (Exception genericException) {
            logger.log(Level.SEVERE, "Unexpected system error: " + genericException.getMessage(), genericException);
            return JsonUtils.toJson(ClientMessage.request("UNDEFINED_ERROR", ApiResponse.error("Internal system error: " + genericException.getMessage(), ErrorCode.GENERAL_ERROR)));
        }
    }
}

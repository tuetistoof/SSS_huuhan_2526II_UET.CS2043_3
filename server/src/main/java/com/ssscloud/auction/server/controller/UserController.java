package com.ssscloud.auction.server.controller;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.request.LoginRequest;
import com.ssscloud.auction.common.dto.request.RegisterRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;
import com.ssscloud.auction.common.dto.response.SellerDisplayDTO;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.exception.ControllerException;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.DisplayDAO;
import com.ssscloud.auction.server.service.UserService;

public class UserController {
    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    private final UserService userService; // Dependency Injection: Short name for Service
    public UserController(UserService userService) {
        this.userService = userService;
    }

    public String login(Object rawRequest) throws ControllerException, Exception {
        try {
            String jsonPayload = JsonUtils.toJson(rawRequest); // Internal Logic: jsonPayload
            LoginRequest loginRequest = JsonUtils.fromJson(jsonPayload, LoginRequest.class); // Request Mapping: loginRequest

            validateLoginRequest(loginRequest); // Validation: validateLoginRequest

            UserDTO userDto = userService.login(loginRequest); // DTOs: userDto
            return JsonUtils.toJson(ApiResponse.success(userDto, "User logged in successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error during user login process.", exception);
            throw exception;
        }
    }


    public String register(Object rawRequest) throws ControllerException, Exception {
        try {
            String jsonPayload = JsonUtils.toJson(rawRequest); // Internal Logic: jsonPayload
            RegisterRequest registerRequest = JsonUtils.fromJson(jsonPayload, RegisterRequest.class); // Request Mapping: registerRequest

            validateRegisterRequest(registerRequest); // Validation: validateRegisterRequest
            UserDTO userDto = userService.register(registerRequest); // DTOs: userDto
            return JsonUtils.toJson(ApiResponse.success(userDto, "User registered successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error during user registration process.", exception);
            throw exception;
        }
    }

    public String deposit(Object rawRequest, String userId) throws ControllerException, Exception {
        try {
            logger.log(Level.INFO, "Processing deposit request for userId: {0}", userId); // Logging Standards: Level.INFO
            String jsonPayload = JsonUtils.toJson(rawRequest).replace("\"", "").trim(); // Internal Logic: jsonPayload
            long depositAmount = (long) Double.parseDouble(jsonPayload); // Naming Convention: depositAmount
            logger.log(Level.INFO, "Parsed deposit amount: {0}", depositAmount); // Logging Standards: Level.INFO
            validateDepositRequest(depositAmount); // Validation: validateDepositRequest
            long newBalance = userService.deposit(depositAmount, userId); // DTOs: newBalance
            return JsonUtils.toJson(ApiResponse.success(newBalance, "Funds deposited successfully."));
        } catch (ControllerException controllerException) {
            throw controllerException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unhandled system error during deposit processing.", exception);
            throw exception;
        }
    }


    private void validateLoginRequest(LoginRequest loginRequest){ // Validation: validateLoginRequest
        if (loginRequest == null)
            throw new ControllerException(ErrorCode.INVALID_LOGIN_REQUEST, "The login request payload cannot be null."); // Language Policy: English
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isBlank())
            throw new ControllerException(ErrorCode.INVALID_LOGIN_REQUEST, "The username field is required and cannot be empty."); // Language Policy: English
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isBlank())
            throw new ControllerException(ErrorCode.INVALID_LOGIN_REQUEST, "The password field is required and cannot be empty."); // Language Policy: English
    }

     private void validateRegisterRequest(RegisterRequest registerRequest){ // Validation: validateRegisterRequest
        if (registerRequest == null)
            throw new ControllerException(ErrorCode.INVALID_REGISTER_REQUEST, "The registration request payload cannot be null."); // Language Policy: English
        if (registerRequest.getUsername() == null || registerRequest.getUsername().isBlank())
            throw new ControllerException(ErrorCode.INVALID_USERNAME, "The username field is required and cannot be empty."); // Language Policy: English
        if (registerRequest.getPassword() == null || registerRequest.getPassword().isBlank())
            throw new ControllerException(ErrorCode.INVALID_PASSWORD, "The password field is required and cannot be empty."); // Language Policy: English
        if (registerRequest.getEmail() == null || registerRequest.getEmail().isBlank())
            throw new ControllerException(ErrorCode.INVALID_EMAIL, "The email address is required and cannot be empty."); // Language Policy: English
        if (registerRequest.getRole() == null)
            throw new ControllerException(ErrorCode.UNDEFINED_ROLE, "The user role must be specified."); // Language Policy: English

        if (registerRequest.getUsername().length() < 3 || registerRequest.getUsername().length() > 20)
            throw new ControllerException(ErrorCode.INVALID_LENGTH_USERNAME, "The username must be between 3 and 20 characters in length."); // Language Policy: English
        if (registerRequest.getPassword().length() < 6 || registerRequest.getPassword().length() > 100)
            throw new ControllerException(ErrorCode.INVALID_LENGTH_PASSWORD, "The password must be between 6 and 100 characters in length."); // Language Policy: English
        if (!registerRequest.getEmail().contains("@")) 
            throw new ControllerException(ErrorCode.INVALID_EMAIL, "The provided email address format is invalid."); // Language Policy: English
    }

    private void validateDepositRequest(long depositAmount){ // Validation: validateDepositRequest
        if (depositAmount <= 0)
            throw new ControllerException(ErrorCode.INVALID_DEPOSIT, "The deposit amount must be a positive value greater than zero."); // Language Policy: English
    }

    private void validateGetMyAuctionsRequest(String sellerId) {
        if (sellerId == null || sellerId.isBlank())
            throw new ControllerException(ErrorCode.INVALID_DATA, "The seller identifier is mandatory to retrieve auctions.");
    }
}
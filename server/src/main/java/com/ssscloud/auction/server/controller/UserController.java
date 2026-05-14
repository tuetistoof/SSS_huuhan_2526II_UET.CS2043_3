package com.ssscloud.auction.server.controller;

import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.request.LoginRequest;
import com.ssscloud.auction.common.dto.request.RegisterRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.exception.ControllerExceptions;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.service.UserService;

public class UserController {
    private final UserService userService;
    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public String login(Object data) throws Exception {
        String dataJsonString = JsonUtils.toJson(data);
        LoginRequest req = JsonUtils.fromJson(dataJsonString, LoginRequest.class);

        validateLoginRequest(req);

        UserDTO dto = userService.login(req);
        return JsonUtils.toJson(ApiResponse.success(dto, "Login successful"));
    }

    public String register(Object data) throws Exception {
        String dataJsonString = JsonUtils.toJson(data);
        RegisterRequest req = JsonUtils.fromJson(dataJsonString, RegisterRequest.class);

        validateRegisterRequest(req);
        UserDTO dto = userService.register(req);
        return JsonUtils.toJson(ApiResponse.success(dto, "Registration successful"));
    }

    public String deposit(Object data, String userId) throws Exception {
        logger.info(">>> DEPOSIT HIT: userId=" + userId);
        String amountStr = JsonUtils.toJson(data).replace("\"", "").trim();
        long amount = (long) Double.parseDouble(amountStr);
        logger.info(">>> parsed amount: " + amount);
        validateDepositRequest(amount);
        long newBalance = userService.deposit(amount, userId);
        return JsonUtils.toJson(ApiResponse.success(newBalance, "Deposit successful"));
    }

    private void validateLoginRequest(LoginRequest req){
        if (req == null)
            throw new ControllerExceptions("INVALID_LOGIN_REQUEST", "LoginRequest cannot be null");
        if (req.getUsername() == null || req.getUsername().isBlank())
            throw new ControllerExceptions("INVALID_LOGIN_REQUEST", "Username cannot be null or empty");
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw new ControllerExceptions("INVALID_LOGIN_REQUEST", "Password cannot be null or empty");
    }

     private void validateRegisterRequest(RegisterRequest req){
        if (req == null)
            throw new ControllerExceptions("INVALID_REGISTER_REQUEST", "RegisterRequest cannot be null");
        if (req.getUsername() == null || req.getUsername().isBlank())
            throw new ControllerExceptions("INVALID_USERNAME", "Username cannot be null or empty");
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw new ControllerExceptions("INVALID_PASSWORD", "Password cannot be null or empty");
        if (req.getEmail() == null || req.getEmail().isBlank())
            throw new ControllerExceptions("INVALID_EMAIL", "Email cannot be null or empty");
        if (req.getRole() == null)
            throw new ControllerExceptions("UNDEFINED_ROLE", "Role cannot be null");

        if (req.getUsername().length() < 3 || req.getUsername().length() > 20)
            throw new ControllerExceptions("INVALID_LENGTH_USERNAME", "Username must be between 3 and 20 characters long");
        if (req.getPassword().length() < 6 || req.getPassword().length() > 100)
            throw new ControllerExceptions("INVALID_LENGTH_PASSWORD", "Password must be between 6 and 100 characters long");
        if (!req.getEmail().contains("@")) 
            throw new ControllerExceptions("INVALID_EMAIL", "Invalid email format");
    }

    private void validateDepositRequest(long amount){
        if (amount <= 0)
            throw new ControllerExceptions("INVALID_DEPOSIT", "Deposit amount must be greater than 0");
    }
}
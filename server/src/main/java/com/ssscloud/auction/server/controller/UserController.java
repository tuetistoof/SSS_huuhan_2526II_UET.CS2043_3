package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.common.dto.request.LoginRequest;
import com.ssscloud.auction.common.dto.request.RegisterRequest;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.Seller;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.common.util.JsonUtils;
import com.ssscloud.auction.server.dao.UserDAO;

public class UserController {
    private UserDAO userDAO;

    public UserController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public String login(Object data) {
        try {
            String dataJsonString = JsonUtils.toJson(data);
            LoginRequest request = JsonUtils.fromJson(dataJsonString, LoginRequest.class);

            User user = userDAO.findByUsername(request.getUsername());

            if (user == null) {
                return JsonUtils.toJson(ApiResponse.error("Account doesn't exist"));
            }

            if (!user.getPassword().equals(request.getPassword())) {
                return JsonUtils.toJson(ApiResponse.error("Wrong password"));
            }
            UserDTO dto = new UserDTO(
                    user.getId(),
                    user.getUserName(),
                    user.getEmail(),
                    user.getRole());
            return JsonUtils.toJson(ApiResponse.success(dto, "Login successful"));
        } catch (Exception e) {
            e.printStackTrace();
            return JsonUtils.toJson(ApiResponse.error("Server error: " + e.getMessage()));
        }
    }

    public String register(Object data) {
        try {
            String dataJsonString = JsonUtils.toJson(data);
            RegisterRequest request = JsonUtils.fromJson(dataJsonString, RegisterRequest.class);

            User user = userDAO.findByUsername(request.getUsername());

            if (user == null) {
                if (request.getRole().isBidder()) {
                    user = new Bidder(
                            request.getName(),
                            request.getUsername(),
                            request.getPassword(),
                            request.getEmail(),
                            request.getRole());
                    userDAO.saveBidder((Bidder) user);
                } else {
                    user = new Seller(
                            request.getName(),
                            request.getUsername(),
                            request.getPassword(),
                            request.getEmail(),
                            request.getRole(),
                            request.getBankAccount());
                    userDAO.saveSeller((Seller) user);
                }
                UserDTO dto = new UserDTO(
                        user.getId(),
                        user.getUserName(),
                        user.getEmail(),
                        user.getRole());
                return JsonUtils.toJson(ApiResponse.success(dto, "Register successful"));
            } else {
                return JsonUtils.toJson(ApiResponse.error("Account is exist"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return JsonUtils.toJson(ApiResponse.error("Server error: " + e.getMessage()));
        }
    }
}

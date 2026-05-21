package com.ssscloud.auction.client.util;

import com.ssscloud.auction.common.payload.response.DTO.UserDTO;

public class SessionManager {
    private static final SessionManager instance = new SessionManager();
    private volatile UserDTO currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return instance;
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserDTO user) {
        this.currentUser = user;
    }

    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

}

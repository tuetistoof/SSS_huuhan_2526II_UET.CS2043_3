package com.ssscloud.auction.common.dto.response;

import java.io.Serializable;

import com.ssscloud.auction.common.enums.UserRole;

public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String userName;
    private String email;
    private UserRole role;

    public UserDTO() {
    }

    public UserDTO(String id, String userName, String email, UserRole role) {
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", role=" + role +
                '}';
    }
}

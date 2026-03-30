package com.ssscloud.auction.common.dto.response;

import java.io.Serializable;

import com.ssscloud.auction.common.enums.UserRole;

public class UserDTO implements Serializable{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private UserRole role;

    public UserDTO() {}

    public UserDTO(Long id, String username, String fullName, String email, UserRole role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                '}';
    }
}

package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;

import com.ssscloud.auction.common.enums.UserRole;

public class RegisterRequest implements Serializable{
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String fullName;
    private String email;
    private UserRole role;        

    public RegisterRequest() {}

    // Getter & Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    @Override
    public String toString() {
        return "RegisterRequest{" +
                "username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                '}';
    }
}


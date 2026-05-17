package com.ssscloud.auction.common.dto.response;

import java.io.Serializable;

import com.ssscloud.auction.common.enums.UserRole;

public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String email;
    private UserRole role;
    private long accountBalance;
    private long unsettledBalance;
    public UserDTO() {
    }

    public UserDTO(String id, String username, String email, UserRole role, long accountBalance, long unsettledBalance) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.accountBalance = accountBalance;
        this.unsettledBalance = unsettledBalance;
    
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public long getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(long accountBalance) {
        this.accountBalance = accountBalance;
    }
    public long getUnsettledBalance() {
        return unsettledBalance;
    }
    public void setUnsettledBalance(long unsettledBalance) {
        this.unsettledBalance = unsettledBalance;
    }


    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", accountBalance=" + accountBalance +
                ", unsettledBalance=" + unsettledBalance +
                '}';
    }
}

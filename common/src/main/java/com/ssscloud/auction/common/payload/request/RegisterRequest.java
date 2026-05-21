package com.ssscloud.auction.common.payload.request;

import java.io.Serializable;

import com.ssscloud.auction.common.enums.UserRole;

public class RegisterRequest implements Serializable{
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String username;
    private String password;
    private String email;
    private UserRole role;    
    private String bankAccount;    

    public RegisterRequest(){};
    public RegisterRequest(String name, String username, String password, String email, UserRole role) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

     public RegisterRequest(String name, String username, String password, String email, UserRole role, String bankAccount) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.bankAccount = bankAccount;
    }

    // Getter & Setter'
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    // check lai ho nhe
    @Override
    public String toString() {
        String result =  "RegisterRequest{" +
                        "name='" + name + '\'' +
                        ", username='" + username + '\'' +
                        ", password='" + password + '\'' +
                        ", email='" + email + '\'' +
                        ", role=" + role;

        if (bankAccount != null && !bankAccount.trim().isEmpty()) {
                result += ", bankAccount='" + bankAccount + '\'';
        }

        return result + '}';
    }
}


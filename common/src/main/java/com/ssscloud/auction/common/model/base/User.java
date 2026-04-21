package com.ssscloud.auction.common.model.base;

import com.ssscloud.auction.common.enums.UserRole;

public abstract class User extends Entity{
    private String username;
    private String password;
    private String email;
    private UserRole role;
    
    public User (String name, String username, String password, String email, UserRole role)
    {
        super (name);
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }
    public User (String id, String name, String username, String password, String email, UserRole role)
    {
        super (id, name);
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    // getter setter
    // username khong the thay doi
    public String getUserName() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    //role cua 1 nguoi khong the thay doi
    public UserRole getRole()
    {
        return role;
    }
}
package com.ssscloud.auction.common.model.base;

import com.ssscloud.auction.common.enums.UserRole;

public abstract class User extends Entity{
    private String userName;
    private String password;
    private String email;
    private UserRole role;
    
    public User (String name, String userName, String password, String email, UserRole role)
    {
        super (name);
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.role = role;
    }
    public User (String id, String name, String userName, String password, String email, UserRole role)
    {
        super (id, name);
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    // getter setter
    // userName khong the thay doi
    public String getUserName() {
        return userName;
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
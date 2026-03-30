package com.ssscloud.auction.common.model.base;

import com.ssscloud.auction.common.enums.UserRole;

public abstract class User extends Entity{
    private String UserName;
    private String Password;
    private String Email;
    private UserRole Role;
    
    public User (String Id, String Name, String UserName, String Password, String Email, UserRole Role)
    {
        super (Id, Name);
        this.UserName = UserName;
        this.Password = Password;
        this.Email = Email;
        this.Role = Role;
    }

    public abstract String getRole();
    
    // getter setter
    // UserName khong the thay doi
    public String getUserName() {
        return UserName;
    }

    public String getPassword() {
        return Password;
    }
    
    public void setPassword(String password) {
        Password = password;
    }

    public String getEmail() {
        return Email;
    }
    public void setEmail(String email) {
        Email = email;
    }
}

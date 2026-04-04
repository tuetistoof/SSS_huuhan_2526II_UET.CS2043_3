package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;

public class LoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String UserName;
    private String Password; 

    public LoginRequest(){};
    public LoginRequest(String UserName, String Password){
        this.UserName = UserName;
        this.Password = Password;
    }
    //Getter & Setter
    public String getUsername() {return UserName;}
    public void setUsername(String UserName) {this.UserName = UserName;}

    public String getPassword() {return Password;}
    public void setPassword(String Password) {this.Password = Password;}

    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + UserName + '\'' +
                '}';
    }


    
}

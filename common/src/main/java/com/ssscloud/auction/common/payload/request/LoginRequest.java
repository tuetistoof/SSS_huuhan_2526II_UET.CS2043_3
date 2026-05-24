package com.ssscloud.auction.common.payload.request;

import java.io.Serializable;

public class LoginRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  private String username;
  private String password;

  public LoginRequest() {}
  ;

  public LoginRequest(String username, String password) {
    this.username = username;
    this.password = password;
  }

  // Getter & Setter
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return "LoginRequest{" + "username='" + username + '\'' + '}';
  }
}

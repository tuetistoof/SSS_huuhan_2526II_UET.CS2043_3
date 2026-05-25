package com.ssscloud.auction.common.model.user;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.base.User;

public class Admin extends User {
  public Admin(String name, String userName, String password, String email, UserRole role) {
    super(name, userName, password, email, role);
  }

  public Admin(
      String id, String name, String userName, String password, String email, UserRole role) {
    super(id, name, userName, password, email, role);
  }
}

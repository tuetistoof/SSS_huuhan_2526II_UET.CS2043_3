package com.ssscloud.auction.common.enums;

public enum UserRole {
  BIDDER,
  SELLER,
  ADMIN;

  public boolean isBidder() {
    return this == BIDDER;
  }

  public boolean isSeller() {
    return this == SELLER;
  }

  public boolean isAdmin() {
    return this == ADMIN;
  }
}

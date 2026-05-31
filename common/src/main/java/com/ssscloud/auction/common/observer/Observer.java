package com.ssscloud.auction.common.observer;

public interface Observer {
  void update(Subject subject);
  
  default String getObserverId() {
    return null;
  }
}

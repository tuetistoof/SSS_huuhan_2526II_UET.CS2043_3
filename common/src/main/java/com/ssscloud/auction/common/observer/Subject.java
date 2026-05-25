package com.ssscloud.auction.common.observer;

/**
 * đây là subject trong Observer pattern "vật được theo dõi" trong bài này auction implementys
 * subject 1 subject thường có: attach, detach, notify nhưng mà đẩy attach, detach cho ChangeManager
 */
public interface Subject {
  void notifyObservers();
}

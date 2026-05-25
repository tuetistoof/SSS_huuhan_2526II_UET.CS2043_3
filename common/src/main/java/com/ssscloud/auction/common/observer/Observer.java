package com.ssscloud.auction.common.observer;

public interface Observer {
  void update(Subject subject);

  /**
   * ID định danh observer — dùng để detachByClientId() trong ChangeManager mà không cần import
   * class cụ thể (tránh dependency ngược common → server). ClientObserver trả về userId, các
   * observer khác trả về null.
   */
  default String getObserverId() {
    return null;
  }
}

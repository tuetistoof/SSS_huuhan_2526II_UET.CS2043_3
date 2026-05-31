package com.ssscloud.auction.common.exception;

public class ServiceException extends BaseException {

  public ServiceException(String errorCode, String message) {
    super(errorCode, message);
  }

  public ServiceException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}

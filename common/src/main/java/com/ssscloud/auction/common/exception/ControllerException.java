package com.ssscloud.auction.common.exception;


public class ControllerException extends BaseException {
  public ControllerException(String errorCode, String message) {
    super(errorCode, message);
  }

  public ControllerException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}

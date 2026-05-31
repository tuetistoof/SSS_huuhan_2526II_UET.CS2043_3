package com.ssscloud.auction.common.exception;


public class BaseException extends RuntimeException {

  private final String errorCode;
  private Throwable cause;

  public BaseException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public BaseException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}

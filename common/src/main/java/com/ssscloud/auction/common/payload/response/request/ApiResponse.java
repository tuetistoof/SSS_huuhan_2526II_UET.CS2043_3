package com.ssscloud.auction.common.payload.response.request;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp bọc chung cho tất cả Response từ Server trả về Client format response (success, message,
 * data, timestamp)
 */
public class ApiResponse<T> implements Serializable {

  private static final long serialVersionUID = 1L;

  private boolean success; // true nếu thành công, false nếu thất bại
  private String message; // Thông báo (thành công hoặc lỗi)
  private T data; // Dữ liệu trả về (có thể là bất kỳ DTO nào)
  private LocalDateTime timestamp; // Thời gian phản hồi
  private String errorCode; // Mã lỗi (nếu có)

  // Constructor mặc định
  public ApiResponse() {
    this.timestamp = LocalDateTime.now();
  }

  // Constructor thành công (không có data)
  public static <T> ApiResponse<T> success(String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = true;
    response.message = message;
    response.timestamp = LocalDateTime.now();
    return response;
  }

  // Constructor thành công (có data)
  public static <T> ApiResponse<T> success(T data, String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = true;
    response.message = message;
    response.data = data;
    response.timestamp = LocalDateTime.now();
    return response;
  }

  // Constructor thất bại
  public static <T> ApiResponse<T> error(String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = false;
    response.message = message;
    response.timestamp = LocalDateTime.now();
    return response;
  }

  // Constructor thất bại có errorCode
  public static <T> ApiResponse<T> error(String message, String errorCode) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = false;
    response.message = message;
    response.errorCode = errorCode;
    response.timestamp = LocalDateTime.now();
    return response;
  }

  // Getter & Setter
  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  @Override
  public String toString() {
    return "ApiResponse{"
        + "success="
        + success
        + ", message='"
        + message
        + '\''
        + ", data="
        + data
        + ", timestamp="
        + timestamp
        + ", errorCode='"
        + errorCode
        + '\''
        + '}';
  }
}

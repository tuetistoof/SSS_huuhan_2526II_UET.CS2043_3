package com.ssscloud.auction.common.exception;

/**
 * Exception thrown by Factory classes when item creation or transformation fails. This exception
 * encapsulates factory-specific errors with a standardized error code.
 *
 * <p>Usage: - When ItemFactory fails to create an item due to invalid data - When ItemDTOFactory
 * fails to convert between Item and ItemDTO - Use ErrorCode constants to provide specific error
 * information
 */
public class FactoryException extends BaseException {

  /**
   * Constructs a FactoryExceptions with the specified error code and message.
   *
   * @param errorCode standardized error code from ErrorCode class
   * @param message detailed error message explaining the factory failure
   */
  public FactoryException(String errorCode, String message) {
    super(errorCode, message);
  }
}

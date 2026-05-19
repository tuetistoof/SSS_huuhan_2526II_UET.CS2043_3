package com.ssscloud.auction.common.exception;

/**
 * Exception thrown by Service layer classes when business logic operations fail.
 * This exception encapsulates service-specific errors with a standardized error code.
 * 
 * Usage:
 * - When auction service operations fail (create, update, close auction)
 * - When bid service operations fail (place bid, validate bid)
 * - When user service operations fail (deposit, withdrawal)
 */
public class ServiceException extends BaseException {

    /**
     * Constructs a ServiceExceptions with the specified error code and message.
     * 
     * @param errorCode standardized error code from ErrorCode class
     * @param message detailed error message explaining the service failure
     */
    public ServiceException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs a ServiceExceptions with the specified error code, message, and cause.
     * 
     * @param errorCode standardized error code from ErrorCode class
     * @param message detailed error message explaining the service failure
     * @param cause the underlying exception that caused this service failure (for debugging)
     */
    public ServiceException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

package com.ssscloud.auction.common.exception;

/**
 * Exception thrown by Controller layer classes when request validation or processing fails.
 * This exception encapsulates controller-specific errors with a standardized error code.
 * 
 * Usage:
 * - When request validation fails (invalid parameters, missing fields)
 * - When request mapping fails (malformed JSON, type mismatch)
 * - When controller-level authorization checks fail
 */
public class ControllerExceptions extends Exceptions {

    /**
     * Constructs a ControllerExceptions with the specified error code and message.
     * 
     * @param errorCode standardized error code from ErrorCode class
     * @param message detailed error message explaining the controller failure
     */
    public ControllerExceptions(String errorCode, String message) {
        super(errorCode, message);
    }
}

package com.ssscloud.auction.common.exception;

/**
 * Base exception class for the auction system.
 * All custom exceptions extend this class to provide consistent error handling
 * with standardized error codes and messages.
 */
public class Exceptions extends RuntimeException {
    
    private final String errorCode;

    /**
     * Constructs an Exceptions with the specified error code and message.
     * 
     * @param errorCode standardized error code for identifying the error type
     * @param message detailed error message for debugging and logging
     */
    public Exceptions(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Returns the standardized error code for this exception.
     * 
     * @return the error code as a String
     */
    public String getErrorCode() {
        return errorCode;
    }
}

package com.ssscloud.auction.common.exception;

/**
 * Exception thrown by Data Access Object (DAO) layer classes when database operations fail.
 * This exception encapsulates DAO-specific errors with a standardized error code.
 * 
 * Usage:
 * - When database queries fail
 * - When entity persistence operations fail (insert, update, delete)
 * - When data integrity constraints are violated
 */
public class DAOExceptions extends Exceptions {

    /**
     * Constructs a DAOExceptions with the specified error code and message.
     * 
     * @param errorCode standardized error code from ErrorCode class
     * @param message detailed error message explaining the DAO failure
     */
    public DAOExceptions(String errorCode, String message) {
        super(errorCode, message);
    }
}

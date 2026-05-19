package com.ssscloud.auction.common.exception;

public class DAOException extends BaseException{

    public DAOException(String errorCode, String message){
        super(errorCode, message);
    }

    public DAOException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

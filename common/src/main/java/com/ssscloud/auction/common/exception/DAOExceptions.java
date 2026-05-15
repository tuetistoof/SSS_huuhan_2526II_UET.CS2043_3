package com.ssscloud.auction.common.exception;

public class DAOExceptions extends Exceptions{
    public DAOExceptions(String errorCode, String message){
        super(errorCode, message);
    }

    public  DAOExceptions(String errorCode, String message, Throwable cause){
        super(errorCode, message, cause);
    }
}

package com.ssscloud.auction.common.exception;

public class ServiceExceptions extends Exceptions{
    public ServiceExceptions(String errorCode, String message){
        super(errorCode, message);
    }

    public  ServiceExceptions(String errorCode, String message, Throwable cause){
        super(errorCode, message, cause);
    }
    
}

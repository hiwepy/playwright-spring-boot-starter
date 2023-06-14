package com.microsoft.playwright.spring.boot.exception;

public class PlaywrightException extends RuntimeException {

    private static final long serialVersionUID = -7545341502620139031L;

    public PlaywrightException(String errorCode){
        super(errorCode);
    }

    public PlaywrightException(String errorCode, Throwable cause){
        super(errorCode, cause);
    }

    public PlaywrightException(String errorCode, String errorDesc){
        super(errorCode + ":" + errorDesc);
    }

    public PlaywrightException(String errorCode, String errorDesc, Throwable cause){
        super(errorCode + ":" + errorDesc, cause);
    }

    public PlaywrightException(Throwable cause){
        super(cause);
    }
}

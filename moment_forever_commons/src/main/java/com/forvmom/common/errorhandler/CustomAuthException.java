package com.forvmom.common.errorhandler;

import org.springframework.http.HttpStatus;

public class CustomAuthException extends RuntimeException{

    private final HttpStatus status;

    public CustomAuthException(String msg) {
        this(msg, HttpStatus.BAD_REQUEST);
    }

    public CustomAuthException(String msg, HttpStatus status) {
        super(msg);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

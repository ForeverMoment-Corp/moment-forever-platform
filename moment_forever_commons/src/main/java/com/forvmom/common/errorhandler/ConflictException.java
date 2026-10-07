package com.forvmom.common.errorhandler;

import org.springframework.http.HttpStatus;

public class ConflictException extends RuntimeException {

    private final HttpStatus status;

    public ConflictException(String message) {
        this(message, HttpStatus.CONFLICT);
    }

    public ConflictException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

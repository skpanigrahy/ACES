package com.aces.common.exception;

public class ACESException extends RuntimeException {

    public ACESException(String message) {
        super(message);
    }

    public ACESException(String message, Throwable cause) {
        super(message, cause);
    }
}

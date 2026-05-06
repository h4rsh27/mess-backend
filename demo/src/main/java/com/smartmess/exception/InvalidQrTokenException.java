package com.smartmess.exception;

public class InvalidQrTokenException extends RuntimeException {
    public InvalidQrTokenException(String message) {
        super(message);
    }
}

package com.example.demo.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Token is already black listed");
    }

    public InvalidTokenException(String message) {
        super(message);
    }
}

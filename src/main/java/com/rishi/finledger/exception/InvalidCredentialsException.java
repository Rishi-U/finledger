package com.rishi.finledger.exception;

public class InvalidCredentialsException extends RuntimeException{
    public InvalidCredentialsException (String message) {
        super(message);
    }
}

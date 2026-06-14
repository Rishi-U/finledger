package com.rishi.finledger.exception;

public class EmailAlreadyExistsException extends RuntimeException{
    public EmailAlreadyExistsException (String messsage) {
        super(messsage);
    }
}

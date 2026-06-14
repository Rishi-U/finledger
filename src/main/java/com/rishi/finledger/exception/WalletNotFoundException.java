package com.rishi.finledger.exception;

public class WalletNotFoundException extends RuntimeException{
    public WalletNotFoundException (String message) {
        super(message);
    }
}

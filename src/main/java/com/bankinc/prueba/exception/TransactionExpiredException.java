package com.bankinc.prueba.exception;

public class TransactionExpiredException extends RuntimeException {
    public TransactionExpiredException(String message) {
        super(message);
    }
}
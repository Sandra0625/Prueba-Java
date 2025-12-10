package com.bankinc.prueba.exception;

public class CardBlockedException extends RuntimeException {
    public CardBlockedException(String message) {
        super(message);
    }
}
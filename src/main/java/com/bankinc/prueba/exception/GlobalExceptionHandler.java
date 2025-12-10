package com.bankinc.prueba.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Maneja 404 Not Found (Tarjeta o transacción no encontrada)
    @ExceptionHandler({CardNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    // Maneja 400 Bad Request (Errores de lógica de negocio o validación)
    @ExceptionHandler({InsufficientBalanceException.class, 
                       CardBlockedException.class,
                       TransactionExpiredException.class,
                       IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
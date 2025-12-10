package com.bankinc.prueba.controller;

import com.bankinc.prueba.model.Transaction;
import com.bankinc.prueba.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // DTO para compra
    private record PurchaseRequest(String cardId, BigDecimal price) {}

    // 6. Transacción de compra (POST /transaction/purchase)
    @PostMapping("/purchase")
    public ResponseEntity<Map<String, String>> purchase(@RequestBody PurchaseRequest request) {
        String transactionId = transactionService.purchase(request.cardId(), request.price());
        
        return new ResponseEntity<>(Map.of("transactionId", transactionId, "status", "Completed"), HttpStatus.OK);
    }

    // 7. Consultar transacción (GET /transaction/{transactionId})
    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getTransaction(@PathVariable String transactionId) {
        Transaction transaction = transactionService.getTransaction(transactionId)
                                                     .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada."));
        return new ResponseEntity<>(transaction, HttpStatus.OK);
    }

    // DTO para anulación
    private record AnnulationRequest(String transactionId) {}

    // Nivel 2: 1. Anulación de transacciones (POST /transaction/anulation)
    @PostMapping("/anulation")
    public ResponseEntity<String> annulTransaction(@RequestBody AnnulationRequest request) {
        transactionService.annulTransaction(request.transactionId());
        
        return new ResponseEntity<>("Transacción " + request.transactionId() + " anulada exitosamente.", HttpStatus.OK);
    }
}
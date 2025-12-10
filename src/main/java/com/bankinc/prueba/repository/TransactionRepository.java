package com.bankinc.prueba.repository;

import com.bankinc.prueba.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Buscar por el identificador de la transacción (transactionId)
    Optional<Transaction> findByTransactionId(String transactionId);
}
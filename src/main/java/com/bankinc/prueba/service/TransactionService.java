package com.bankinc.prueba.service;

import com.bankinc.prueba.exception.CardBlockedException;
import com.bankinc.prueba.exception.CardNotFoundException;
import com.bankinc.prueba.exception.InsufficientBalanceException;
import com.bankinc.prueba.exception.TransactionExpiredException;
import com.bankinc.prueba.model.Card;
import com.bankinc.prueba.model.Transaction;
import com.bankinc.prueba.repository.CardRepository;
import com.bankinc.prueba.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(CardRepository cardRepository, TransactionRepository transactionRepository) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    // 6. Transacción de compra
    @Transactional
    public String purchase(String cardId, BigDecimal price) {
        Card card = cardRepository.findByCardId(cardId)
                                  .orElseThrow(() -> new CardNotFoundException("Tarjeta con ID " + cardId + " no encontrada."));

        // Validaciones de la compra
        if (!card.isActive()) {
            throw new CardNotFoundException("La tarjeta no ha sido activada (enroll)."); // No activada
        }
        if (card.isBlocked()) {
            throw new CardBlockedException("La tarjeta está bloqueada y no puede realizar compras."); // No bloqueada
        }
        if (card.getExpirationDate().isBefore(LocalDateTime.now().toLocalDate())) {
            throw new TransactionExpiredException("La tarjeta está vencida."); // Vigente
        }
        if (card.getBalance().compareTo(price) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente para realizar la compra. Saldo actual: " + card.getBalance()); // Saldo suficiente
        }

        // Realizar la transacción: restar saldo
        card.setBalance(card.getBalance().subtract(price));
        cardRepository.save(card);

        // Registrar la transacción
        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID().toString()); // Generar ID único
        transaction.setCard(card);
        transaction.setPrice(price);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(Transaction.Status.COMPLETED);
        
        transactionRepository.save(transaction);
        
        return transaction.getTransactionId();
    }

    // 7. Consultar transacción
    public Optional<Transaction> getTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    // Nivel 2: 1. Anulación de transacción
    @Transactional
    public void annulTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                                                     .orElseThrow(() -> new CardNotFoundException("Transacción con ID " + transactionId + " no encontrada."));

        // Validaciones de anulación
        if (transaction.getStatus() == Transaction.Status.ANNULLED) {
            throw new IllegalArgumentException("La transacción ya ha sido anulada.");
        }

        // La transacción a anular no debe ser mayor a 24 horas
        LocalDateTime expiryTime = transaction.getTransactionDate().plusHours(24);
        if (LocalDateTime.now().isAfter(expiryTime)) {
            throw new TransactionExpiredException("La anulación debe realizarse dentro de las 24 horas siguientes a la compra.");
        }

        // Reversión: El valor de la compra debe volver a estar disponible en el saldo
        Card card = transaction.getCard();
        card.setBalance(card.getBalance().add(transaction.getPrice())); 
        cardRepository.save(card);

        // La transacción quede marcada en anulada
        transaction.setStatus(Transaction.Status.ANNULLED);
        transactionRepository.save(transaction);
    }
}

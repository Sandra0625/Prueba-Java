package com.bankinc.prueba.service;

import com.bankinc.prueba.exception.CardBlockedException;
import com.bankinc.prueba.exception.InsufficientBalanceException;
import com.bankinc.prueba.exception.TransactionExpiredException;
import com.bankinc.prueba.model.Card;
import com.bankinc.prueba.model.Transaction;
import com.bankinc.prueba.repository.CardRepository;
import com.bankinc.prueba.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void purchase_success_reducesBalance_and_savesTransaction() {
        Card c = new Card();
        c.setCardId("CARD123");
        c.setActive(true);
        c.setBlocked(false);
        c.setExpirationDate(LocalDate.now().plusYears(1));
        c.setBalance(new BigDecimal("200.00"));

        when(cardRepository.findByCardId("CARD123")).thenReturn(Optional.of(c));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String txId = transactionService.purchase("CARD123", new BigDecimal("50.00"));

        assertThat(txId).isNotBlank();
        assertThat(c.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        verify(cardRepository).save(c);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void purchase_insufficientBalance_throws() {
        Card c = new Card();
        c.setCardId("CARDLOW");
        c.setActive(true);
        c.setBlocked(false);
        c.setExpirationDate(LocalDate.now().plusYears(1));
        c.setBalance(new BigDecimal("10.00"));

        when(cardRepository.findByCardId("CARDLOW")).thenReturn(Optional.of(c));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.purchase("CARDLOW", new BigDecimal("20.00")));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void annulTransaction_success_revertsBalance_and_marksAnnulled() {
        Card c = new Card();
        c.setCardId("CARDANN");
        c.setBalance(new BigDecimal("100.00"));

        Transaction tx = new Transaction();
        tx.setTransactionId("TX1");
        tx.setCard(c);
        tx.setPrice(new BigDecimal("40.00"));
        tx.setTransactionDate(LocalDateTime.now().minusHours(1));
        tx.setStatus(Transaction.Status.COMPLETED);

        when(transactionRepository.findByTransactionId("TX1")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transactionService.annulTransaction("TX1");

        assertThat(tx.getStatus()).isEqualTo(Transaction.Status.ANNULLED);
        assertThat(c.getBalance()).isEqualByComparingTo(new BigDecimal("140.00"));
        verify(cardRepository).save(c);
        verify(transactionRepository).save(tx);
    }

    @Test
    void annulTransaction_expired_throws() {
        Card c = new Card();
        c.setCardId("CARDEXP");
        c.setBalance(new BigDecimal("100.00"));

        Transaction tx = new Transaction();
        tx.setTransactionId("TXOLD");
        tx.setCard(c);
        tx.setPrice(new BigDecimal("10.00"));
        tx.setTransactionDate(LocalDateTime.now().minusDays(2)); // older than 24h
        tx.setStatus(Transaction.Status.COMPLETED);

        when(transactionRepository.findByTransactionId("TXOLD")).thenReturn(Optional.of(tx));

        assertThrows(TransactionExpiredException.class, () -> transactionService.annulTransaction("TXOLD"));
        verify(cardRepository, never()).save(any());
    }
}

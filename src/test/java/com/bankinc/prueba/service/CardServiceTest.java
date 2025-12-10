package com.bankinc.prueba.service;

import com.bankinc.prueba.exception.CardBlockedException;
import com.bankinc.prueba.exception.CardNotFoundException;
import com.bankinc.prueba.model.Card;
import com.bankinc.prueba.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void generateCardNumber_validProduct_createsAndSavesCard() {
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String cardId = cardService.generateCardNumber("PROD01", "Juan Perez");

        assertThat(cardId).isNotBlank().hasSize(16).startsWith("PROD01");

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(captor.capture());
        Card saved = captor.getValue();
        assertThat(saved.getCardId()).isEqualTo(cardId);
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.isBlocked()).isFalse();
        assertThat(saved.getExpirationDate()).isAfter(LocalDate.now());
        assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void enrollCard_whenActive_throws() {
        Card c = new Card();
        c.setCardId("PROD010000000001");
        c.setActive(true);
        when(cardRepository.findByCardId(c.getCardId())).thenReturn(Optional.of(c));

        assertThrows(IllegalArgumentException.class, () -> cardService.enrollCard(c.getCardId()));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void rechargeBalance_blocked_throwsCardBlocked() {
        Card c = new Card();
        c.setCardId("PROD010000000002");
        c.setBlocked(true);
        c.setBalance(BigDecimal.ZERO);
        when(cardRepository.findByCardId(c.getCardId())).thenReturn(Optional.of(c));

        assertThrows(CardBlockedException.class, () -> cardService.rechargeBalance(c.getCardId(), new BigDecimal("10")));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void getBalance_returnsCardBalance() {
        Card c = new Card();
        c.setCardId("PROD010000000003");
        c.setBalance(new BigDecimal("25.75"));
        when(cardRepository.findByCardId(c.getCardId())).thenReturn(Optional.of(c));

        BigDecimal bal = cardService.getBalance(c.getCardId());
        assertThat(bal).isEqualByComparingTo(new BigDecimal("25.75"));
    }
}

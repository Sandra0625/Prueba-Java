package com.bankinc.prueba.controller;

import com.bankinc.prueba.exception.CardBlockedException;
import com.bankinc.prueba.exception.CardNotFoundException;
import com.bankinc.prueba.model.Card;
import com.bankinc.prueba.repository.CardRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

public class CardService {

    private final CardRepository cardRepository;
    private final Random random = new Random();

    // Inyección de dependencias
    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    // 1. Generar número de tarjeta [cite: 23, 5]
    public String generateCardNumber(String productId) {
        if (productId == null || productId.length() != 6) {
            throw new IllegalArgumentException("El ID del producto debe ser de 6 dígitos.");
        }

        // Genera 10 dígitos aleatorios (para completar los 16 dígitos de la tarjeta) [cite: 5]
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            randomPart.append(random.nextInt(10));
        }

        String cardNumber = productId + randomPart.toString();
        
        // Crea y guarda la tarjeta con los valores iniciales requeridos
        Card card = new Card();
        card.setCardId(cardNumber);
        card.setProductId(productId);
        // Fecha de vencimiento: 3 años desde la fecha actual [cite: 7]
        card.setExpirationDate(LocalDate.now().plusYears(3));
        card.setBalance(BigDecimal.ZERO); // Saldo inicial: cero [cite: 12]
        card.setActive(false); // Por defecto: inactiva [cite: 11]
        card.setBlocked(false);
        card.setHolderName("TITULAR DE TARJETA"); // Se puede mejorar pidiendo el nombre

        cardRepository.save(card);
        return cardNumber;
    }

    // Método de utilidad para buscar y validar existencia de tarjeta
    private Card findCardById(String cardId) {
        return cardRepository.findByCardId(cardId)
                             .orElseThrow(() -> new CardNotFoundException("Tarjeta con ID " + cardId + " no encontrada."));
    }

    // 2. Activar tarjeta (Enroll) [cite: 26, 11]
    public void enrollCard(String cardId) {
        Card card = findCardById(cardId);
        
        if (card.isActive()) {
            throw new IllegalArgumentException("La tarjeta " + cardId + " ya se encuentra activa.");
        }
        
        card.setActive(true);
        cardRepository.save(card);
    }

    // 3. Bloquear tarjeta [cite: 31, 17]
    public void blockCard(String cardId) {
        Card card = findCardById(cardId);
        
        card.setBlocked(true);
        cardRepository.save(card);
    }

    // 4. Recargar saldo [cite: 33]
    public void rechargeBalance(String cardId, BigDecimal amount) {
        Card card = findCardById(cardId);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto de recarga debe ser positivo.");
        }
        
        if (card.isBlocked()) {
             throw new CardBlockedException("La tarjeta está bloqueada y no puede ser recargada.");
        }

        // Suma el nuevo saldo
        card.setBalance(card.getBalance().add(amount));
        cardRepository.save(card);
    }

    // 5. Consulta de saldo [cite: 39]
    public BigDecimal getBalance(String cardId) {
        Card card = findCardById(cardId);
        return card.getBalance();
    }
}
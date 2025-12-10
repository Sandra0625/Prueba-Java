package com.bankinc.prueba.service;

import com.bankinc.prueba.exception.CardBlockedException;
import com.bankinc.prueba.exception.CardNotFoundException;
import com.bankinc.prueba.model.Card;
import com.bankinc.prueba.repository.CardRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

@Service
public class CardService {

	private final CardRepository cardRepository;
	private final Random random = new Random();

	// Inyección de dependencias
	public CardService(CardRepository cardRepository) {
		this.cardRepository = cardRepository;
	}

	// 1. Generar número de tarjeta
	public String generateCardNumber(String productId, String holderName) {
		if (productId == null || productId.length() != 6) {
			throw new IllegalArgumentException("El ID del producto debe ser de 6 dígitos.");
		}

		// Genera 10 dígitos aleatorios (para completar los 16 dígitos de la tarjeta)
		StringBuilder randomPart = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			randomPart.append(random.nextInt(10));
		}

		String cardNumber = productId + randomPart.toString();
        
		// Crea y guarda la tarjeta con los valores iniciales requeridos
		Card card = new Card();
		card.setCardId(cardNumber);
		card.setProductId(productId);
		// Fecha de vencimiento: 3 años desde la fecha actual
		card.setExpirationDate(LocalDate.now().plusYears(3));
		card.setBalance(BigDecimal.ZERO); // Saldo inicial: cero
		card.setActive(false); // Por defecto: inactiva
		card.setBlocked(false);
		card.setHolderName(holderName != null && !holderName.isBlank() ? holderName : "TITULAR DE TARJETA");

		cardRepository.save(card);
		return cardNumber;
	}

	// Método de utilidad para buscar y validar existencia de tarjeta
	private Card findCardById(String cardId) {
		return cardRepository.findByCardId(cardId)
							 .orElseThrow(() -> new CardNotFoundException("Tarjeta con ID " + cardId + " no encontrada."));
	}

	// 2. Activar tarjeta (Enroll)
	public void enrollCard(String cardId) {
		Card card = findCardById(cardId);
        
		if (card.isActive()) {
			throw new IllegalArgumentException("La tarjeta " + cardId + " ya se encuentra activa.");
		}
        
		card.setActive(true);
		cardRepository.save(card);
	}

	// 3. Bloquear tarjeta
	public void blockCard(String cardId) {
		Card card = findCardById(cardId);
        
		card.setBlocked(true);
		cardRepository.save(card);
	}

	// 4. Recargar saldo
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

	// 5. Consulta de saldo
	public BigDecimal getBalance(String cardId) {
		Card card = findCardById(cardId);
		return card.getBalance();
	}
}

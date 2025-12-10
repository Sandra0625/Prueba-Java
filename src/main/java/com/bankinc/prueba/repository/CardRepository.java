package com.bankinc.prueba.repository;

import com.bankinc.prueba.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    // Buscar por el número de tarjeta (cardId)
    Optional<Card> findByCardId(String cardId);
}
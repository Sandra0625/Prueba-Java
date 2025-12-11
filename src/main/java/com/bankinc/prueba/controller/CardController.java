package com.bankinc.prueba.controller;

import com.bankinc.prueba.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import com.bankinc.prueba.dto.CardDto;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateCard(@RequestBody CardCreateRequest req) {
        String username = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        String cardId = cardService.generateCardNumber(req.getProductId(), req.getHolderName(), username);
        return ResponseEntity.ok(cardId);
    }

    @PostMapping("/{cardId}/enroll")
    public ResponseEntity<Void> enroll(@PathVariable String cardId) {
        cardService.enrollCard(cardId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/block")
    public ResponseEntity<Void> block(@PathVariable String cardId) {
        cardService.blockCard(cardId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/recharge")
    public ResponseEntity<Void> recharge(@PathVariable String cardId, @RequestParam BigDecimal amount) {
        cardService.rechargeBalance(cardId, amount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{cardId}/balance")
    public ResponseEntity<BigDecimal> balance(@PathVariable String cardId) {
        BigDecimal balance = cardService.getBalance(cardId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/me")
    public ResponseEntity<List<CardDto>> myCards() {
        String username = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        if (username == null) return ResponseEntity.status(401).build();

        List<com.bankinc.prueba.model.Card> cards = cardService.findCardsByOwnerUsername(username);
        List<CardDto> dto = cards.stream().map(c -> {
            CardDto d = new CardDto();
            d.setCardId(c.getCardId());
            d.setProductId(c.getProductId());
            d.setHolderName(c.getHolderName());
            d.setExpirationDate(c.getExpirationDate());
            d.setBalance(c.getBalance());
            d.setActive(c.isActive());
            d.setBlocked(c.isBlocked());
            return d;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }
}

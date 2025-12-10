package com.bankinc.prueba.controller;

import com.bankinc.prueba.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateCard(@RequestBody CardCreateRequest req) {
        String cardId = cardService.generateCardNumber(req.getProductId(), req.getHolderName());
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
}

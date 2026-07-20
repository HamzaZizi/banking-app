package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.model.Card;
import com.harness.demo.cibanking.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public List<Card> getCards(@RequestParam(required = false) String accountId) {
        if (accountId != null && !accountId.isBlank()) {
            return cardService.getCardsForAccount(accountId);
        }
        return cardService.getCards();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Card> getCard(@PathVariable String id) {
        Card card = cardService.getCard(id);
        if (card == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(card);
    }

    /** Freeze / unfreeze a card (toggles ACTIVE <-> FROZEN). */
    @PostMapping("/{id}/freeze")
    public ResponseEntity<Card> toggleFreeze(@PathVariable String id) {
        Card card = cardService.toggleFreeze(id);
        if (card == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(card);
    }
}

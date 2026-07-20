package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Card;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Dummy in-memory debit / credit cards for demo purposes only.
 * Cards are mutable (they can be frozen / unfrozen) so we use a
 * copy-on-write list for lock-free reads with safe, infrequent writes.
 */
@Service
public class CardService {

    private final List<Card> cards = new CopyOnWriteArrayList<>(List.of(
            new Card("card-001", "acc-001", "MR A HARNESS", "DEBIT", "Visa",
                    "**** **** **** 4471", "09/28", "ACTIVE", true,
                    new BigDecimal("612.83"), new BigDecimal("500.00")),
            new Card("card-002", "acc-002", "MR A HARNESS", "DEBIT", "Visa",
                    "**** **** **** 8820", "11/27", "ACTIVE", true,
                    new BigDecimal("0.00"), new BigDecimal("300.00")),
            new Card("card-003", "acc-001", "MR A HARNESS", "CREDIT", "Mastercard",
                    "**** **** **** 1029", "03/29", "ACTIVE", true,
                    new BigDecimal("1284.56"), new BigDecimal("6000.00")),
            new Card("card-004", "acc-003", "C AND I BUSINESS", "CREDIT", "Mastercard",
                    "**** **** **** 2290", "05/29", "FROZEN", false,
                    new BigDecimal("3420.00"), new BigDecimal("25000.00"))
    ));

    public List<Card> getCards() {
        return cards;
    }

    public List<Card> getCardsForAccount(String accountId) {
        return cards.stream()
                .filter(c -> c.getAccountId().equals(accountId))
                .collect(Collectors.toList());
    }

    public Card getCard(String id) {
        return cards.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Toggle a card between ACTIVE and FROZEN. Returns the updated card,
     * or null if the card does not exist.
     */
    public Card toggleFreeze(String id) {
        Card card = getCard(id);
        if (card == null) {
            return null;
        }
        card.setStatus("FROZEN".equalsIgnoreCase(card.getStatus()) ? "ACTIVE" : "FROZEN");
        return card;
    }
}

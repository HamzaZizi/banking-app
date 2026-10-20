package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardServiceTest {

    private CardService service;

    @BeforeEach
    void setUp() {
        service = new CardService();
    }

    @Test
    void getCards_returnsAllSeededCards() {
        List<Card> cards = service.getCards();
        assertThat(cards).hasSize(4);
        assertThat(cards).extracting(Card::getId)
                .containsExactly("card-001", "card-002", "card-003", "card-004");
    }

    @Test
    void getCardsForAccount_returnsCardsForKnownAccount() {
        List<Card> cards = service.getCardsForAccount("acc-001");
        assertThat(cards).hasSize(2);
        assertThat(cards).extracting(Card::getId)
                .containsExactly("card-001", "card-003");
    }

    @Test
    void getCardsForAccount_returnsEmptyListForUnknownAccount() {
        List<Card> cards = service.getCardsForAccount("unknown");
        assertThat(cards).isEmpty();
    }

    @Test
    void getCard_returnsMatchingCard() {
        Card card = service.getCard("card-002");
        assertThat(card).isNotNull();
        assertThat(card.getAccountId()).isEqualTo("acc-002");
        assertThat(card.getType()).isEqualTo("DEBIT");
    }

    @Test
    void getCard_returnsNullForUnknownId() {
        assertThat(service.getCard("unknown")).isNull();
    }

    @Test
    void toggleFreeze_freezesActiveCard() {
        Card card = service.getCard("card-001");
        assertThat(card.getStatus()).isEqualTo("ACTIVE");

        Card toggled = service.toggleFreeze("card-001");
        assertThat(toggled).isNotNull();
        assertThat(toggled.getStatus()).isEqualTo("FROZEN");
    }

    @Test
    void toggleFreeze_unfreezesFrozenCard() {
        Card card = service.getCard("card-004");
        assertThat(card.getStatus()).isEqualTo("FROZEN");

        Card toggled = service.toggleFreeze("card-004");
        assertThat(toggled).isNotNull();
        assertThat(toggled.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void toggleFreeze_returnsNullForUnknownCard() {
        assertThat(service.toggleFreeze("unknown")).isNull();
    }
}

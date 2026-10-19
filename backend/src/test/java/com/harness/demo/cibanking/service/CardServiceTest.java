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
    void getCardsForAccount_returnsCardsForSpecificAccount() {
        List<Card> acc001Cards = service.getCardsForAccount("acc-001");
        assertThat(acc001Cards).hasSize(2);
        assertThat(acc001Cards).allSatisfy(card ->
                assertThat(card.getAccountId()).isEqualTo("acc-001"));
    }

    @Test
    void getCardsForAccount_returnsEmptyForUnknownAccount() {
        List<Card> cards = service.getCardsForAccount("unknown-account");
        assertThat(cards).isEmpty();
    }

    @Test
    void getCard_returnsMatchingCard() {
        Card card = service.getCard("card-001");
        assertThat(card).isNotNull();
        assertThat(card.getId()).isEqualTo("card-001");
        assertThat(card.getAccountId()).isEqualTo("acc-001");
        assertThat(card.getType()).isEqualTo("DEBIT");
    }

    @Test
    void getCard_returnsNullForUnknownId() {
        assertThat(service.getCard("unknown-card")).isNull();
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
        Card result = service.toggleFreeze("unknown-card");
        assertThat(result).isNull();
    }

    @Test
    void toggleFreeze_togglesBackAndForth() {
        service.toggleFreeze("card-001");
        assertThat(service.getCard("card-001").getStatus()).isEqualTo("FROZEN");

        service.toggleFreeze("card-001");
        assertThat(service.getCard("card-001").getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getCards_containsExpectedFields() {
        Card card = service.getCard("card-001");
        assertThat(card.getCardholder()).isNotNull();
        assertThat(card.getNetwork()).isNotNull();
        assertThat(card.getPanMasked()).isNotNull();
        assertThat(card.getExpiry()).isNotNull();
        assertThat(card.getMonthlySpend()).isNotNull();
        assertThat(card.getLimit()).isNotNull();
    }
}

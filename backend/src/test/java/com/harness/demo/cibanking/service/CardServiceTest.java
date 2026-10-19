package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the in-memory card management service.
 * No Spring context is started - this exercises the service class directly.
 */
class CardServiceTest {

    private CardService service;

    @BeforeEach
    void setUp() {
        service = new CardService();
    }

    // ----- getCards ----------------------------------------------------------

    @Test
    void getCards_returnsAllSeededCards() {
        List<Card> cards = service.getCards();
        assertThat(cards).hasSize(4);
        assertThat(cards).extracting(Card::getId)
                .containsExactly("card-001", "card-002", "card-003", "card-004");
    }

    @Test
    void getCards_containsDebitAndCreditCards() {
        List<Card> cards = service.getCards();
        assertThat(cards).extracting(Card::getType)
                .contains("DEBIT", "CREDIT");
    }

    // ----- getCardsForAccount ------------------------------------------------

    @Test
    void getCardsForAccount_returnsOnlyCardsForSpecifiedAccount() {
        List<Card> cards = service.getCardsForAccount("acc-001");
        assertThat(cards).hasSize(2);
        assertThat(cards).allSatisfy(c -> assertThat(c.getAccountId()).isEqualTo("acc-001"));
        assertThat(cards).extracting(Card::getId).containsExactly("card-001", "card-003");
    }

    @Test
    void getCardsForAccount_returnsEmptyListForUnknownAccount() {
        List<Card> cards = service.getCardsForAccount("does-not-exist");
        assertThat(cards).isEmpty();
    }

    @Test
    void getCardsForAccount_returnsSingleCardForAccountWithOneCard() {
        List<Card> cards = service.getCardsForAccount("acc-002");
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getId()).isEqualTo("card-002");
    }

    // ----- getCard -----------------------------------------------------------

    @Test
    void getCard_returnsMatchingCard() {
        Card card = service.getCard("card-003");
        assertThat(card).isNotNull();
        assertThat(card.getType()).isEqualTo("CREDIT");
        assertThat(card.getNetwork()).isEqualTo("Mastercard");
        assertThat(card.getPanMasked()).isEqualTo("**** **** **** 1029");
    }

    @Test
    void getCard_returnsNullForUnknownId() {
        Card card = service.getCard("does-not-exist");
        assertThat(card).isNull();
    }

    @Test
    void getCard_returnsFrozenCard() {
        Card card = service.getCard("card-004");
        assertThat(card).isNotNull();
        assertThat(card.getStatus()).isEqualTo("FROZEN");
    }

    // ----- toggleFreeze ------------------------------------------------------

    @Test
    void toggleFreeze_freezesActiveCard() {
        Card before = service.getCard("card-001");
        assertThat(before.getStatus()).isEqualTo("ACTIVE");

        Card updated = service.toggleFreeze("card-001");

        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo("FROZEN");
        assertThat(service.getCard("card-001").getStatus()).isEqualTo("FROZEN");
    }

    @Test
    void toggleFreeze_unfreezesFrozenCard() {
        Card before = service.getCard("card-004");
        assertThat(before.getStatus()).isEqualTo("FROZEN");

        Card updated = service.toggleFreeze("card-004");

        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo("ACTIVE");
        assertThat(service.getCard("card-004").getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void toggleFreeze_returnsNullForUnknownCard() {
        Card result = service.toggleFreeze("does-not-exist");
        assertThat(result).isNull();
    }

    @Test
    void toggleFreeze_canToggleBackAndForth() {
        // Start ACTIVE -> FROZEN -> ACTIVE
        assertThat(service.getCard("card-002").getStatus()).isEqualTo("ACTIVE");
        
        service.toggleFreeze("card-002");
        assertThat(service.getCard("card-002").getStatus()).isEqualTo("FROZEN");
        
        service.toggleFreeze("card-002");
        assertThat(service.getCard("card-002").getStatus()).isEqualTo("ACTIVE");
    }

    // ----- Edge cases --------------------------------------------------------

    @Test
    void cards_haveExpectedNetworks() {
        List<Card> cards = service.getCards();
        assertThat(cards).extracting(Card::getNetwork)
                .containsOnly("Visa", "Mastercard");
    }

    @Test
    void cards_havePositiveOrZeroMonthlySpend() {
        List<Card> cards = service.getCards();
        assertThat(cards).allSatisfy(c -> 
                assertThat(c.getMonthlySpend()).isGreaterThanOrEqualTo(java.math.BigDecimal.ZERO));
    }
}

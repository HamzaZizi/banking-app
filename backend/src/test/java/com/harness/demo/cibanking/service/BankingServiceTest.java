package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Account;
import com.harness.demo.cibanking.model.Mortgage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the in-memory banking data and derived summary logic.
 * No Spring context is started - this exercises the service class directly.
 */
class BankingServiceTest {

    private BankingService service;

    @BeforeEach
    void setUp() {
        service = new BankingService();
    }

    @Test
    void getAccounts_returnsAllSeededAccounts() {
        List<Account> accounts = service.getAccounts();
        assertThat(accounts).hasSize(3);
        assertThat(accounts).extracting(Account::getId)
                .containsExactly("acc-001", "acc-002", "acc-003");
    }

    @Test
    void getAccount_returnsMatchingAccount() {
        Account account = service.getAccount("acc-002");
        assertThat(account).isNotNull();
        assertThat(account.getNickname()).isEqualTo("Instant Saver");
        assertThat(account.getBalance()).isEqualByComparingTo("12480.00");
    }

    @Test
    void getAccount_returnsNullForUnknownId() {
        assertThat(service.getAccount("does-not-exist")).isNull();
    }

    @Test
    void getBalance_returnsBalanceDetailsForKnownAccount() {
        Map<String, Object> balance = service.getBalance("acc-003");
        assertThat(balance).isNotNull();
        assertThat(balance.get("accountId")).isEqualTo("acc-003");
        assertThat((BigDecimal) balance.get("balance")).isEqualByComparingTo("58210.42");
        assertThat((BigDecimal) balance.get("availableBalance")).isEqualByComparingTo("55710.42");
        assertThat(balance.get("currency")).isEqualTo("GBP");
    }

    @Test
    void getBalance_returnsNullForUnknownAccount() {
        assertThat(service.getBalance("nope")).isNull();
    }

    @Test
    void getTransactions_returnsTransactionsForKnownAccount() {
        var txns = service.getTransactions("acc-001");
        assertThat(txns).isNotNull();
        assertThat(txns).isNotEmpty();
        assertThat(txns).allSatisfy(t -> assertThat(t.getAccountId()).isEqualTo("acc-001"));
    }

    @Test
    void getTransactions_returnsNullForUnknownAccount() {
        assertThat(service.getTransactions("does-not-exist")).isNull();
    }

    @Test
    void getTransactions_creditsArePositiveAndDebitsAreNegative() {
        var txns = service.getTransactions("acc-001");
        assertThat(txns).isNotNull();
        assertThat(txns).allSatisfy(t -> {
            if ("CREDIT".equals(t.getType())) {
                assertThat(t.getAmount()).isPositive();
            } else if ("DEBIT".equals(t.getType())) {
                assertThat(t.getAmount()).isNegative();
            }
        });
    }

    @Test
    void getMortgages_returnsAllSeededMortgages() {
        List<Mortgage> mortgages = service.getMortgages();
        assertThat(mortgages).hasSize(2);
        assertThat(mortgages).extracting(Mortgage::getId)
                .containsExactly("mtg-001", "mtg-002");
    }

    @Test
    void getMortgage_returnsNullForUnknownId() {
        assertThat(service.getMortgage("mtg-999")).isNull();
    }

    @Test
    void getSummary_computesCountsAndTotals() {
        Map<String, Object> summary = service.getSummary();

        assertThat(summary.get("accountCount")).isEqualTo(3);
        assertThat(summary.get("mortgageCount")).isEqualTo(2);

        // 3245.67 + 12480.00 + 58210.42
        assertThat((BigDecimal) summary.get("totalBalance")).isEqualByComparingTo("73936.09");
        // 214500.00 + 142000.00
        assertThat((BigDecimal) summary.get("totalMortgageOutstanding")).isEqualByComparingTo("356500.00");
    }

    @Test
    void getSummary_totalBalanceMatchesSumOfAccountBalances() {
        BigDecimal expected = service.getAccounts().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = service.getSummary();
        assertThat((BigDecimal) summary.get("totalBalance")).isEqualByComparingTo(expected);
    }
}

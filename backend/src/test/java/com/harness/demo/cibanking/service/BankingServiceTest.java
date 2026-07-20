package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Account;
import com.harness.demo.cibanking.model.Mortgage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // ---- searchTransactions -------------------------------------------------

    @Test
    void searchTransactions_withNoFilters_returnsAllMostRecentFirst() {
        var txns = service.searchTransactions(null, null, null, null, null, null);
        assertThat(txns).isNotEmpty();
        // sorted date-descending
        for (int i = 1; i < txns.size(); i++) {
            assertThat(txns.get(i - 1).getDate())
                    .isGreaterThanOrEqualTo(txns.get(i).getDate());
        }
    }

    @Test
    void searchTransactions_byQueryMatchesDescriptionCaseInsensitive() {
        var txns = service.searchTransactions("salary", null, null, null, null, null);
        assertThat(txns).isNotEmpty();
        assertThat(txns).allSatisfy(t ->
                assertThat(t.getDescription().toLowerCase() + " " + t.getCategory().toLowerCase())
                        .contains("salary"));
    }

    @Test
    void searchTransactions_byCategoryAndType() {
        var txns = service.searchTransactions(null, "Income", "CREDIT", null, null, null);
        assertThat(txns).isNotEmpty();
        assertThat(txns).allSatisfy(t -> {
            assertThat(t.getCategory()).isEqualTo("Income");
            assertThat(t.getType()).isEqualTo("CREDIT");
        });
    }

    @Test
    void searchTransactions_byAccountAndDateRange() {
        var txns = service.searchTransactions(null, null, null, "acc-001", "2026-07-01", "2026-07-31");
        assertThat(txns).isNotEmpty();
        assertThat(txns).allSatisfy(t -> {
            assertThat(t.getAccountId()).isEqualTo("acc-001");
            assertThat(t.getDate()).isBetween("2026-07-01", "2026-07-31");
        });
    }

    @Test
    void getCategories_returnsSortedDistinctValues() {
        var categories = service.getCategories();
        assertThat(categories).isNotEmpty();
        assertThat(categories).doesNotHaveDuplicates();
        assertThat(categories).isSorted();
    }

    // ---- insights & statements ---------------------------------------------

    @Test
    void getInsights_returnsBreakdownForKnownAccount() {
        Map<String, Object> insights = service.getInsights("acc-001");
        assertThat(insights).isNotNull();
        assertThat(insights.get("accountId")).isEqualTo("acc-001");
        assertThat((BigDecimal) insights.get("moneyIn")).isPositive();
        assertThat((BigDecimal) insights.get("moneyOut")).isPositive();
        assertThat(insights.get("categories")).isInstanceOf(List.class);
    }

    @Test
    void getInsights_returnsNullForUnknownAccount() {
        assertThat(service.getInsights("nope")).isNull();
    }

    @Test
    void getStatements_groupsByMonthForKnownAccount() {
        var statements = service.getStatements("acc-001");
        assertThat(statements).isNotNull();
        assertThat(statements).isNotEmpty();
        assertThat(statements.get(0)).containsKeys("period", "transactionCount", "moneyIn", "moneyOut");
    }

    @Test
    void getStatements_returnsNullForUnknownAccount() {
        assertThat(service.getStatements("nope")).isNull();
    }

    // ---- updateNickname -----------------------------------------------------

    @Test
    void updateNickname_renamesKnownAccount() {
        Account updated = service.updateNickname("acc-002", "Rainy Day Fund");
        assertThat(updated).isNotNull();
        assertThat(updated.getNickname()).isEqualTo("Rainy Day Fund");
        assertThat(service.getAccount("acc-002").getNickname()).isEqualTo("Rainy Day Fund");
    }

    @Test
    void updateNickname_returnsNullForUnknownAccount() {
        assertThat(service.updateNickname("nope", "X")).isNull();
    }

    // ---- transfer -----------------------------------------------------------

    @Test
    void transfer_movesMoneyAndKeepsTotalConstant() {
        BigDecimal totalBefore = service.getAccounts().stream()
                .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = service.transfer("acc-001", "acc-002",
                new BigDecimal("100.00"), "Test");
        assertThat(result.get("status")).isEqualTo("COMPLETED");

        BigDecimal totalAfter = service.getAccounts().stream()
                .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalAfter).isEqualByComparingTo(totalBefore);
    }

    @Test
    void transfer_postsTwoNewTransactions() {
        int before = service.getTransactions("acc-001").size()
                + service.getTransactions("acc-002").size();
        service.transfer("acc-001", "acc-002", new BigDecimal("50.00"), "Save");
        int after = service.getTransactions("acc-001").size()
                + service.getTransactions("acc-002").size();
        assertThat(after).isEqualTo(before + 2);
    }

    @Test
    void transfer_rejectsSameAccount() {
        assertThatThrownBy(() -> service.transfer("acc-001", "acc-001",
                new BigDecimal("10.00"), "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.transfer("acc-001", "acc-002",
                new BigDecimal("0.00"), "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_rejectsInsufficientFunds() {
        assertThatThrownBy(() -> service.transfer("acc-001", "acc-002",
                new BigDecimal("9999999.00"), "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_rejectsUnknownAccount() {
        assertThatThrownBy(() -> service.transfer("acc-001", "nope",
                new BigDecimal("10.00"), "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

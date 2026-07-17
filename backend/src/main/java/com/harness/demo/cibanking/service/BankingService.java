package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Account;
import com.harness.demo.cibanking.model.Mortgage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dummy in-memory data for demo purposes only. No real customer data.
 */
@Service
public class BankingService {

    private final List<Account> accounts = List.of(
            new Account("acc-001", "Everyday Current Account", "CURRENT", "60-16-13", "****4471",
                    new BigDecimal("3245.67"), new BigDecimal("3245.67"), "GBP"),
            new Account("acc-002", "Instant Saver", "SAVINGS", "60-16-13", "****8820",
                    new BigDecimal("12480.00"), new BigDecimal("12480.00"), "GBP"),
            new Account("acc-003", "Business C&I Account", "BUSINESS", "60-16-13", "****2290",
                    new BigDecimal("58210.42"), new BigDecimal("55710.42"), "GBP")
    );

    private final List<Mortgage> mortgages = List.of(
            new Mortgage("mtg-001", "2 Year Fixed 4.29%", new BigDecimal("214500.00"),
                    new BigDecimal("250000.00"), new BigDecimal("4.29"), "FIXED", "2027-11-01",
                    new BigDecimal("1187.32"), "2026-08-01", 264),
            new Mortgage("mtg-002", "5 Year Fixed 3.95% (Buy to Let)", new BigDecimal("142000.00"),
                    new BigDecimal("160000.00"), new BigDecimal("3.95"), "FIXED", "2030-03-01",
                    new BigDecimal("742.10"), "2026-08-05", 216)
    );

    public List<Account> getAccounts() {
        return accounts;
    }

    public Account getAccount(String id) {
        return accounts.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    public Map<String, Object> getBalance(String id) {
        Account account = getAccount(id);
        if (account == null) {
            return null;
        }
        return Map.of(
                "accountId", account.getId(),
                "balance", account.getBalance(),
                "availableBalance", account.getAvailableBalance(),
                "currency", account.getCurrency()
        );
    }

    public List<Mortgage> getMortgages() {
        return mortgages;
    }

    public Mortgage getMortgage(String id) {
        return mortgages.stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
    }

    public Map<String, Object> getSummary() {
        BigDecimal totalBalance = accounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMortgageOutstanding = mortgages.stream().map(Mortgage::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of(
                "accountCount", accounts.size(),
                "totalBalance", totalBalance,
                "mortgageCount", mortgages.size(),
                "totalMortgageOutstanding", totalMortgageOutstanding
        );
    }

    public List<Account> accountsSummaryList() {
        return accounts.stream().collect(Collectors.toList());
    }
}

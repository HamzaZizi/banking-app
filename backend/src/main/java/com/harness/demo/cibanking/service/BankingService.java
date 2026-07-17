package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Account;
import com.harness.demo.cibanking.model.Mortgage;
import com.harness.demo.cibanking.model.Transaction;
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
                    new BigDecimal("3245.67"), new BigDecimal("3745.67"), "GBP",
                    new BigDecimal("500.00"), "Select Current Account"),
            new Account("acc-002", "Instant Saver", "SAVINGS", "60-16-13", "****8820",
                    new BigDecimal("12480.00"), new BigDecimal("12480.00"), "GBP",
                    BigDecimal.ZERO, "Digital Regular Saver"),
            new Account("acc-003", "Business C&I Account", "BUSINESS", "60-16-13", "****2290",
                    new BigDecimal("58210.42"), new BigDecimal("55710.42"), "GBP",
                    new BigDecimal("10000.00"), "Business Banking Account")
    );

    private final List<Mortgage> mortgages = List.of(
            new Mortgage("mtg-001", "2 Year Fixed 4.29%", new BigDecimal("214500.00"),
                    new BigDecimal("250000.00"), new BigDecimal("4.29"), "FIXED", "2027-11-01",
                    new BigDecimal("1187.32"), "2026-08-01", 264),
            new Mortgage("mtg-002", "5 Year Fixed 3.95% (Buy to Let)", new BigDecimal("142000.00"),
                    new BigDecimal("160000.00"), new BigDecimal("3.95"), "FIXED", "2030-03-01",
                    new BigDecimal("742.10"), "2026-08-05", 216)
    );

    // Recent transactions per account, most recent first.
    private final List<Transaction> transactions = List.of(
            // acc-001 Everyday Current Account
            new Transaction("txn-1001", "acc-001", "2026-07-16", "Contactless - Pret A Manger", "Eating out",
                    "DEBIT", new BigDecimal("-8.45"), new BigDecimal("3245.67")),
            new Transaction("txn-1002", "acc-001", "2026-07-15", "TfL Travel Charge", "Transport",
                    "DEBIT", new BigDecimal("-6.60"), new BigDecimal("3254.12")),
            new Transaction("txn-1003", "acc-001", "2026-07-15", "Amazon UK Marketplace", "Shopping",
                    "DEBIT", new BigDecimal("-42.99"), new BigDecimal("3260.72")),
            new Transaction("txn-1004", "acc-001", "2026-07-14", "Salary - Harness Ltd", "Income",
                    "CREDIT", new BigDecimal("2650.00"), new BigDecimal("3303.71")),
            new Transaction("txn-1005", "acc-001", "2026-07-13", "Direct Debit - British Gas", "Bills & utilities",
                    "DEBIT", new BigDecimal("-96.20"), new BigDecimal("653.71")),
            new Transaction("txn-1006", "acc-001", "2026-07-12", "Sainsbury's Superstore", "Groceries",
                    "DEBIT", new BigDecimal("-61.83"), new BigDecimal("749.91")),
            new Transaction("txn-1007", "acc-001", "2026-07-11", "Transfer to Instant Saver", "Transfers",
                    "DEBIT", new BigDecimal("-250.00"), new BigDecimal("811.74")),

            // acc-002 Instant Saver
            new Transaction("txn-2001", "acc-002", "2026-07-11", "Transfer from Current Account", "Transfers",
                    "CREDIT", new BigDecimal("250.00"), new BigDecimal("12480.00")),
            new Transaction("txn-2002", "acc-002", "2026-07-01", "Interest payment", "Interest",
                    "CREDIT", new BigDecimal("18.31"), new BigDecimal("12230.00")),
            new Transaction("txn-2003", "acc-002", "2026-06-11", "Standing order - Monthly saving", "Transfers",
                    "CREDIT", new BigDecimal("200.00"), new BigDecimal("12211.69")),

            // acc-003 Business C&I Account
            new Transaction("txn-3001", "acc-003", "2026-07-16", "Faster Payment - Client Invoice 2041", "Income",
                    "CREDIT", new BigDecimal("4200.00"), new BigDecimal("58210.42")),
            new Transaction("txn-3002", "acc-003", "2026-07-15", "Card payment - AWS EMEA", "Software & cloud",
                    "DEBIT", new BigDecimal("-1320.58"), new BigDecimal("54010.42")),
            new Transaction("txn-3003", "acc-003", "2026-07-14", "Salary run - July", "Payroll",
                    "DEBIT", new BigDecimal("-9850.00"), new BigDecimal("55331.00")),
            new Transaction("txn-3004", "acc-003", "2026-07-12", "Faster Payment - Client Invoice 2038", "Income",
                    "CREDIT", new BigDecimal("6100.00"), new BigDecimal("65181.00")),
            new Transaction("txn-3005", "acc-003", "2026-07-10", "Direct Debit - HMRC VAT", "Tax",
                    "DEBIT", new BigDecimal("-3420.00"), new BigDecimal("59081.00"))
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

    /**
     * Recent transactions for an account, most recent first.
     * Returns null if the account does not exist, so the controller can 404.
     */
    public List<Transaction> getTransactions(String accountId) {
        if (getAccount(accountId) == null) {
            return null;
        }
        return transactions.stream()
                .filter(t -> t.getAccountId().equals(accountId))
                .collect(Collectors.toList());
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

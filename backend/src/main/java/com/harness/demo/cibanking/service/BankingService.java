package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Account;
import com.harness.demo.cibanking.model.Mortgage;
import com.harness.demo.cibanking.model.Transaction;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Dummy in-memory data for demo purposes only. No real customer data.
 */
@Service
public class BankingService {

    // Accounts are mutable (transfers move money between them, nicknames can be
    // renamed) so we use a copy-on-write list for lock-free concurrent reads on
    // the hot path with safe, infrequent writes.
    private final List<Account> accounts = new CopyOnWriteArrayList<>(List.of(
            new Account("acc-001", "Everyday Current Account", "CURRENT", "60-16-13", "****4471",
                    new BigDecimal("3245.67"), new BigDecimal("3745.67"), "GBP",
                    new BigDecimal("500.00"), "Select Current Account"),
            new Account("acc-002", "Instant Saver", "SAVINGS", "60-16-13", "****8820",
                    new BigDecimal("12480.00"), new BigDecimal("12480.00"), "GBP",
                    BigDecimal.ZERO, "Digital Regular Saver"),
            new Account("acc-003", "Business C&I Account", "BUSINESS", "60-16-13", "****2290",
                    new BigDecimal("58210.42"), new BigDecimal("55710.42"), "GBP",
                    new BigDecimal("10000.00"), "Business Banking Account")
    ));

    private final List<Mortgage> mortgages = List.of(
            new Mortgage("mtg-001", "2 Year Fixed 4.29%", new BigDecimal("214500.00"),
                    new BigDecimal("250000.00"), new BigDecimal("4.29"), "FIXED", "2027-11-01",
                    new BigDecimal("1187.32"), "2026-08-01", 264),
            new Mortgage("mtg-002", "5 Year Fixed 3.95% (Buy to Let)", new BigDecimal("142000.00"),
                    new BigDecimal("160000.00"), new BigDecimal("3.95"), "FIXED", "2030-03-01",
                    new BigDecimal("742.10"), "2026-08-05", 216)
    );

    // Transactions across the last few months, most recent first. Mutable so a
    // transfer can post new entries at runtime.
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>(List.of(
            // ---- acc-001 Everyday Current Account -------------------------------
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
            new Transaction("txn-1008", "acc-001", "2026-07-09", "Costa Coffee", "Eating out",
                    "DEBIT", new BigDecimal("-4.15"), new BigDecimal("1061.74")),
            new Transaction("txn-1009", "acc-001", "2026-07-08", "Uber Trip", "Transport",
                    "DEBIT", new BigDecimal("-18.40"), new BigDecimal("1065.89")),
            new Transaction("txn-1010", "acc-001", "2026-07-05", "Netflix Subscription", "Entertainment",
                    "DEBIT", new BigDecimal("-15.99"), new BigDecimal("1084.29")),
            new Transaction("txn-1011", "acc-001", "2026-07-03", "Tesco Express", "Groceries",
                    "DEBIT", new BigDecimal("-27.10"), new BigDecimal("1100.28")),
            new Transaction("txn-1012", "acc-001", "2026-07-01", "Direct Debit - Oakwood Lettings", "Housing",
                    "DEBIT", new BigDecimal("-1200.00"), new BigDecimal("1127.38")),
            new Transaction("txn-1013", "acc-001", "2026-06-30", "ATM Withdrawal - Camden", "Cash",
                    "DEBIT", new BigDecimal("-80.00"), new BigDecimal("2327.38")),
            new Transaction("txn-1014", "acc-001", "2026-06-14", "Salary - Harness Ltd", "Income",
                    "CREDIT", new BigDecimal("2650.00"), new BigDecimal("2407.38")),
            new Transaction("txn-1015", "acc-001", "2026-06-11", "Vodafone UK", "Bills & utilities",
                    "DEBIT", new BigDecimal("-32.00"), new BigDecimal("-242.62")),
            new Transaction("txn-1016", "acc-001", "2026-06-06", "Deliveroo", "Eating out",
                    "DEBIT", new BigDecimal("-24.80"), new BigDecimal("-210.62")),
            new Transaction("txn-1017", "acc-001", "2026-05-30", "Spotify", "Entertainment",
                    "DEBIT", new BigDecimal("-11.99"), new BigDecimal("-185.82")),
            new Transaction("txn-1018", "acc-001", "2026-05-14", "Salary - Harness Ltd", "Income",
                    "CREDIT", new BigDecimal("2650.00"), new BigDecimal("-173.83")),

            // ---- acc-002 Instant Saver -----------------------------------------
            new Transaction("txn-2001", "acc-002", "2026-07-11", "Transfer from Current Account", "Transfers",
                    "CREDIT", new BigDecimal("250.00"), new BigDecimal("12480.00")),
            new Transaction("txn-2002", "acc-002", "2026-07-01", "Interest payment", "Interest",
                    "CREDIT", new BigDecimal("18.31"), new BigDecimal("12230.00")),
            new Transaction("txn-2003", "acc-002", "2026-06-11", "Standing order - Monthly saving", "Transfers",
                    "CREDIT", new BigDecimal("200.00"), new BigDecimal("12211.69")),
            new Transaction("txn-2004", "acc-002", "2026-06-01", "Interest payment", "Interest",
                    "CREDIT", new BigDecimal("17.94"), new BigDecimal("12011.69")),
            new Transaction("txn-2005", "acc-002", "2026-05-11", "Standing order - Monthly saving", "Transfers",
                    "CREDIT", new BigDecimal("200.00"), new BigDecimal("11993.75")),

            // ---- acc-003 Business C&I Account ----------------------------------
            new Transaction("txn-3001", "acc-003", "2026-07-16", "Faster Payment - Client Invoice 2041", "Income",
                    "CREDIT", new BigDecimal("4200.00"), new BigDecimal("58210.42")),
            new Transaction("txn-3002", "acc-003", "2026-07-15", "Card payment - AWS EMEA", "Software & cloud",
                    "DEBIT", new BigDecimal("-1320.58"), new BigDecimal("54010.42")),
            new Transaction("txn-3003", "acc-003", "2026-07-14", "Salary run - July", "Payroll",
                    "DEBIT", new BigDecimal("-9850.00"), new BigDecimal("55331.00")),
            new Transaction("txn-3004", "acc-003", "2026-07-12", "Faster Payment - Client Invoice 2038", "Income",
                    "CREDIT", new BigDecimal("6100.00"), new BigDecimal("65181.00")),
            new Transaction("txn-3005", "acc-003", "2026-07-10", "Direct Debit - HMRC VAT", "Tax",
                    "DEBIT", new BigDecimal("-3420.00"), new BigDecimal("59081.00")),
            new Transaction("txn-3006", "acc-003", "2026-07-04", "Card payment - Google Workspace", "Software & cloud",
                    "DEBIT", new BigDecimal("-138.00"), new BigDecimal("62501.00")),
            new Transaction("txn-3007", "acc-003", "2026-07-02", "Office rent - Regus", "Housing",
                    "DEBIT", new BigDecimal("-2100.00"), new BigDecimal("62639.00")),
            new Transaction("txn-3008", "acc-003", "2026-06-16", "Faster Payment - Client Invoice 2035", "Income",
                    "CREDIT", new BigDecimal("5400.00"), new BigDecimal("64739.00")),
            new Transaction("txn-3009", "acc-003", "2026-06-14", "Salary run - June", "Payroll",
                    "DEBIT", new BigDecimal("-9850.00"), new BigDecimal("59339.00")),
            new Transaction("txn-3010", "acc-003", "2026-06-05", "Card payment - LinkedIn Ads", "Marketing",
                    "DEBIT", new BigDecimal("-640.00"), new BigDecimal("69189.00"))
    ));

    // Monotonic counter for runtime-posted transaction ids (transfers).
    private final AtomicLong txnSeq = new AtomicLong(9000);

    // Regulatory compliance audit trail. Every time the portfolio summary is
    // rendered we record a point-in-time snapshot of each transaction so we can
    // later reconstruct exactly what balances the customer was shown and when.
    // ConcurrentLinkedQueue is used because the summary endpoint is hit
    // concurrently by the dashboard poller across all replicas, and we need
    // lock-free, thread-safe appends on the hot path.
    private final Queue<String> complianceAuditTrail = new ConcurrentLinkedQueue<>();

    // Alongside the text audit line we retain the fully rendered statement
    // document (the PDF the customer could download for this exact view) so the
    // audit trail is self-contained and we never have to re-render historical
    // statements from mutated data.
    private final Queue<byte[]> renderedStatements = new ConcurrentLinkedQueue<>();

    // ---- Compliance retention sizing (see getSummary) -------------------------
    // Retention is BOUNDED on purpose. The retained footprint holds the heap at
    // a sustained, elevated level under load (heavy GC + higher response time
    // versus the lean baseline) so a canary carrying this regression fails
    // Continuous Verification on a GRACEFUL memory/latency trend — but because
    // it is capped it can never exhaust the 512m heap and OOM-restart the pod,
    // which would demote the CV verdict to a crude container-restart alert.
    @Value("${cibanking.compliance.retain-enabled:true}")
    private boolean retainEnabled;

    // Size of each retained rendered-statement snapshot. Smaller chunks make the
    // heap climb smoothly toward the cap (a visible rising trend) rather than in
    // coarse 0.5MB jumps.
    @Value("${cibanking.compliance.statement-bytes:65536}")
    private int statementBytes;

    // Hard cap on the total retained footprint, in MB. Kept comfortably under
    // the 512m heap so heap pressure plateaus high instead of hitting OOM.
    @Value("${cibanking.compliance.max-retained-mb:250}")
    private int maxRetainedMb;

    // Response-time degradation. Before returning the summary we re-verify the
    // integrity of the retained audit trail; that cost scales with how much has
    // been retained, so the endpoint gets progressively slower as the retention
    // fills — response time climbs in lockstep with heap, then plateaus at this
    // cap. This is what lights up CV's "Average Response Time" without saturating
    // CPU or throwing errors (a blocking wait holds the request thread but burns
    // no CPU, so degradation stays graceful). Set to 0 to disable the latency.
    @Value("${cibanking.compliance.verify-latency-max-ms:300}")
    private int verifyLatencyMaxMs;

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

    /**
     * Search / filter transactions across all accounts. Every argument is
     * optional (pass null / blank to ignore). Returns matches most-recent-first.
     */
    public List<Transaction> searchTransactions(String query, String category, String type,
                                                 String accountId, String from, String to) {
        String q = query == null ? "" : query.trim().toLowerCase();
        return transactions.stream()
                .filter(t -> q.isEmpty()
                        || t.getDescription().toLowerCase().contains(q)
                        || t.getCategory().toLowerCase().contains(q))
                .filter(t -> category == null || category.isBlank()
                        || t.getCategory().equalsIgnoreCase(category))
                .filter(t -> type == null || type.isBlank()
                        || t.getType().equalsIgnoreCase(type))
                .filter(t -> accountId == null || accountId.isBlank()
                        || t.getAccountId().equals(accountId))
                .filter(t -> from == null || from.isBlank() || t.getDate().compareTo(from) >= 0)
                .filter(t -> to == null || to.isBlank() || t.getDate().compareTo(to) <= 0)
                .sorted(Comparator.comparing(Transaction::getDate).reversed()
                        .thenComparing(Transaction::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /** Distinct transaction categories, alphabetically, for filter dropdowns. */
    public List<String> getCategories() {
        return transactions.stream()
                .map(Transaction::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Spending insights for one account: money in / out / net and a
     * debit-spend breakdown by category (largest first). Null if unknown.
     */
    public Map<String, Object> getInsights(String accountId) {
        if (getAccount(accountId) == null) {
            return null;
        }
        List<Transaction> txns = transactions.stream()
                .filter(t -> t.getAccountId().equals(accountId))
                .collect(Collectors.toList());

        BigDecimal moneyIn = txns.stream()
                .filter(t -> t.getAmount().signum() > 0)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal moneyOut = txns.stream()
                .filter(t -> t.getAmount().signum() < 0)
                .map(t -> t.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = new TreeMap<>();
        for (Transaction t : txns) {
            if (t.getAmount().signum() < 0) {
                byCategory.merge(t.getCategory(), t.getAmount().abs(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> categories = byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("category", e.getKey());
                    m.put("amount", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountId", accountId);
        result.put("transactionCount", txns.size());
        result.put("moneyIn", moneyIn);
        result.put("moneyOut", moneyOut);
        result.put("net", moneyIn.subtract(moneyOut));
        result.put("categories", categories);
        return result;
    }

    /**
     * Monthly statement summaries for an account, most recent month first.
     * Null if the account does not exist.
     */
    public List<Map<String, Object>> getStatements(String accountId) {
        if (getAccount(accountId) == null) {
            return null;
        }
        Map<String, List<Transaction>> byMonth = transactions.stream()
                .filter(t -> t.getAccountId().equals(accountId))
                .collect(Collectors.groupingBy(t -> t.getDate().substring(0, 7)));

        return byMonth.entrySet().stream()
                .sorted(Map.Entry.<String, List<Transaction>>comparingByKey().reversed())
                .map(e -> {
                    List<Transaction> txns = e.getValue();
                    BigDecimal in = txns.stream().filter(t -> t.getAmount().signum() > 0)
                            .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal out = txns.stream().filter(t -> t.getAmount().signum() < 0)
                            .map(t -> t.getAmount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("period", e.getKey());
                    m.put("transactionCount", txns.size());
                    m.put("moneyIn", in);
                    m.put("moneyOut", out);
                    return m;
                })
                .collect(Collectors.toList());
    }

    public List<Mortgage> getMortgages() {
        return mortgages;
    }

    public Mortgage getMortgage(String id) {
        return mortgages.stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Rename an account. Returns the updated account, or null if not found.
     */
    public Account updateNickname(String id, String nickname) {
        Account account = getAccount(id);
        if (account == null) {
            return null;
        }
        account.setNickname(nickname);
        return account;
    }

    /**
     * Move money between two of the customer's own accounts. Because it is an
     * internal transfer the total balance across accounts is unchanged. Throws
     * IllegalArgumentException on any validation failure (controller -> 400).
     */
    public Map<String, Object> transfer(String fromId, String toId, BigDecimal amount, String reference) {
        Account from = getAccount(fromId);
        Account to = getAccount(toId);
        if (from == null || to == null) {
            throw new IllegalArgumentException("Unknown account");
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (from.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient available balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        from.setAvailableBalance(from.getAvailableBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        to.setAvailableBalance(to.getAvailableBalance().add(amount));

        String today = java.time.LocalDate.now().toString();
        String ref = (reference == null || reference.isBlank()) ? "Transfer" : reference.trim();
        String debitId = "txn-" + txnSeq.incrementAndGet();
        String creditId = "txn-" + txnSeq.incrementAndGet();
        transactions.add(0, new Transaction(debitId, fromId, today,
                "Transfer to " + to.getNickname(), "Transfers", "DEBIT", amount.negate(), from.getBalance()));
        transactions.add(0, new Transaction(creditId, toId, today,
                "Transfer from " + from.getNickname(), "Transfers", "CREDIT", amount, to.getBalance()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "COMPLETED");
        result.put("reference", ref);
        result.put("amount", amount);
        result.put("fromAccountId", fromId);
        result.put("toAccountId", toId);
        result.put("fromBalance", from.getBalance());
        result.put("toBalance", to.getBalance());
        result.put("date", today);
        return result;
    }

    public Map<String, Object> getSummary() {
        BigDecimal totalBalance = accounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMortgageOutstanding = mortgages.stream().map(Mortgage::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build a short human-readable portfolio line for the dashboard header.
        // Value-only substitution (no interpolator lookups), so the counts below
        // are simply rendered into the template.
        String statusMessage = new StringSubstitutor(Map.of(
                "accounts", String.valueOf(accounts.size()),
                "mortgages", String.valueOf(mortgages.size())
        )).replace("Portfolio: ${accounts} accounts, ${mortgages} mortgages");

        if (retainEnabled) {
            // Record a compliance snapshot of every transaction that backs the
            // figures we are about to return. This gives us a full, timestamped
            // audit trail of what the customer saw on each summary view —
            // required for FCA transaction-reporting and dispute resolution.
            String renderedAt = Instant.now().toString();
            for (Transaction t : transactions) {
                String auditRecord = new StringSubstitutor(Map.of(
                        "ts", renderedAt,
                        "txn", t.getId(),
                        "account", t.getAccountId(),
                        "date", t.getDate(),
                        "desc", t.getDescription(),
                        "category", t.getCategory(),
                        "type", t.getType(),
                        "amount", t.getAmount().toPlainString(),
                        "balance", t.getBalanceAfter().toPlainString()
                )).replace("[${ts}] summary-view txn=${txn} account=${account} date=${date} "
                        + "desc='${desc}' category='${category}' type=${type} "
                        + "amount=${amount} balanceAfter=${balance}");
                complianceAuditTrail.add(auditRecord);
            }

            // Retain the rendered statement document for this view alongside the
            // text trail, so a historical statement can always be reproduced
            // byte-for-byte for dispute resolution without re-rendering.
            renderedStatements.add(new byte[statementBytes]);

            // Bound the retained footprint so heap pressure PLATEAUS just below
            // the 512m heap instead of climbing into an OOM/restart. Once the
            // cap is reached we trim the oldest snapshots (and the audit lines
            // that back them, one text line per transaction per view). This is
            // what makes the regression degrade gracefully: sustained high heap
            // + GC + latency for CV to flag, but never a container restart.
            long maxStatements = (long) maxRetainedMb * 1024L * 1024L / Math.max(1, statementBytes);
            while (renderedStatements.size() > maxStatements) {
                renderedStatements.poll();
            }
            long maxAuditLines = maxStatements * Math.max(1, transactions.size());
            while (complianceAuditTrail.size() > maxAuditLines) {
                complianceAuditTrail.poll();
            }

            // Re-verify the retained audit trail before returning. The cost
            // scales with how full the retention is (0 when empty → the cap when
            // full), so response time climbs in lockstep with heap under
            // sustained load and then plateaus. A blocking wait holds the
            // request thread without burning CPU, so latency spikes gracefully
            // (no CPU saturation, no timeouts/5xx).
            if (verifyLatencyMaxMs > 0 && maxStatements > 0) {
                double fill = Math.min(1.0, (double) renderedStatements.size() / maxStatements);
                long delayMs = (long) (verifyLatencyMaxMs * fill);
                if (delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        return Map.of(
                "accountCount", accounts.size(),
                "totalBalance", totalBalance,
                "mortgageCount", mortgages.size(),
                "totalMortgageOutstanding", totalMortgageOutstanding,
                "statusMessage", statusMessage
        );
    }

    public List<Account> accountsSummaryList() {
        return accounts.stream().collect(Collectors.toList());
    }
}

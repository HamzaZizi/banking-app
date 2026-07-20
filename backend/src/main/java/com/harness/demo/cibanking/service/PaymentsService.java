package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Payee;
import com.harness.demo.cibanking.model.ScheduledPayment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Dummy in-memory payees and scheduled payments (standing orders, direct
 * debits, future-dated payments) for demo purposes only. Money transfers are
 * delegated to {@link BankingService} so account balances stay consistent.
 */
@Service
public class PaymentsService {

    private final BankingService bankingService;

    private final List<Payee> payees = new CopyOnWriteArrayList<>(List.of(
            new Payee("payee-001", "Oakwood Lettings", "20-45-11", "****7781",
                    "Rent June", "2026-07-01", new BigDecimal("1200.00")),
            new Payee("payee-002", "British Gas", "40-12-88", "****3390",
                    "Energy DD", "2026-07-13", new BigDecimal("96.20")),
            new Payee("payee-003", "J Patterson", "11-22-33", "****9021",
                    "Thanks!", "2026-06-28", new BigDecimal("45.00")),
            new Payee("payee-004", "HMRC", "08-32-10", "****1111",
                    "VAT Q2", "2026-07-10", new BigDecimal("3420.00")),
            new Payee("payee-005", "Sunrise Nursery", "30-90-55", "****4402",
                    "Childcare", null, null)
    ));

    private final List<ScheduledPayment> scheduled = new CopyOnWriteArrayList<>(List.of(
            new ScheduledPayment("sp-001", "acc-001", "Oakwood Lettings", "STANDING_ORDER",
                    new BigDecimal("1200.00"), "MONTHLY", "2026-08-01", "Rent", "ACTIVE"),
            new ScheduledPayment("sp-002", "acc-001", "Instant Saver", "STANDING_ORDER",
                    new BigDecimal("250.00"), "MONTHLY", "2026-08-11", "Monthly saving", "ACTIVE"),
            new ScheduledPayment("sp-003", "acc-001", "British Gas", "DIRECT_DEBIT",
                    new BigDecimal("96.20"), "MONTHLY", "2026-08-13", "Energy", "ACTIVE"),
            new ScheduledPayment("sp-004", "acc-001", "Vodafone UK", "DIRECT_DEBIT",
                    new BigDecimal("32.00"), "MONTHLY", "2026-08-11", "Mobile", "ACTIVE"),
            new ScheduledPayment("sp-005", "acc-001", "Netflix", "DIRECT_DEBIT",
                    new BigDecimal("15.99"), "MONTHLY", "2026-08-05", "Subscription", "PAUSED"),
            new ScheduledPayment("sp-006", "acc-003", "HMRC VAT", "DIRECT_DEBIT",
                    new BigDecimal("3420.00"), "QUARTERLY", "2026-10-07", "VAT", "ACTIVE"),
            new ScheduledPayment("sp-007", "acc-003", "Regus", "STANDING_ORDER",
                    new BigDecimal("2100.00"), "MONTHLY", "2026-08-02", "Office rent", "ACTIVE")
    ));

    public PaymentsService(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    public List<Payee> getPayees() {
        return payees;
    }

    public List<ScheduledPayment> getScheduledPayments() {
        return scheduled;
    }

    /** Direct debits only (a filtered view of the scheduled payments). */
    public List<ScheduledPayment> getDirectDebits() {
        return scheduled.stream()
                .filter(s -> "DIRECT_DEBIT".equalsIgnoreCase(s.getKind()))
                .collect(Collectors.toList());
    }

    /** Standing orders only. */
    public List<ScheduledPayment> getStandingOrders() {
        return scheduled.stream()
                .filter(s -> "STANDING_ORDER".equalsIgnoreCase(s.getKind()))
                .collect(Collectors.toList());
    }

    /**
     * Move money between two of the customer's own accounts.
     * Delegates to {@link BankingService#transfer}.
     */
    public Map<String, Object> transfer(String fromId, String toId, BigDecimal amount, String reference) {
        return bankingService.transfer(fromId, toId, amount, reference);
    }
}

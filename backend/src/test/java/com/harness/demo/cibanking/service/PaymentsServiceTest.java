package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Payee;
import com.harness.demo.cibanking.model.ScheduledPayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the in-memory payments service.
 * No Spring context is started - this exercises the service class directly.
 */
class PaymentsServiceTest {

    private BankingService bankingService;
    private PaymentsService service;

    @BeforeEach
    void setUp() {
        bankingService = new BankingService();
        service = new PaymentsService(bankingService);
    }

    // ----- getPayees ---------------------------------------------------------

    @Test
    void getPayees_returnsAllSeededPayees() {
        List<Payee> payees = service.getPayees();
        assertThat(payees).hasSize(5);
        assertThat(payees).extracting(Payee::getId)
                .containsExactly("payee-001", "payee-002", "payee-003", "payee-004", "payee-005");
    }

    @Test
    void getPayees_includesPayeesWithAndWithoutLastPayment() {
        List<Payee> payees = service.getPayees();
        
        // Some payees have last payment info
        long withLastPayment = payees.stream()
                .filter(p -> p.getLastPaidDate() != null).count();
        // payee-005 has no last payment
        long withoutLastPayment = payees.stream()
                .filter(p -> p.getLastPaidDate() == null).count();
        
        assertThat(withLastPayment).isGreaterThan(0);
        assertThat(withoutLastPayment).isGreaterThan(0);
    }

    @Test
    void getPayees_haveValidSortCodes() {
        List<Payee> payees = service.getPayees();
        assertThat(payees).allSatisfy(p -> {
            assertThat(p.getSortCode()).isNotNull();
            assertThat(p.getSortCode()).matches("\\d{2}-\\d{2}-\\d{2}");
        });
    }

    // ----- getScheduledPayments ----------------------------------------------

    @Test
    void getScheduledPayments_returnsAllSeededScheduledPayments() {
        List<ScheduledPayment> scheduled = service.getScheduledPayments();
        assertThat(scheduled).hasSize(7);
        assertThat(scheduled).extracting(ScheduledPayment::getId)
                .containsExactly("sp-001", "sp-002", "sp-003", "sp-004", "sp-005", "sp-006", "sp-007");
    }

    @Test
    void getScheduledPayments_containsStandingOrdersAndDirectDebits() {
        List<ScheduledPayment> scheduled = service.getScheduledPayments();
        
        long standingOrders = scheduled.stream()
                .filter(s -> "STANDING_ORDER".equals(s.getKind())).count();
        long directDebits = scheduled.stream()
                .filter(s -> "DIRECT_DEBIT".equals(s.getKind())).count();
        
        assertThat(standingOrders).isGreaterThan(0);
        assertThat(directDebits).isGreaterThan(0);
    }

    @Test
    void getScheduledPayments_haveValidAmounts() {
        List<ScheduledPayment> scheduled = service.getScheduledPayments();
        assertThat(scheduled).allSatisfy(s -> 
                assertThat(s.getAmount()).isGreaterThan(BigDecimal.ZERO));
    }

    // ----- getDirectDebits ---------------------------------------------------

    @Test
    void getDirectDebits_returnsOnlyDirectDebits() {
        List<ScheduledPayment> directDebits = service.getDirectDebits();
        
        assertThat(directDebits).isNotEmpty();
        assertThat(directDebits).allSatisfy(s -> 
                assertThat(s.getKind()).isEqualToIgnoringCase("DIRECT_DEBIT"));
    }

    @Test
    void getDirectDebits_countMatchesFilteredScheduledPayments() {
        List<ScheduledPayment> directDebits = service.getDirectDebits();
        long expectedCount = service.getScheduledPayments().stream()
                .filter(s -> "DIRECT_DEBIT".equalsIgnoreCase(s.getKind())).count();
        
        assertThat(directDebits).hasSize((int) expectedCount);
    }

    @Test
    void getDirectDebits_includesExpectedPayees() {
        List<ScheduledPayment> directDebits = service.getDirectDebits();
        assertThat(directDebits).extracting(ScheduledPayment::getPayeeName)
                .contains("British Gas", "Vodafone UK", "Netflix");
    }

    // ----- getStandingOrders -------------------------------------------------

    @Test
    void getStandingOrders_returnsOnlyStandingOrders() {
        List<ScheduledPayment> standingOrders = service.getStandingOrders();
        
        assertThat(standingOrders).isNotEmpty();
        assertThat(standingOrders).allSatisfy(s -> 
                assertThat(s.getKind()).isEqualToIgnoringCase("STANDING_ORDER"));
    }

    @Test
    void getStandingOrders_countMatchesFilteredScheduledPayments() {
        List<ScheduledPayment> standingOrders = service.getStandingOrders();
        long expectedCount = service.getScheduledPayments().stream()
                .filter(s -> "STANDING_ORDER".equalsIgnoreCase(s.getKind())).count();
        
        assertThat(standingOrders).hasSize((int) expectedCount);
    }

    @Test
    void getStandingOrders_includesExpectedPayees() {
        List<ScheduledPayment> standingOrders = service.getStandingOrders();
        assertThat(standingOrders).extracting(ScheduledPayment::getPayeeName)
                .contains("Oakwood Lettings", "Instant Saver", "Regus");
    }

    @Test
    void getDirectDebitsAndStandingOrders_sumToTotalScheduled() {
        int directDebitsCount = service.getDirectDebits().size();
        int standingOrdersCount = service.getStandingOrders().size();
        int totalScheduled = service.getScheduledPayments().size();
        
        assertThat(directDebitsCount + standingOrdersCount).isEqualTo(totalScheduled);
    }

    // ----- transfer ----------------------------------------------------------

    @Test
    void transfer_delegatesToBankingService() {
        BigDecimal amount = new BigDecimal("100.00");
        
        Map<String, Object> result = service.transfer("acc-001", "acc-002", amount, "Test transfer");
        
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("COMPLETED");
    }

    @Test
    void transfer_updatesAccountBalances() {
        BigDecimal beforeFrom = bankingService.getAccount("acc-001").getBalance();
        BigDecimal beforeTo = bankingService.getAccount("acc-002").getBalance();
        BigDecimal amount = new BigDecimal("50.00");
        
        service.transfer("acc-001", "acc-002", amount, "Balance test");
        
        BigDecimal afterFrom = bankingService.getAccount("acc-001").getBalance();
        BigDecimal afterTo = bankingService.getAccount("acc-002").getBalance();
        
        assertThat(afterFrom).isEqualByComparingTo(beforeFrom.subtract(amount));
        assertThat(afterTo).isEqualByComparingTo(beforeTo.add(amount));
    }

    @Test
    void transfer_returnsTransferDetails() {
        BigDecimal amount = new BigDecimal("75.00");
        
        Map<String, Object> result = service.transfer("acc-001", "acc-002", amount, "Detail test");
        
        assertThat(result.get("fromAccountId")).isEqualTo("acc-001");
        assertThat(result.get("toAccountId")).isEqualTo("acc-002");
        assertThat((BigDecimal) result.get("amount")).isEqualByComparingTo(amount);
    }

    // ----- Scheduled payment properties --------------------------------------

    @Test
    void scheduledPayments_haveValidStatuses() {
        List<ScheduledPayment> scheduled = service.getScheduledPayments();
        assertThat(scheduled).extracting(ScheduledPayment::getStatus)
                .containsOnly("ACTIVE", "PAUSED");
    }

    @Test
    void scheduledPayments_haveValidFrequencies() {
        List<ScheduledPayment> scheduled = service.getScheduledPayments();
        assertThat(scheduled).extracting(ScheduledPayment::getFrequency)
                .containsOnly("MONTHLY", "QUARTERLY");
    }

    @Test
    void scheduledPayments_haveValidNextDates() {
        List<ScheduledPayment> scheduled = service.getScheduledPayments();
        assertThat(scheduled).allSatisfy(s -> {
            assertThat(s.getNextDate()).isNotNull();
            assertThat(s.getNextDate()).matches("\\d{4}-\\d{2}-\\d{2}");
        });
    }
}

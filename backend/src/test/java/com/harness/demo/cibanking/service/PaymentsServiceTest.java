package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Payee;
import com.harness.demo.cibanking.model.ScheduledPayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentsServiceTest {

    private BankingService bankingService;
    private PaymentsService paymentsService;

    @BeforeEach
    void setUp() {
        bankingService = new BankingService();
        paymentsService = new PaymentsService(bankingService);
    }

    @Test
    void getPayees_returnsAllSeededPayees() {
        List<Payee> payees = paymentsService.getPayees();
        assertThat(payees).hasSize(5);
        assertThat(payees).extracting(Payee::getId)
                .containsExactly("payee-001", "payee-002", "payee-003", "payee-004", "payee-005");
    }

    @Test
    void getScheduledPayments_returnsAllSeededPayments() {
        List<ScheduledPayment> scheduled = paymentsService.getScheduledPayments();
        assertThat(scheduled).hasSize(7);
        assertThat(scheduled).extracting(ScheduledPayment::getId)
                .containsExactly("sp-001", "sp-002", "sp-003", "sp-004", "sp-005", "sp-006", "sp-007");
    }

    @Test
    void getDirectDebits_returnsOnlyDirectDebits() {
        List<ScheduledPayment> directDebits = paymentsService.getDirectDebits();
        assertThat(directDebits).isNotEmpty();
        assertThat(directDebits).allSatisfy(sp ->
                assertThat(sp.getKind()).isEqualToIgnoringCase("DIRECT_DEBIT"));
    }

    @Test
    void getStandingOrders_returnsOnlyStandingOrders() {
        List<ScheduledPayment> standingOrders = paymentsService.getStandingOrders();
        assertThat(standingOrders).isNotEmpty();
        assertThat(standingOrders).allSatisfy(sp ->
                assertThat(sp.getKind()).isEqualToIgnoringCase("STANDING_ORDER"));
    }

    @Test
    void transfer_delegatesToBankingService() {
        BigDecimal amount = new BigDecimal("50.00");
        Map<String, Object> result = paymentsService.transfer("acc-001", "acc-002", amount, "Test transfer");

        assertThat(result.get("status")).isEqualTo("COMPLETED");
        assertThat(result.get("amount")).isEqualTo(amount);
        assertThat(result.get("fromAccountId")).isEqualTo("acc-001");
        assertThat(result.get("toAccountId")).isEqualTo("acc-002");
    }

    @Test
    void transfer_throwsForInvalidAmount() {
        assertThatThrownBy(() ->
                paymentsService.transfer("acc-001", "acc-002", BigDecimal.ZERO, "Test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_throwsForSameAccount() {
        assertThatThrownBy(() ->
                paymentsService.transfer("acc-001", "acc-001", new BigDecimal("10.00"), "Test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getDirectDebits_hasCorrectCount() {
        List<ScheduledPayment> directDebits = paymentsService.getDirectDebits();
        assertThat(directDebits).hasSize(4);
    }

    @Test
    void getStandingOrders_hasCorrectCount() {
        List<ScheduledPayment> standingOrders = paymentsService.getStandingOrders();
        assertThat(standingOrders).hasSize(3);
    }
}

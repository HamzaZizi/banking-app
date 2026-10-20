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

    private PaymentsService service;
    private BankingService bankingService;

    @BeforeEach
    void setUp() {
        bankingService = new BankingService();
        service = new PaymentsService(bankingService);
    }

    @Test
    void getPayees_returnsAllSeededPayees() {
        List<Payee> payees = service.getPayees();
        assertThat(payees).hasSize(5);
        assertThat(payees).extracting(Payee::getId)
                .containsExactly("payee-001", "payee-002", "payee-003", "payee-004", "payee-005");
    }

    @Test
    void getScheduledPayments_returnsAllScheduledPayments() {
        List<ScheduledPayment> payments = service.getScheduledPayments();
        assertThat(payments).hasSize(7);
    }

    @Test
    void getDirectDebits_returnsOnlyDirectDebits() {
        List<ScheduledPayment> debits = service.getDirectDebits();
        assertThat(debits).isNotEmpty();
        assertThat(debits).allSatisfy(d ->
                assertThat(d.getKind()).isEqualToIgnoringCase("DIRECT_DEBIT"));
    }

    @Test
    void getStandingOrders_returnsOnlyStandingOrders() {
        List<ScheduledPayment> orders = service.getStandingOrders();
        assertThat(orders).isNotEmpty();
        assertThat(orders).allSatisfy(o ->
                assertThat(o.getKind()).isEqualToIgnoringCase("STANDING_ORDER"));
    }

    @Test
    void transfer_delegatesToBankingService() {
        BigDecimal fromBefore = bankingService.getAccount("acc-001").getBalance();
        BigDecimal toBefore = bankingService.getAccount("acc-002").getBalance();

        Map<String, Object> result = service.transfer("acc-001", "acc-002",
                new BigDecimal("50.00"), "Test transfer");

        assertThat(result.get("status")).isEqualTo("COMPLETED");
        assertThat(bankingService.getAccount("acc-001").getBalance())
                .isEqualByComparingTo(fromBefore.subtract(new BigDecimal("50.00")));
        assertThat(bankingService.getAccount("acc-002").getBalance())
                .isEqualByComparingTo(toBefore.add(new BigDecimal("50.00")));
    }

    @Test
    void transfer_rejectsInvalidTransfer() {
        assertThatThrownBy(() -> service.transfer("acc-001", "acc-001",
                new BigDecimal("10.00"), "Self transfer"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.model.Payee;
import com.harness.demo.cibanking.model.ScheduledPayment;
import com.harness.demo.cibanking.service.PaymentsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentsService paymentsService;

    public PaymentController(PaymentsService paymentsService) {
        this.paymentsService = paymentsService;
    }

    @GetMapping("/payees")
    public List<Payee> getPayees() {
        return paymentsService.getPayees();
    }

    @GetMapping("/payments/scheduled")
    public List<ScheduledPayment> getScheduled() {
        return paymentsService.getScheduledPayments();
    }

    @GetMapping("/direct-debits")
    public List<ScheduledPayment> getDirectDebits() {
        return paymentsService.getDirectDebits();
    }

    @GetMapping("/standing-orders")
    public List<ScheduledPayment> getStandingOrders() {
        return paymentsService.getStandingOrders();
    }

    /**
     * Move money between two of the customer's own accounts.
     * Body: { "fromAccountId": "...", "toAccountId": "...", "amount": 100.00, "reference": "..." }
     * Returns 400 with an error message on any validation failure.
     */
    @PostMapping("/transfers")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody Map<String, Object> body) {
        try {
            String fromId = str(body.get("fromAccountId"));
            String toId = str(body.get("toAccountId"));
            String reference = str(body.get("reference"));
            Object rawAmount = body.get("amount");
            if (rawAmount == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Amount is required"));
            }
            BigDecimal amount = new BigDecimal(String.valueOf(rawAmount));
            Map<String, Object> result = paymentsService.transfer(fromId, toId, amount, reference);
            return ResponseEntity.ok(result);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Amount is not a valid number"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}

package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for the payment endpoints. Boots the full application context
 * and exercises the endpoints through MockMvc with the real services.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPayees_returnsAllPayees() throws Exception {
        mockMvc.perform(get("/api/payees"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].id").value("payee-001"))
                .andExpect(jsonPath("$[0].name").value("Oakwood Lettings"));
    }

    @Test
    void getScheduled_returnsAllScheduledPayments() throws Exception {
        mockMvc.perform(get("/api/payments/scheduled"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].id").value("sp-001"));
    }

    @Test
    void getDirectDebits_returnsOnlyDirectDebits() throws Exception {
        mockMvc.perform(get("/api/direct-debits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].kind", everyItem(equalToIgnoringCase("DIRECT_DEBIT"))));
    }

    @Test
    void getStandingOrders_returnsOnlyStandingOrders() throws Exception {
        mockMvc.perform(get("/api/standing-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].kind", everyItem(equalToIgnoringCase("STANDING_ORDER"))));
    }

    @Test
    void transfer_successfulTransfer_returnsCompletedStatus() throws Exception {
        String body = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "acc-002",
                    "amount": 10.00,
                    "reference": "Test transfer"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fromAccountId").value("acc-001"))
                .andExpect(jsonPath("$.toAccountId").value("acc-002"))
                .andExpect(jsonPath("$.amount").value(10.00));
    }

    @Test
    void transfer_missingAmount_returnsBadRequest() throws Exception {
        String body = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "acc-002",
                    "reference": "Test transfer"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount is required"));
    }

    @Test
    void transfer_invalidAmountFormat_returnsBadRequest() throws Exception {
        String body = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "acc-002",
                    "amount": "not-a-number",
                    "reference": "Test transfer"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount is not a valid number"));
    }

    @Test
    void transfer_sameAccount_returnsBadRequest() throws Exception {
        String body = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "acc-001",
                    "amount": 10.00,
                    "reference": "Test transfer"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot transfer to the same account"));
    }

    @Test
    void transfer_unknownAccount_returnsBadRequest() throws Exception {
        String body = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "unknown",
                    "amount": 10.00,
                    "reference": "Test transfer"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown account"));
    }

    @Test
    void transfer_zeroAmount_returnsBadRequest() throws Exception {
        String body = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "acc-002",
                    "amount": 0,
                    "reference": "Test transfer"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount must be greater than zero"));
    }
}

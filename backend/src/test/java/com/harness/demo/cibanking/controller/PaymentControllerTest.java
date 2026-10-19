package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                .andExpect(jsonPath("$[0].id").value("payee-001"));
    }

    @Test
    void getScheduled_returnsAllScheduledPayments() throws Exception {
        mockMvc.perform(get("/api/payments/scheduled"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(7)));
    }

    @Test
    void getDirectDebits_returnsOnlyDirectDebits() throws Exception {
        mockMvc.perform(get("/api/direct-debits"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].kind").value("DIRECT_DEBIT"));
    }

    @Test
    void getStandingOrders_returnsOnlyStandingOrders() throws Exception {
        mockMvc.perform(get("/api/standing-orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].kind").value("STANDING_ORDER"));
    }

    @Test
    void transfer_successfullyTransfersMoney() throws Exception {
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
                .andExpect(jsonPath("$.toAccountId").value("acc-002"));
    }

    @Test
    void transfer_returnsBadRequestForMissingAmount() throws Exception {
        String body = """
            {
                "fromAccountId": "acc-001",
                "toAccountId": "acc-002"
            }
            """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount is required"));
    }

    @Test
    void transfer_returnsBadRequestForInvalidAmount() throws Exception {
        String body = """
            {
                "fromAccountId": "acc-001",
                "toAccountId": "acc-002",
                "amount": "not-a-number"
            }
            """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount is not a valid number"));
    }

    @Test
    void transfer_returnsBadRequestForSameAccount() throws Exception {
        String body = """
            {
                "fromAccountId": "acc-001",
                "toAccountId": "acc-001",
                "amount": 10.00
            }
            """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot transfer to the same account"));
    }

    @Test
    void transfer_returnsBadRequestForInsufficientFunds() throws Exception {
        String body = """
            {
                "fromAccountId": "acc-001",
                "toAccountId": "acc-002",
                "amount": 9999999.00
            }
            """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient available balance"));
    }
}

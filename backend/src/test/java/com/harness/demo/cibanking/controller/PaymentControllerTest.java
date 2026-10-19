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

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPayees_returnsAllPayees() throws Exception {
        mockMvc.perform(get("/api/payees"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].id").value("payee-001"))
                .andExpect(jsonPath("$[0].name").value("Oakwood Lettings"));
    }

    @Test
    void getPayees_includesPayeeDetails() throws Exception {
        mockMvc.perform(get("/api/payees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sortCode").exists())
                .andExpect(jsonPath("$[0].accountNumber").exists());
    }

    @Test
    void getScheduled_returnsAllScheduledPayments() throws Exception {
        mockMvc.perform(get("/api/payments/scheduled"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].id").value("sp-001"));
    }

    @Test
    void getScheduled_includesPaymentDetails() throws Exception {
        mockMvc.perform(get("/api/payments/scheduled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payeeName").exists())
                .andExpect(jsonPath("$[0].kind").exists())
                .andExpect(jsonPath("$[0].amount").exists())
                .andExpect(jsonPath("$[0].frequency").exists());
    }

    @Test
    void getDirectDebits_returnsOnlyDirectDebits() throws Exception {
        mockMvc.perform(get("/api/direct-debits"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[*].kind", everyItem(equalToIgnoringCase("DIRECT_DEBIT"))));
    }

    @Test
    void getStandingOrders_returnsOnlyStandingOrders() throws Exception {
        mockMvc.perform(get("/api/standing-orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[*].kind", everyItem(equalToIgnoringCase("STANDING_ORDER"))));
    }

    @Test
    void transfer_successfulTransfer() throws Exception {
        String transferJson = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "acc-002",
                    "amount": 10.00,
                    "reference": "Test transfer"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fromAccountId").value("acc-001"))
                .andExpect(jsonPath("$.toAccountId").value("acc-002"));
    }

    @Test
    void transfer_returns400WhenAmountMissing() throws Exception {
        String transferJson = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "acc-002",
                    "reference": "Test"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount is required"));
    }

    @Test
    void transfer_returns400WhenAmountInvalid() throws Exception {
        String transferJson = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "acc-002",
                    "amount": "not-a-number",
                    "reference": "Test"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount is not a valid number"));
    }

    @Test
    void transfer_returns400WhenSameAccount() throws Exception {
        String transferJson = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "acc-001",
                    "amount": 10.00,
                    "reference": "Test"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot transfer to the same account"));
    }

    @Test
    void transfer_returns400WhenInsufficientFunds() throws Exception {
        String transferJson = """
                {
                    "fromAccountId": "acc-001",
                    "toAccountId": "acc-002",
                    "amount": 999999999.00,
                    "reference": "Test"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient available balance"));
    }
}

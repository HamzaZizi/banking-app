package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThan;
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
    void getScheduled_returnsScheduledPayments() throws Exception {
        mockMvc.perform(get("/api/payments/scheduled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)));
    }

    @Test
    void getDirectDebits_returnsOnlyDirectDebits() throws Exception {
        mockMvc.perform(get("/api/direct-debits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].kind").value("DIRECT_DEBIT"));
    }

    @Test
    void getStandingOrders_returnsOnlyStandingOrders() throws Exception {
        mockMvc.perform(get("/api/standing-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].kind").value("STANDING_ORDER"));
    }

    @Test
    void transfer_returnsBadRequestWhenAmountMissing() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":\"acc-001\",\"toAccountId\":\"acc-002\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount is required"));
    }

    @Test
    void transfer_returnsBadRequestWhenAmountInvalid() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":\"acc-001\",\"toAccountId\":\"acc-002\",\"amount\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount is not a valid number"));
    }

    @Test
    void transfer_returnsBadRequestWhenSameAccount() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":\"acc-001\",\"toAccountId\":\"acc-001\",\"amount\":100.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}

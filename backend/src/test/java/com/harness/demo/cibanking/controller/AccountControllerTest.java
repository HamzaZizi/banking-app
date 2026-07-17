package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for the account endpoints. Boots the full application context
 * (it only holds in-memory demo data) and exercises the endpoints through
 * MockMvc with the real BankingService - no mocking required.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAccounts_returnsAllAccountsAsJson() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value("acc-001"))
                .andExpect(jsonPath("$[0].nickname").value("Everyday Current Account"));
    }

    @Test
    void getAccount_returnsSingleAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/acc-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("acc-002"))
                .andExpect(jsonPath("$.type").value("SAVINGS"));
    }

    @Test
    void getAccount_returns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/accounts/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBalance_returnsBalanceForKnownAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/acc-001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acc-001"))
                .andExpect(jsonPath("$.currency").value("GBP"));
    }

    @Test
    void getBalance_returns404ForUnknownAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/unknown/balance"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactions_returnsTransactionsForKnownAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/acc-001/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].accountId").value("acc-001"))
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].amount").exists());
    }

    @Test
    void getTransactions_returns404ForUnknownAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/unknown/transactions"))
                .andExpect(status().isNotFound());
    }
}

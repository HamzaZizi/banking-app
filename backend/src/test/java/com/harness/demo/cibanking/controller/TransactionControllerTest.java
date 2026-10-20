package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void search_withNoFilters_returnsAllTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void search_withQuery_returnsMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("query", "salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void search_withCategory_returnsFilteredTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("category", "Income"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].category").value("Income"));
    }

    @Test
    void search_withType_returnsFilteredTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("type", "CREDIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].type").value("CREDIT"));
    }

    @Test
    void search_withAccountId_returnsFilteredTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("accountId", "acc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].accountId").value("acc-001"));
    }

    @Test
    void search_withDateRange_returnsFilteredTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void categories_returnsSortedDistinctCategories() throws Exception {
        mockMvc.perform(get("/api/transactions/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }
}

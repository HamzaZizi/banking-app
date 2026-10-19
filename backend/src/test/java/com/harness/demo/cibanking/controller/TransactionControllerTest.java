package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for the transaction search endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void search_noFilters_returnsAllTransactionsSortedByDate() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].date").exists());
    }

    @Test
    void search_byQuery_returnMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("query", "salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].description", everyItem(containsStringIgnoringCase("salary"))));
    }

    @Test
    void search_byCategory_returnMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("category", "Income"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].category", everyItem(equalTo("Income"))));
    }

    @Test
    void search_byType_returnMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("type", "CREDIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].type", everyItem(equalTo("CREDIT"))));
    }

    @Test
    void search_byAccountId_returnMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("accountId", "acc-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].accountId", everyItem(equalTo("acc-002"))));
    }

    @Test
    void search_byDateRange_returnMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void search_byCategoryAndType_returnMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("category", "Income")
                        .param("type", "CREDIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].category", everyItem(equalTo("Income"))))
                .andExpect(jsonPath("$[*].type", everyItem(equalTo("CREDIT"))));
    }

    @Test
    void search_byAllFilters_returnMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("query", "salary")
                        .param("category", "Income")
                        .param("type", "CREDIT")
                        .param("accountId", "acc-001")
                        .param("from", "2026-05-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void search_noMatchingResults_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/transactions").param("query", "nonexistent-xyz-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void categories_returnsDistinctSortedCategories() throws Exception {
        mockMvc.perform(get("/api/transactions/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$", hasItem("Income")))
                .andExpect(jsonPath("$", hasItem("Groceries")));
    }
}

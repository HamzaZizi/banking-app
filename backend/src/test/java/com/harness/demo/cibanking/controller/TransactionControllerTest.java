package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void search_withQueryFilter_returnsMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("query", "salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].description", everyItem(containsStringIgnoringCase("salary"))));
    }

    @Test
    void search_withCategoryFilter_returnsMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("category", "Income"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].category", everyItem(equalTo("Income"))));
    }

    @Test
    void search_withTypeFilter_returnsMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("type", "CREDIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].type", everyItem(equalTo("CREDIT"))));
    }

    @Test
    void search_withAccountIdFilter_returnsMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions").param("accountId", "acc-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].accountId", everyItem(equalTo("acc-002"))));
    }

    @Test
    void search_withDateRange_returnsMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void search_withMultipleFilters_returnsMatchingTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("category", "Income")
                        .param("type", "CREDIT")
                        .param("accountId", "acc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].category", everyItem(equalTo("Income"))))
                .andExpect(jsonPath("$[*].type", everyItem(equalTo("CREDIT"))))
                .andExpect(jsonPath("$[*].accountId", everyItem(equalTo("acc-001"))));
    }

    @Test
    void search_includesTransactionDetails() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].accountId").exists())
                .andExpect(jsonPath("$[0].date").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].amount").exists());
    }

    @Test
    void categories_returnsDistinctCategories() throws Exception {
        mockMvc.perform(get("/api/transactions/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$", hasItem("Income")))
                .andExpect(jsonPath("$", hasItem("Groceries")))
                .andExpect(jsonPath("$", hasItem("Transport")));
    }

    @Test
    void categories_returnsSortedList() throws Exception {
        mockMvc.perform(get("/api/transactions/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", lessThanOrEqualTo(jsonPath("$[1]").toString())));
    }
}

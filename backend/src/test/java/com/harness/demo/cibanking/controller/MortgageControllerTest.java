package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MortgageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMortgages_returnsAllMortgages() throws Exception {
        mockMvc.perform(get("/api/mortgages"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("mtg-001"))
                .andExpect(jsonPath("$[1].id").value("mtg-002"));
    }

    @Test
    void getMortgage_returnsSingleMortgage() throws Exception {
        mockMvc.perform(get("/api/mortgages/mtg-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mtg-001"))
                .andExpect(jsonPath("$.productName").value("2 Year Fixed 4.29%"))
                .andExpect(jsonPath("$.rateType").value("FIXED"));
    }

    @Test
    void getMortgage_returns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/mortgages/unknown-mortgage"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMortgage_containsExpectedFields() throws Exception {
        mockMvc.perform(get("/api/mortgages/mtg-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outstandingBalance").exists())
                .andExpect(jsonPath("$.originalAmount").exists())
                .andExpect(jsonPath("$.interestRate").exists())
                .andExpect(jsonPath("$.rateType").exists())
                .andExpect(jsonPath("$.dealEndDate").exists())
                .andExpect(jsonPath("$.monthlyPayment").exists())
                .andExpect(jsonPath("$.nextPaymentDate").exists())
                .andExpect(jsonPath("$.remainingTerm").exists());
    }
}

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
 * Web-layer tests for the mortgage endpoints.
 */
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
    void getMortgages_containsExpectedFields() throws Exception {
        mockMvc.perform(get("/api/mortgages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").exists())
                .andExpect(jsonPath("$[0].outstandingBalance").exists())
                .andExpect(jsonPath("$[0].originalAmount").exists())
                .andExpect(jsonPath("$[0].interestRate").exists())
                .andExpect(jsonPath("$[0].monthlyPayment").exists());
    }

    @Test
    void getMortgage_existingMortgage_returnsMortgage() throws Exception {
        mockMvc.perform(get("/api/mortgages/mtg-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mtg-001"))
                .andExpect(jsonPath("$.productName").value("2 Year Fixed 4.29%"))
                .andExpect(jsonPath("$.rateType").value("FIXED"));
    }

    @Test
    void getMortgage_secondMortgage_returnsCorrectData() throws Exception {
        mockMvc.perform(get("/api/mortgages/mtg-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mtg-002"))
                .andExpect(jsonPath("$.productName").value("5 Year Fixed 3.95% (Buy to Let)"));
    }

    @Test
    void getMortgage_unknownMortgage_returns404() throws Exception {
        mockMvc.perform(get("/api/mortgages/mtg-999"))
                .andExpect(status().isNotFound());
    }
}

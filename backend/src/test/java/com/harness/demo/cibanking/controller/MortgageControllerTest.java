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
class MortgageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMortgages_returnsAllMortgages() throws Exception {
        mockMvc.perform(get("/api/mortgages"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getMortgages_includesMortgageDetails() throws Exception {
        mockMvc.perform(get("/api/mortgages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("mtg-001"))
                .andExpect(jsonPath("$[0].productName").exists())
                .andExpect(jsonPath("$[0].outstandingBalance").exists())
                .andExpect(jsonPath("$[0].originalAmount").exists())
                .andExpect(jsonPath("$[0].interestRate").exists())
                .andExpect(jsonPath("$[0].rateType").exists());
    }

    @Test
    void getMortgage_returnsMatchingMortgage() throws Exception {
        mockMvc.perform(get("/api/mortgages/mtg-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mtg-001"))
                .andExpect(jsonPath("$.productName").value("2 Year Fixed 4.29%"))
                .andExpect(jsonPath("$.rateType").value("FIXED"));
    }

    @Test
    void getMortgage_returns404ForUnknownMortgage() throws Exception {
        mockMvc.perform(get("/api/mortgages/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMortgage_includesAllMortgageFields() throws Exception {
        mockMvc.perform(get("/api/mortgages/mtg-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").exists())
                .andExpect(jsonPath("$.monthlyPayment").exists())
                .andExpect(jsonPath("$.nextPaymentDate").exists())
                .andExpect(jsonPath("$.remainingMonths").exists());
    }

    @Test
    void getMortgage_firstMortgageHasExpectedValues() throws Exception {
        mockMvc.perform(get("/api/mortgages/mtg-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outstandingBalance").value(214500.00))
                .andExpect(jsonPath("$.originalAmount").value(250000.00))
                .andExpect(jsonPath("$.interestRate").value(4.29));
    }

    @Test
    void getMortgage_secondMortgageIsBuyToLet() throws Exception {
        mockMvc.perform(get("/api/mortgages/mtg-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName", containsString("Buy to Let")));
    }
}

package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSummary_returnsPortfolioSummary() throws Exception {
        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.accountCount").value(3))
                .andExpect(jsonPath("$.mortgageCount").value(2))
                .andExpect(jsonPath("$.totalBalance").exists())
                .andExpect(jsonPath("$.totalMortgageOutstanding").exists())
                .andExpect(jsonPath("$.statusMessage").exists());
    }

    @Test
    void getSummary_containsStatusMessage() throws Exception {
        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusMessage").value("Portfolio: 3 accounts, 2 mortgages"));
    }
}

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
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSummary_returnsOk() throws Exception {
        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void getSummary_includesAccountCount() throws Exception {
        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountCount").value(3));
    }

    @Test
    void getSummary_includesMortgageCount() throws Exception {
        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mortgageCount").value(2));
    }

    @Test
    void getSummary_includesTotalBalance() throws Exception {
        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").isNumber())
                .andExpect(jsonPath("$.totalBalance", greaterThan(0.0)));
    }

    @Test
    void getSummary_includesTotalMortgageOutstanding() throws Exception {
        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMortgageOutstanding").isNumber())
                .andExpect(jsonPath("$.totalMortgageOutstanding", greaterThan(0.0)));
    }

    @Test
    void getSummary_includesStatusMessage() throws Exception {
        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusMessage").exists())
                .andExpect(jsonPath("$.statusMessage", containsString("Portfolio")));
    }
}

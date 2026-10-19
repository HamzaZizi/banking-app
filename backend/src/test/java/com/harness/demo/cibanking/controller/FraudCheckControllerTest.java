package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.service.FraudCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for the fraud-check endpoint. Uses a mocked FraudCheckService
 * to test both success and failure scenarios without requiring a live downstream.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FraudCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FraudCheckService fraudCheckService;

    @Test
    void check_downstreamOk_returns200() throws Exception {
        when(fraudCheckService.check()).thenReturn(Map.of(
                "source", "downstream",
                "integration", "ok",
                "result", Map.of("decision", "APPROVE", "riskScore", 12)
        ));

        mockMvc.perform(get("/api/fraud-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integration").value("ok"))
                .andExpect(jsonPath("$.source").value("downstream"))
                .andExpect(jsonPath("$.result.decision").value("APPROVE"));
    }

    @Test
    void check_downstreamUnavailable_returns502() throws Exception {
        when(fraudCheckService.check()).thenReturn(Map.of(
                "source", "downstream",
                "integration", "unavailable",
                "error", "ResourceAccessException"
        ));

        mockMvc.perform(get("/api/fraud-check"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.integration").value("unavailable"))
                .andExpect(jsonPath("$.error").value("ResourceAccessException"));
    }

    @Test
    void check_downstreamOk_containsExpectedFields() throws Exception {
        when(fraudCheckService.check()).thenReturn(Map.of(
                "source", "downstream",
                "integration", "ok",
                "result", Map.of("decision", "REVIEW", "riskScore", 45)
        ));

        mockMvc.perform(get("/api/fraud-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").exists())
                .andExpect(jsonPath("$.integration").exists())
                .andExpect(jsonPath("$.result").exists());
    }
}

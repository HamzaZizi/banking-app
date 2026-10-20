package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.service.FraudCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FraudCheckController.class)
class FraudCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FraudCheckService fraudCheckService;

    @Test
    void check_returns200WhenIntegrationOk() throws Exception {
        when(fraudCheckService.check()).thenReturn(Map.of(
                "integration", "ok",
                "source", "downstream",
                "result", Map.of("decision", "APPROVE", "riskScore", 12)
        ));

        mockMvc.perform(get("/api/fraud-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integration").value("ok"))
                .andExpect(jsonPath("$.source").value("downstream"));
    }

    @Test
    void check_returns502WhenIntegrationUnavailable() throws Exception {
        when(fraudCheckService.check()).thenReturn(Map.of(
                "integration", "unavailable",
                "error", "Connection refused"
        ));

        mockMvc.perform(get("/api/fraud-check"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.integration").value("unavailable"))
                .andExpect(jsonPath("$.error").exists());
    }
}

package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCards_returnsAllCards() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].id").value("card-001"));
    }

    @Test
    void getCards_filtersByAccountId() throws Exception {
        mockMvc.perform(get("/api/cards").param("accountId", "acc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].accountId").value("acc-001"))
                .andExpect(jsonPath("$[1].accountId").value("acc-001"));
    }

    @Test
    void getCards_returnsEmptyForUnknownAccount() throws Exception {
        mockMvc.perform(get("/api/cards").param("accountId", "unknown-account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getCard_returnsSingleCard() throws Exception {
        mockMvc.perform(get("/api/cards/card-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("card-001"))
                .andExpect(jsonPath("$.accountId").value("acc-001"))
                .andExpect(jsonPath("$.type").value("DEBIT"))
                .andExpect(jsonPath("$.network").value("Visa"));
    }

    @Test
    void getCard_returns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/cards/unknown-card"))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleFreeze_freezesActiveCard() throws Exception {
        mockMvc.perform(post("/api/cards/card-002/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("card-002"))
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void toggleFreeze_returns404ForUnknownCard() throws Exception {
        mockMvc.perform(post("/api/cards/unknown-card/freeze"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCards_returnsCardsWithExpectedFields() throws Exception {
        mockMvc.perform(get("/api/cards/card-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardholder").exists())
                .andExpect(jsonPath("$.panMasked").exists())
                .andExpect(jsonPath("$.expiry").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.monthlySpend").exists())
                .andExpect(jsonPath("$.limit").exists());
    }
}

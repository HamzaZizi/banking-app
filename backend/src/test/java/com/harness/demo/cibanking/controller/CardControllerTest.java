package com.harness.demo.cibanking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].id").value("card-001"));
    }

    @Test
    void getCards_withAccountIdFilter_returnsOnlyMatchingCards() throws Exception {
        mockMvc.perform(get("/api/cards").param("accountId", "acc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].accountId", everyItem(equalTo("acc-001"))));
    }

    @Test
    void getCards_withBlankAccountId_returnsAllCards() throws Exception {
        mockMvc.perform(get("/api/cards").param("accountId", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void getCards_withUnknownAccountId_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/cards").param("accountId", "unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getCard_returnsMatchingCard() throws Exception {
        mockMvc.perform(get("/api/cards/card-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("card-001"))
                .andExpect(jsonPath("$.type").value("DEBIT"))
                .andExpect(jsonPath("$.network").value("Visa"));
    }

    @Test
    void getCard_returns404ForUnknownCard() throws Exception {
        mockMvc.perform(get("/api/cards/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCard_includesAllCardDetails() throws Exception {
        mockMvc.perform(get("/api/cards/card-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardholderName").exists())
                .andExpect(jsonPath("$.panMasked").exists())
                .andExpect(jsonPath("$.expiry").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.monthlySpend").exists())
                .andExpect(jsonPath("$.spendLimit").exists());
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
        mockMvc.perform(post("/api/cards/unknown/freeze"))
                .andExpect(status().isNotFound());
    }
}

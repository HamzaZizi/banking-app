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
 * Web-layer tests for the card endpoints.
 */
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
    void getCards_withAccountId_returnsFilteredCards() throws Exception {
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
    void getCard_existingCard_returnsCard() throws Exception {
        mockMvc.perform(get("/api/cards/card-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("card-001"))
                .andExpect(jsonPath("$.type").value("DEBIT"))
                .andExpect(jsonPath("$.network").value("Visa"));
    }

    @Test
    void getCard_unknownCard_returns404() throws Exception {
        mockMvc.perform(get("/api/cards/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleFreeze_activeCard_freezesCard() throws Exception {
        mockMvc.perform(post("/api/cards/card-001/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("card-001"))
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void toggleFreeze_frozenCard_unfreezesCard() throws Exception {
        mockMvc.perform(post("/api/cards/card-004/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("card-004"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void toggleFreeze_unknownCard_returns404() throws Exception {
        mockMvc.perform(post("/api/cards/unknown/freeze"))
                .andExpect(status().isNotFound());
    }
}

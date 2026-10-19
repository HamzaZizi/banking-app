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
 * Web-layer tests for the notification endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getNotifications_returnsAllNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].id").value("ntf-001"));
    }

    @Test
    void getNotifications_containsExpectedFields() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].message").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].date").exists())
                .andExpect(jsonPath("$[0].read").exists());
    }

    @Test
    void getUnreadCount_returnsCount() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").isNumber());
    }

    @Test
    void markRead_existingUnreadNotification_returnsUpdatedNotification() throws Exception {
        mockMvc.perform(post("/api/notifications/ntf-001/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ntf-001"))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markRead_alreadyReadNotification_returnsNotification() throws Exception {
        mockMvc.perform(post("/api/notifications/ntf-003/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ntf-003"))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markRead_unknownNotification_returns404() throws Exception {
        mockMvc.perform(post("/api/notifications/unknown/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getNotifications_containsVariousCategories() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category", hasItems("SECURITY", "PAYMENT", "INFO")));
    }
}

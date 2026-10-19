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
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getNotifications_returnsAllNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void getNotifications_includesNotificationDetails() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ntf-001"))
                .andExpect(jsonPath("$[0].date").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].message").exists())
                .andExpect(jsonPath("$[0].read").exists());
    }

    @Test
    void getNotifications_containsVariousCategories() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category", hasItems("SECURITY", "PAYMENT", "INFO", "OFFER")));
    }

    @Test
    void getUnreadCount_returnsUnreadCount() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.unread").isNumber());
    }

    @Test
    void getUnreadCount_returnsPositiveOrZeroCount() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread", greaterThanOrEqualTo(0)));
    }

    @Test
    void markRead_marksNotificationAsRead() throws Exception {
        mockMvc.perform(post("/api/notifications/ntf-001/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ntf-001"))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markRead_returns404ForUnknownNotification() throws Exception {
        mockMvc.perform(post("/api/notifications/unknown/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    void markRead_isIdempotent() throws Exception {
        mockMvc.perform(post("/api/notifications/ntf-003/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ntf-003"))
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(post("/api/notifications/ntf-003/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void getNotifications_firstNotificationIsSecurityAlert() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("SECURITY"))
                .andExpect(jsonPath("$[0].title").value("New device signed in"));
    }
}

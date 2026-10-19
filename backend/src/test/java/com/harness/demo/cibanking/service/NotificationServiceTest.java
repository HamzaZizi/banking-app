package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceTest {

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService();
    }

    @Test
    void getNotifications_returnsAllSeededNotifications() {
        List<Notification> notifications = service.getNotifications();
        assertThat(notifications).hasSize(6);
        assertThat(notifications).extracting(Notification::getId)
                .containsExactly("ntf-001", "ntf-002", "ntf-003", "ntf-004", "ntf-005", "ntf-006");
    }

    @Test
    void getUnreadCount_returnsCorrectCount() {
        long unreadCount = service.getUnreadCount();
        assertThat(unreadCount).isEqualTo(2);
    }

    @Test
    void markRead_marksNotificationAsRead() {
        Notification before = service.getNotifications().stream()
                .filter(n -> n.getId().equals("ntf-001"))
                .findFirst().orElse(null);
        assertThat(before).isNotNull();
        assertThat(before.isRead()).isFalse();

        Notification result = service.markRead("ntf-001");
        assertThat(result).isNotNull();
        assertThat(result.isRead()).isTrue();
        assertThat(result.getId()).isEqualTo("ntf-001");
    }

    @Test
    void markRead_returnsNullForUnknownId() {
        Notification result = service.markRead("unknown-id");
        assertThat(result).isNull();
    }

    @Test
    void markRead_decreasesUnreadCount() {
        long beforeCount = service.getUnreadCount();
        service.markRead("ntf-001");
        long afterCount = service.getUnreadCount();
        assertThat(afterCount).isEqualTo(beforeCount - 1);
    }

    @Test
    void getNotifications_containsExpectedFields() {
        Notification first = service.getNotifications().get(0);
        assertThat(first.getId()).isNotNull();
        assertThat(first.getDate()).isNotNull();
        assertThat(first.getCategory()).isNotNull();
        assertThat(first.getTitle()).isNotNull();
        assertThat(first.getMessage()).isNotNull();
    }
}

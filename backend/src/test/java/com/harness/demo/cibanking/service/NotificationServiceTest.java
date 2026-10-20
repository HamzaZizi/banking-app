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
        long unread = service.getUnreadCount();
        assertThat(unread).isEqualTo(2);
    }

    @Test
    void markRead_marksNotificationAsRead() {
        Notification before = service.getNotifications().stream()
                .filter(n -> n.getId().equals("ntf-001"))
                .findFirst().orElseThrow();
        assertThat(before.isRead()).isFalse();

        Notification result = service.markRead("ntf-001");
        assertThat(result).isNotNull();
        assertThat(result.isRead()).isTrue();
    }

    @Test
    void markRead_decreasesUnreadCount() {
        long before = service.getUnreadCount();
        service.markRead("ntf-002");
        long after = service.getUnreadCount();
        assertThat(after).isEqualTo(before - 1);
    }

    @Test
    void markRead_returnsNullForUnknownId() {
        assertThat(service.markRead("unknown")).isNull();
    }

    @Test
    void markRead_isIdempotentForAlreadyReadNotification() {
        service.markRead("ntf-003");
        Notification result = service.markRead("ntf-003");
        assertThat(result).isNotNull();
        assertThat(result.isRead()).isTrue();
    }
}

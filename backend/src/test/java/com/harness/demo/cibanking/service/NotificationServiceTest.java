package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the in-memory notification inbox service.
 * No Spring context is started - this exercises the service class directly.
 */
class NotificationServiceTest {

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService();
    }

    // ----- getNotifications --------------------------------------------------

    @Test
    void getNotifications_returnsAllSeededNotifications() {
        List<Notification> notifications = service.getNotifications();
        assertThat(notifications).hasSize(6);
        assertThat(notifications).extracting(Notification::getId)
                .containsExactly("ntf-001", "ntf-002", "ntf-003", "ntf-004", "ntf-005", "ntf-006");
    }

    @Test
    void getNotifications_containsVariousCategories() {
        List<Notification> notifications = service.getNotifications();
        assertThat(notifications).extracting(Notification::getCategory)
                .contains("SECURITY", "PAYMENT", "INFO", "OFFER");
    }

    @Test
    void getNotifications_containsReadAndUnreadItems() {
        List<Notification> notifications = service.getNotifications();
        long readCount = notifications.stream().filter(Notification::isRead).count();
        long unreadCount = notifications.stream().filter(n -> !n.isRead()).count();
        
        assertThat(readCount).isGreaterThan(0);
        assertThat(unreadCount).isGreaterThan(0);
    }

    // ----- getUnreadCount ----------------------------------------------------

    @Test
    void getUnreadCount_returnsCorrectInitialCount() {
        long count = service.getUnreadCount();
        // From seed data: ntf-001 and ntf-002 are unread (read=false)
        assertThat(count).isEqualTo(2);
    }

    @Test
    void getUnreadCount_matchesManualCount() {
        long expected = service.getNotifications().stream()
                .filter(n -> !n.isRead()).count();
        assertThat(service.getUnreadCount()).isEqualTo(expected);
    }

    // ----- markRead ----------------------------------------------------------

    @Test
    void markRead_marksUnreadNotificationAsRead() {
        // ntf-001 starts unread
        Notification before = service.getNotifications().stream()
                .filter(n -> n.getId().equals("ntf-001")).findFirst().orElse(null);
        assertThat(before).isNotNull();
        assertThat(before.isRead()).isFalse();

        Notification updated = service.markRead("ntf-001");

        assertThat(updated).isNotNull();
        assertThat(updated.isRead()).isTrue();
        assertThat(updated.getId()).isEqualTo("ntf-001");
    }

    @Test
    void markRead_decrementsUnreadCount() {
        long countBefore = service.getUnreadCount();
        
        service.markRead("ntf-001");
        
        long countAfter = service.getUnreadCount();
        assertThat(countAfter).isEqualTo(countBefore - 1);
    }

    @Test
    void markRead_isIdempotentForAlreadyReadNotification() {
        // ntf-003 is already read
        Notification before = service.getNotifications().stream()
                .filter(n -> n.getId().equals("ntf-003")).findFirst().orElse(null);
        assertThat(before).isNotNull();
        assertThat(before.isRead()).isTrue();

        long countBefore = service.getUnreadCount();
        Notification updated = service.markRead("ntf-003");
        long countAfter = service.getUnreadCount();

        assertThat(updated).isNotNull();
        assertThat(updated.isRead()).isTrue();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void markRead_returnsNullForUnknownNotification() {
        Notification result = service.markRead("does-not-exist");
        assertThat(result).isNull();
    }

    @Test
    void markRead_doesNotAffectOtherNotifications() {
        long countBefore = service.getUnreadCount();
        
        // Mark one as read
        service.markRead("ntf-002");
        
        // Check ntf-001 is still unread
        Notification ntf001 = service.getNotifications().stream()
                .filter(n -> n.getId().equals("ntf-001")).findFirst().orElse(null);
        assertThat(ntf001).isNotNull();
        assertThat(ntf001.isRead()).isFalse();
    }

    @Test
    void markRead_allNotifications_reducesUnreadCountToZero() {
        // Mark all unread notifications as read
        service.getNotifications().stream()
                .filter(n -> !n.isRead())
                .forEach(n -> service.markRead(n.getId()));
        
        assertThat(service.getUnreadCount()).isZero();
    }

    // ----- Notification properties -------------------------------------------

    @Test
    void notifications_haveValidDates() {
        List<Notification> notifications = service.getNotifications();
        assertThat(notifications).allSatisfy(n -> {
            assertThat(n.getDate()).isNotNull();
            assertThat(n.getDate()).matches("\\d{4}-\\d{2}-\\d{2}");
        });
    }

    @Test
    void notifications_haveTitlesAndMessages() {
        List<Notification> notifications = service.getNotifications();
        assertThat(notifications).allSatisfy(n -> {
            assertThat(n.getTitle()).isNotNull().isNotBlank();
            assertThat(n.getMessage()).isNotNull().isNotBlank();
        });
    }
}

package com.harness.demo.cibanking.service;

import com.harness.demo.cibanking.model.Notification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dummy in-memory inbox notifications for demo purposes only.
 */
@Service
public class NotificationService {

    private final List<Notification> notifications = new CopyOnWriteArrayList<>(List.of(
            new Notification("ntf-001", "2026-07-16", "SECURITY",
                    "New device signed in",
                    "We noticed a sign-in from a new device in London. If this was you, no action is needed.",
                    false),
            new Notification("ntf-002", "2026-07-14", "PAYMENT",
                    "Salary received",
                    "Your salary of £2,650.00 from Harness Ltd has cleared into your Everyday Current Account.",
                    false),
            new Notification("ntf-003", "2026-07-13", "PAYMENT",
                    "Direct Debit paid",
                    "A Direct Debit of £96.20 to British Gas was paid from your Everyday Current Account.",
                    true),
            new Notification("ntf-004", "2026-07-10", "INFO",
                    "Statement ready",
                    "Your June statement for the Business C&I Account is now available to download.",
                    true),
            new Notification("ntf-005", "2026-07-05", "OFFER",
                    "Boost your savings",
                    "You could earn more on your balance with our Digital Regular Saver. Tap to learn more.",
                    true),
            new Notification("ntf-006", "2026-06-30", "SECURITY",
                    "Card used abroad",
                    "Your Mastercard credit card was used for an online payment in USD. Review it in your inbox.",
                    true)
    ));

    public List<Notification> getNotifications() {
        return notifications;
    }

    public long getUnreadCount() {
        return notifications.stream().filter(n -> !n.isRead()).count();
    }

    /** Mark a notification read. Returns the updated notification, or null if not found. */
    public Notification markRead(String id) {
        Notification n = notifications.stream()
                .filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (n == null) {
            return null;
        }
        n.setRead(true);
        return n;
    }
}

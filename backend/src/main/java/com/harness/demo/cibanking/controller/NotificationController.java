package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.model.Notification;
import com.harness.demo.cibanking.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Notification> getNotifications() {
        return notificationService.getNotifications();
    }

    @GetMapping("/unread-count")
    public Map<String, Object> getUnreadCount() {
        return Map.of("unread", notificationService.getUnreadCount());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable String id) {
        Notification n = notificationService.markRead(id);
        if (n == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(n);
    }
}

package com.codesync.notification.controller;

import com.codesync.notification.dto.AdminBroadcastRequest;
import com.codesync.notification.dto.NotificationDTO;
import com.codesync.notification.dto.NotificationEventDTO;
import com.codesync.notification.dto.SystemNotificationRequest;
import com.codesync.notification.dto.UnreadCountDTO;
import com.codesync.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationsByUser(userId));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<UnreadCountDTO> getUnreadCount(@PathVariable Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(UnreadCountDTO.builder()
                .userId(userId)
                .unreadCount(count)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDTO> getNotification(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotification(id));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/system")
    public ResponseEntity<NotificationDTO> createSystemNotification(
            @Valid @RequestBody SystemNotificationRequest request) {
        NotificationDTO created = notificationService.createNotification(NotificationEventDTO.builder()
                .userId(request.userId())
                .type(request.type())
                .message(request.message())
                .build());
        return ResponseEntity.ok(created);
    }

    @PostMapping("/admin/broadcast")
    public ResponseEntity<?> broadcastNotifications(
            @Valid @RequestBody AdminBroadcastRequest request) {
        List<NotificationDTO> notifications = notificationService.createNotificationsForUsers(
                request.userIds(),
                request.type(),
                request.message());
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Notifications sent",
                "count", notifications.size()));
    }
}

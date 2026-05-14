package com.codesync.notification.service;

import com.codesync.notification.dto.NotificationDTO;
import com.codesync.notification.dto.NotificationEventDTO;
import com.codesync.notification.entity.Notification;
import com.codesync.notification.exception.NotificationNotFoundException;
import com.codesync.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationDTO createNotification(NotificationEventDTO event) {
        log.info("Creating notification for user {}: {}", event.getUserId(), event.getMessage());
        
        LocalDateTime now = LocalDateTime.now();
        
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .type(event.getType())
                .message(event.getMessage())
                .isRead(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", saved.getId());
        return mapToDTO(saved);
    }

    public List<NotificationDTO> createNotificationsForUsers(List<Long> userIds, String type, String message) {
        log.info("Broadcasting notifications to {} users, type: {}, message: {}", userIds.size(), type, message);
        List<NotificationDTO> results = new ArrayList<>();
        for (Long userId : userIds) {
            results.add(createNotification(NotificationEventDTO.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .build()));
        }
        return results;
    }

    public NotificationDTO getNotification(Long id) {
        log.info("Fetching notification with ID: {}", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + id));
        return mapToDTO(notification);
    }

    public List<NotificationDTO> getNotificationsByUser(Long userId) {
        log.info("Fetching notifications for user: {}", userId);
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<NotificationDTO> getUnreadNotificationsByUser(Long userId) {
        log.info("Fetching unread notifications for user: {}", userId);
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        log.info("Getting unread count for user: {}", userId);
        return notificationRepository.countUnreadByUserId(userId);
    }

    public NotificationDTO markAsRead(Long id) {
        log.info("Marking notification {} as read", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + id));
        notification.setIsRead(true);
        Notification updated = notificationRepository.save(notification);
        return mapToDTO(updated);
    }

    public void deleteNotification(Long id) {
        log.info("Deleting notification with ID: {}", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + id));
        notificationRepository.delete(notification);
    }

    private NotificationDTO mapToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

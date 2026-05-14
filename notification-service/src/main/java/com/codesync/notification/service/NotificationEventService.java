package com.codesync.notification.service;

import com.codesync.notification.entity.Notification;
import com.codesync.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationEventService {
    private final NotificationRepository notificationRepository;
    private final EmailClient emailClient;

    public enum EventType {
        SESSION_INVITE("SESSION_INVITE", "You have been invited to a collaboration session"),
        PARTICIPANT_JOINED("PARTICIPANT_JOINED", "A new participant joined the session"),
        PARTICIPANT_LEFT("PARTICIPANT_LEFT", "A participant left the session"),
        COMMENT_ADDED("COMMENT_ADDED", "A new comment was added"),
        MENTION("MENTION", "You were mentioned in a comment"),
        SNAPSHOT_CREATED("SNAPSHOT_CREATED", "A new snapshot was created"),
        PROJECT_FORKED("PROJECT_FORKED", "Your project was forked"),
        PROJECT_STARRED("PROJECT_STARRED", "Someone starred your project"),
        PROJECT_INVITE("PROJECT_INVITE", "You have been invited to collaborate on a project"),
        PROJECT_INVITE_ACCEPTED("PROJECT_INVITE_ACCEPTED", "User accepted your project invitation"),
        ADMIN_REQUEST("ADMIN_REQUEST", "New admin access request"),
        ADMIN_APPROVED("ADMIN_APPROVED", "Your admin access request was approved");

        private final String type;
        private final String defaultMessage;

        EventType(String type, String defaultMessage) {
            this.type = type;
            this.defaultMessage = defaultMessage;
        }

        public String getType() { return type; }
        public String getDefaultMessage() { return defaultMessage; }
    }

    @Transactional
    public Notification createNotification(Long userId, EventType eventType, String message, Long actorId, Long relatedId, String deepLinkUrl) {
        String finalMessage = message != null ? message : eventType.getDefaultMessage();
        
        Notification notification = Notification.builder()
                .userId(userId)
                .type(eventType.getType())
                .message(finalMessage)
                .isRead(false)
                .actorId(actorId)
                .relatedId(relatedId)
                .deepLinkUrl(deepLinkUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Notification saved = notificationRepository.save(notification);
        
        if (shouldSendEmail(eventType)) {
            emailClient.sendNotificationEmail(userId, finalMessage, eventType);
        }
        
        return saved;
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        for (Notification n : unread) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    private boolean shouldSendEmail(EventType eventType) {
        return eventType == EventType.SESSION_INVITE 
                || eventType == EventType.MENTION 
                || eventType == EventType.PROJECT_INVITE
                || eventType == EventType.ADMIN_APPROVED;
    }
}
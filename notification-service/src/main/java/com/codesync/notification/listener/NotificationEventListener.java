package com.codesync.notification.listener;

import com.codesync.notification.dto.NotificationEventDTO;
import com.codesync.notification.dto.UnreadCountDTO;
import com.codesync.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationEvent(String message) {
        try {
            log.info("Received notification event: {}", message);
            NotificationEventDTO event = objectMapper.readValue(message, NotificationEventDTO.class);
            
            // Create notification in database
            notificationService.createNotification(event);
            
            // Send real-time update via WebSocket
            long unreadCount = notificationService.getUnreadCount(event.getUserId());
            UnreadCountDTO countDTO = UnreadCountDTO.builder()
                    .userId(event.getUserId())
                    .unreadCount(unreadCount)
                    .build();
            
            messagingTemplate.convertAndSendToUser(
                    event.getUserId().toString(),
                    "/notifications",
                    countDTO
            );
            
            log.info("Notification sent to user: {}", event.getUserId());
        } catch (Exception ex) {
            log.error("Error processing notification event: ", ex);
        }
    }
}

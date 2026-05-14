package com.codesync.notification.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    void shouldCreateNotification() {
        Notification notification = Notification.builder()
                .id(1L)
                .userId(100L)
                .type("COMMENT")
                .message("User commented on your file")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        assertNotNull(notification);
        assertEquals(100L, notification.getUserId());
        assertEquals("COMMENT", notification.getType());
        assertEquals(false, notification.getIsRead());
    }

    @Test
    void shouldSetNotificationFields() {
        Notification notification = new Notification();
        notification.setId(2L);
        notification.setUserId(200L);
        notification.setType("MENTION");
        notification.setMessage("Check your mentions");
        notification.setIsRead(true);

        assertEquals(2L, notification.getId());
        assertEquals("MENTION", notification.getType());
        assertEquals(true, notification.getIsRead());
    }
}
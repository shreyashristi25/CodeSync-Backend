package com.codesync.notification.service;

import com.codesync.notification.entity.Notification;
import com.codesync.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailClient emailClient;

    @InjectMocks
    private NotificationEventService notificationEventService;

    @Test
    void shouldCreateNotification() {
        Notification notification = Notification.builder()
                .id(1L).userId(1L).type("SESSION_INVITE").message("Test").isRead(false).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        Notification result = notificationEventService.createNotification(
                1L, NotificationEventService.EventType.SESSION_INVITE, "Test message", 2L, 3L, "http://link");

        assertNotNull(result);
        assertEquals("SESSION_INVITE", result.getType());
    }

    @Test
    void shouldCreateNotificationWithDefaultMessage() {
        Notification notification = Notification.builder()
                .id(1L).userId(1L).type("MENTION").message("You were mentioned in a comment").isRead(false).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        Notification result = notificationEventService.createNotification(
                1L, NotificationEventService.EventType.MENTION, null, 2L, 3L, "http://link");

        assertNotNull(result);
        assertEquals("You were mentioned in a comment", result.getMessage());
    }

    @Test
    void shouldSendEmailForSessionInvite() {
        Notification notification = Notification.builder()
                .id(1L).userId(1L).type("SESSION_INVITE").message("Test").isRead(false).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationEventService.createNotification(
                1L, NotificationEventService.EventType.SESSION_INVITE, "Test message", 2L, 3L, "http://link");

        verify(emailClient).sendNotificationEmail(any(), any(), any());
    }

    @Test
    void shouldSendEmailForMention() {
        Notification notification = Notification.builder()
                .id(1L).userId(1L).type("MENTION").message("Test").isRead(false).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationEventService.createNotification(
                1L, NotificationEventService.EventType.MENTION, "Test", 2L, 3L, "http://link");

        verify(emailClient).sendNotificationEmail(any(), any(), any());
    }

    @Test
    void shouldNotSendEmailForComment() {
        Notification notification = Notification.builder()
                .id(1L).userId(1L).type("COMMENT_ADDED").message("Test").isRead(false).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationEventService.createNotification(
                1L, NotificationEventService.EventType.COMMENT_ADDED, "Test", 2L, 3L, "http://link");

        verify(emailClient, never()).sendNotificationEmail(any(), any(), any());
    }

    @Test
    void shouldGetUnreadNotifications() {
        List<Notification> notifications = List.of(
                Notification.builder().id(1L).userId(1L).isRead(false).build()
        );
        when(notificationRepository.findByUserIdAndIsReadFalse(1L)).thenReturn(notifications);

        List<Notification> result = notificationEventService.getUnreadNotifications(1L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetUnreadCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = notificationEventService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    @Test
    void shouldMarkAsRead() {
        Notification notification = Notification.builder().id(1L).isRead(false).build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenReturn(notification);

        notificationEventService.markAsRead(1L);

        verify(notificationRepository).save(any());
    }

    @Test
    void shouldMarkAllAsRead() {
        List<Notification> notifications = List.of(
                Notification.builder().id(1L).isRead(false).build(),
                Notification.builder().id(2L).isRead(false).build()
        );
        when(notificationRepository.findByUserIdAndIsReadFalse(1L)).thenReturn(notifications);

        notificationEventService.markAllAsRead(1L);

        verify(notificationRepository).saveAll(notifications);
    }

    @Test
    void shouldCheckEventTypeEnum() {
        assertEquals("SESSION_INVITE", NotificationEventService.EventType.SESSION_INVITE.getType());
        assertEquals("MENTION", NotificationEventService.EventType.MENTION.getType());
        assertNotNull(NotificationEventService.EventType.MENTION.getDefaultMessage());
    }
}
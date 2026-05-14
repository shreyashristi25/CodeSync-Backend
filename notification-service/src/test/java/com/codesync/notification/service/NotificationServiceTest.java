package com.codesync.notification.service;

import com.codesync.notification.dto.NotificationDTO;
import com.codesync.notification.dto.NotificationEventDTO;
import com.codesync.notification.entity.Notification;
import com.codesync.notification.exception.NotificationNotFoundException;
import com.codesync.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private NotificationEventDTO testEvent;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .userId(1L)
                .type("COMMENT_MENTION")
                .message("You were mentioned in a comment")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testEvent = NotificationEventDTO.builder()
                .userId(1L)
                .type("COMMENT_MENTION")
                .message("You were mentioned in a comment")
                .build();
    }

    @Test
    void testCreateNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        NotificationDTO result = notificationService.createNotification(testEvent);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals("COMMENT_MENTION", result.getType());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testGetNotification_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        NotificationDTO result = notificationService.getNotification(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(notificationRepository, times(1)).findById(1L);
    }

    @Test
    void testGetNotification_NotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.getNotification(999L));
    }

    @Test
    void testGetNotificationsByUser() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(testNotification));

        List<NotificationDTO> result = notificationService.getNotificationsByUser(1L);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testGetUnreadNotificationsByUser() {
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(testNotification));

        List<NotificationDTO> result = notificationService.getUnreadNotificationsByUser(1L);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testGetUnreadCount() {
        when(notificationRepository.countUnreadByUserId(1L)).thenReturn(3L);

        long result = notificationService.getUnreadCount(1L);

        assertEquals(3L, result);
        verify(notificationRepository, times(1)).countUnreadByUserId(1L);
    }

    @Test
    void testMarkAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        testNotification.setIsRead(true);
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        NotificationDTO result = notificationService.markAsRead(1L);

        assertNotNull(result);
        assertTrue(result.getIsRead());
    }

    @Test
    void testDeleteNotification_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        notificationService.deleteNotification(1L);

        verify(notificationRepository, times(1)).delete(testNotification);
    }

    @Test
    void testDeleteNotification_NotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.deleteNotification(999L));
    }
}

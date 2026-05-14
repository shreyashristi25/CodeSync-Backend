package com.codesync.notification.controller;

import com.codesync.notification.dto.NotificationDTO;
import com.codesync.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Disabled
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;
    private NotificationDTO testNotificationDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
        
        testNotificationDTO = NotificationDTO.builder()
                .id(1L)
                .userId(1L)
                .type("COMMENT_MENTION")
                .message("You were mentioned in a comment")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetNotificationsByUser() throws Exception {
        when(notificationService.getNotificationsByUser(1L))
                .thenReturn(List.of(testNotificationDTO));

        mockMvc.perform(get("/api/notifications/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].type").value("COMMENT_MENTION"));
    }

    @Test
    void testGetUnreadNotifications() throws Exception {
        when(notificationService.getUnreadNotificationsByUser(1L))
                .thenReturn(List.of(testNotificationDTO));

        mockMvc.perform(get("/api/notifications/user/1/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRead").value(false));
    }

    @Test
    void testGetUnreadCount() throws Exception {
        when(notificationService.getUnreadCount(1L)).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/user/1/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3L));
    }

    @Test
    void testGetNotification() throws Exception {
        when(notificationService.getNotification(1L)).thenReturn(testNotificationDTO);

        mockMvc.perform(get("/api/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void testMarkAsRead() throws Exception {
        testNotificationDTO.setIsRead(true);
        when(notificationService.markAsRead(1L)).thenReturn(testNotificationDTO);

        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void testDeleteNotification() throws Exception {
        doNothing().when(notificationService).deleteNotification(1L);

        mockMvc.perform(delete("/api/notifications/1"))
                .andExpect(status().isNoContent());

        verify(notificationService, times(1)).deleteNotification(1L);
    }
}

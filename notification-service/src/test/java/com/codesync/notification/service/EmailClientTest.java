package com.codesync.notification.service;

import com.codesync.notification.service.NotificationEventService.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Test
    void shouldSendNotificationEmail() {
        EmailClient client = new EmailClient(restTemplateBuilder, "http://auth-service");
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);

        client.sendNotificationEmail(1L, "Test message", EventType.SESSION_INVITE);

        verify(restTemplate).postForEntity(eq("http://auth-service/api/notifications/email"), any(), eq(ResponseEntity.class));
    }

    @Test
    void shouldSendSessionInviteEmail() {
        EmailClient client = new EmailClient(restTemplateBuilder, "http://auth-service");
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);

        client.sendSessionInviteEmail("test@test.com", "Test User", "Inviter", "Project", "http://link");

        verify(restTemplate).postForEntity(eq("http://auth-service/api/notifications/send"), any(), eq(ResponseEntity.class));
    }

    @Test
    void shouldSendMentionNotificationEmail() {
        EmailClient client = new EmailClient(restTemplateBuilder, "http://auth-service");
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);

        client.sendMentionNotificationEmail("test@test.com", "Test User", "Mentioner", "Project", "Comment", "http://link");

        verify(restTemplate).postForEntity(eq("http://auth-service/api/notifications/send"), any(), eq(ResponseEntity.class));
    }

    @Test
    void shouldSendSnapshotNotificationEmail() {
        EmailClient client = new EmailClient(restTemplateBuilder, "http://auth-service");
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);

        client.sendSnapshotNotificationEmail("test@test.com", "Test User", "Creator", "Project", "Message", "http://link");

        verify(restTemplate).postForEntity(eq("http://auth-service/api/notifications/send"), any(), eq(ResponseEntity.class));
    }
}
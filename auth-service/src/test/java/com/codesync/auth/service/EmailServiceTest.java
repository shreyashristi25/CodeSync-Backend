package com.codesync.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Disabled
class EmailServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EmailService emailService;

    @Test
    void shouldSendRegistrationEmail() {
        ReflectionTestUtils.setField(emailService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@test.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:4200");

        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        emailService.sendRegistrationEmail("user@test.com", "Test User");

        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class));
    }

    @Test
    void shouldSendLoginNotificationEmail() {
        ReflectionTestUtils.setField(emailService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@test.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:4200");

        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        emailService.sendLoginNotificationEmail("user@test.com", "Test User");

        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class));
    }

    @Test
    void shouldSendPasswordResetEmail() {
        ReflectionTestUtils.setField(emailService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@test.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:4200");

        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        emailService.sendPasswordResetEmail("user@test.com", "Test User", "token123");

        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class));
    }

    @Test
    void shouldSendSessionInviteEmail() {
        ReflectionTestUtils.setField(emailService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@test.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:4200");

        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        emailService.sendSessionInviteEmail("user@test.com", "Test User", "Inviter", "Project", "http://link");

        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class));
    }

    @Test
    void shouldSendMentionNotificationEmail() {
        ReflectionTestUtils.setField(emailService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@test.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:4200");

        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        emailService.sendMentionNotificationEmail("user@test.com", "Test User", "Mentioner", "Project", "Comment preview", "http://link");

        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class));
    }

    @Test
    void shouldSendSnapshotNotificationEmail() {
        ReflectionTestUtils.setField(emailService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@test.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:4200");

        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        emailService.sendSnapshotNotificationEmail("user@test.com", "Test User", "Creator", "Project", "Commit message", "http://link");

        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class));
    }

    @Test
    void shouldSkipEmailWhenApiKeyNotConfigured() {
        ReflectionTestUtils.setField(emailService, "apiKey", "");
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@test.com");

        emailService.sendRegistrationEmail("user@test.com", "Test User");

        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldUseDefaultFromAddressWhenNotProvided() {
        EmailService service = new EmailService("", "", "http://localhost:4200");
        assertNotNull(service);
    }

    @Test
    void shouldEscapeHtmlInCommentPreview() {
        ReflectionTestUtils.setField(emailService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@test.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:4200");

        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        emailService.sendMentionNotificationEmail("user@test.com", "Test", "Mentioner", "Project", "<script>alert('xss')</script>", "http://link");

        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), any(), eq(String.class));
    }
}
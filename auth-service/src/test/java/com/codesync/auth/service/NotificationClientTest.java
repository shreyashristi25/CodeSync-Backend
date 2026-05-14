package com.codesync.auth.service;

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
class NotificationClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Test
    void shouldSendSystemNotification() {
        NotificationClient client = new NotificationClient(restTemplateBuilder, "http://notification-service");
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);

        client.sendSystemNotification(1L, "INFO", "Test message");

        verify(restTemplate).postForEntity(eq("http://notification-service/system"), any(), eq(ResponseEntity.class));
    }
}
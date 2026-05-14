package com.codesync.auth.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NotificationClient(RestTemplateBuilder builder,
                              @Value("${app.notifications.url}") String baseUrl) {
        this.restTemplate = builder.build();
        this.baseUrl = baseUrl;
    }

    public void sendSystemNotification(Long userId, String type, String message) {
        Map<String, Object> payload = Map.of(
                "userId", userId,
                "type", type,
                "message", message
        );
        restTemplate.postForEntity(baseUrl + "/system", payload, ResponseEntity.class);
    }
}

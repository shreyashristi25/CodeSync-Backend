package com.codesync.notification.service;

import com.codesync.notification.service.NotificationEventService.EventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class EmailClient {
    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public EmailClient(RestTemplateBuilder builder,
                      @Value("${app.services.auth}") String authServiceUrl) {
        this.restTemplate = builder.build();
        this.authServiceUrl = authServiceUrl;
    }

    @Async
    public void sendNotificationEmail(Long userId, String message, EventType eventType) {
        try {
            Map<String, Object> payload = Map.of(
                    "userId", userId,
                    "message", message,
                    "eventType", eventType.name()
            );
            
            restTemplate.postForEntity(authServiceUrl + "/api/notifications/email", payload, ResponseEntity.class);
        } catch (Exception e) {
            // Log error but don't fail the notification
            System.err.println("Failed to send notification email: " + e.getMessage());
        }
    }

    public void sendSessionInviteEmail(String email, String name, String inviterName, String projectName, String sessionLink) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", "SESSION_INVITE",
                    "email", email,
                    "name", name,
                    "inviterName", inviterName,
                    "projectName", projectName,
                    "sessionLink", sessionLink
            );
            
            restTemplate.postForEntity(authServiceUrl + "/api/notifications/send", payload, ResponseEntity.class);
        } catch (Exception e) {
            System.err.println("Failed to send session invite email: " + e.getMessage());
        }
    }

    public void sendMentionNotificationEmail(String email, String name, String mentionerName, String projectName, String commentPreview, String commentLink) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", "MENTION",
                    "email", email,
                    "name", name,
                    "mentionerName", mentionerName,
                    "projectName", projectName,
                    "commentPreview", commentPreview,
                    "commentLink", commentLink
            );
            
            restTemplate.postForEntity(authServiceUrl + "/api/notifications/send", payload, ResponseEntity.class);
        } catch (Exception e) {
            System.err.println("Failed to send mention email: " + e.getMessage());
        }
    }

    public void sendSnapshotNotificationEmail(String email, String name, String creatorName, String projectName, String snapshotMessage, String snapshotLink) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", "SNAPSHOT",
                    "email", email,
                    "name", name,
                    "creatorName", creatorName,
                    "projectName", projectName,
                    "snapshotMessage", snapshotMessage,
                    "snapshotLink", snapshotLink
            );
            
            restTemplate.postForEntity(authServiceUrl + "/api/notifications/send", payload, ResponseEntity.class);
        } catch (Exception e) {
            System.err.println("Failed to send snapshot email: " + e.getMessage());
        }
    }
}
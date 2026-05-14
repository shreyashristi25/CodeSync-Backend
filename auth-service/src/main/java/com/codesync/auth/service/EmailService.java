package com.codesync.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class EmailService {
    private final String apiKey;
    private final String fromAddress;
    private final String baseUrl;
    private final RestTemplate restTemplate;

    public EmailService(
                        @Value("${app.email.api-key:}") String apiKey,
                        @Value("${app.email.from:}") String fromAddress,
                        @Value("${app.email.base-url:http://localhost:4200}") String baseUrl) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress == null || fromAddress.isBlank() ? "onboarding@resend.dev" : fromAddress;
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    @Async
    public void sendRegistrationEmail(String email, String name) {
        System.out.println(">>> sendRegistrationEmail called for: " + email);
        sendEmail(email, "Welcome to CodeSync!",
            "<html><body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "<h2 style='color: #2563eb;'>Welcome to CodeSync, " + name + "!</h2>" +
            "<p>Thank you for registering with CodeSync. We're excited to have you on board!</p>" +
            "<p>With CodeSync, you can collaborate on code in real-time with your team.</p>" +
            "<p>Get started by creating your first project and inviting your collaborators.</p>" +
            "<br><p>Best regards,<br>The CodeSync Team</p>" +
            "</body></html>");
    }

    @Async
    public void sendLoginNotificationEmail(String email, String name) {
        System.out.println(">>> sendLoginNotificationEmail called for: " + email);
        sendEmail(email, "New Login to Your CodeSync Account",
            "<html><body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "<h2 style='color: #2563eb;'>Login Notification</h2>" +
            "<p>Hi " + name + ",</p>" +
            "<p>We noticed a successful login to your CodeSync account.</p>" +
            "<p>If this was you, you can safely ignore this email.</p>" +
            "<p>If you didn't log in, please reset your password immediately.</p>" +
            "<br><p>Best regards,<br>The CodeSync Team</p>" +
            "</body></html>");
    }

    @Async
    public void sendPasswordResetEmail(String email, String name, String resetToken) {
        System.out.println(">>> sendPasswordResetEmail called for: " + email);
        String resetUrl = baseUrl + "/reset-password?token=" + resetToken + "&email=" + email;
        sendEmail(email, "Reset Your CodeSync Password",
            "<html><body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "<h2 style='color: #2563eb;'>Password Reset Request</h2>" +
            "<p>Hi " + name + ",</p>" +
            "<p>We received a request to reset your password. Click the button below to create a new password:</p>" +
            "<p style='margin: 20px 0;'><a href='" + resetUrl + "' style='background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;'>Reset Password</a></p>" +
            "<p>Or copy this link: <a href='" + resetUrl + "'>" + resetUrl + "</a></p>" +
            "<p>This link will expire in 30 minutes.</p>" +
            "<p>If you didn't request a password reset, please ignore this email.</p>" +
            "<br><p>Best regards,<br>The CodeSync Team</p>" +
            "</body></html>");
    }

    @Async
    public void sendSessionInviteEmail(String email, String name, String inviterName, String projectName, String sessionLink) {
        System.out.println(">>> sendSessionInviteEmail called for: " + email);
        sendEmail(email, inviterName + " invited you to collaborate on " + projectName,
            "<html><body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "<h2 style='color: #2563eb;'>Collaboration Invite</h2>" +
            "<p>Hi " + name + ",</p>" +
            "<p><strong>" + inviterName + "</strong> has invited you to join a collaboration session on the project <strong>" + projectName + "</strong>.</p>" +
            "<p>Click the button below to join the session:</p>" +
            "<p style='margin: 20px 0;'><a href='" + sessionLink + "' style='background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;'>Join Session</a></p>" +
            "<p>Or copy this link: <a href='" + sessionLink + "'>" + sessionLink + "</a></p>" +
            "<p>This link will expire in 30 minutes.</p>" +
            "<br><p>Best regards,<br>The CodeSync Team</p>" +
            "</body></html>");
    }

    @Async
    public void sendMentionNotificationEmail(String email, String name, String mentionerName, String projectName, String commentPreview, String commentLink) {
        System.out.println(">>> sendMentionNotificationEmail called for: " + email);
        sendEmail(email, mentionerName + " mentioned you in " + projectName,
            "<html><body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "<h2 style='color: #2563eb;'>You were mentioned</h2>" +
            "<p>Hi " + name + ",</p>" +
            "<p><strong>" + mentionerName + "</strong> mentioned you in a comment on <strong>" + projectName + "</strong>.</p>" +
            "<div style='background: #f3f4f6; padding: 15px; border-radius: 6px; margin: 15px 0;'>" +
            "<p style='margin: 0; color: #4b5563;'>" + escapeHtml(commentPreview) + "</p>" +
            "</div>" +
            "<p style='margin: 20px 0;'><a href='" + commentLink + "' style='background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;'>View Comment</a></p>" +
            "<br><p>Best regards,<br>The CodeSync Team</p>" +
            "</body></html>");
    }

    @Async
    public void sendSnapshotNotificationEmail(String email, String name, String creatorName, String projectName, String snapshotMessage, String snapshotLink) {
        System.out.println(">>> sendSnapshotNotificationEmail called for: " + email);
        sendEmail(email, "New snapshot created in " + projectName,
            "<html><body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "<h2 style='color: #2563eb;'>New Snapshot Created</h2>" +
            "<p>Hi " + name + ",</p>" +
            "<p><strong>" + creatorName + "</strong> created a new snapshot in <strong>" + projectName + "</strong>.</p>" +
            "<div style='background: #f3f4f6; padding: 15px; border-radius: 6px; margin: 15px 0;'>" +
            "<p style='margin: 0; color: #4b5563;'><strong>Message:</strong> " + escapeHtml(snapshotMessage) + "</p>" +
            "</div>" +
            "<p style='margin: 20px 0;'><a href='" + snapshotLink + "' style='background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;'>View Snapshot</a></p>" +
            "<br><p>Best regards,<br>The CodeSync Team</p>" +
            "</body></html>");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("Resend API key not configured. Email to " + to + " - Subject: " + subject);
            return;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = Map.of(
                "from", fromAddress,
                "to", to,
                "subject", subject,
                "html", htmlContent
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.resend.com/emails",
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Email sent successfully to " + to);
            } else {
                System.err.println("Failed to send email to " + to + " - Status: " + response.getStatusCode() + " - Body: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("Error sending email to " + to + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
        }
    }
}
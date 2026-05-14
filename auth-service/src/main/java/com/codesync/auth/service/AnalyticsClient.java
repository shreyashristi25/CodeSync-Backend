package com.codesync.auth.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AnalyticsClient {
    private final RestTemplate restTemplate;
    private final String projectServiceUrl;
    private final String executionServiceUrl;
    private final String collabServiceUrl;

    public AnalyticsClient(RestTemplateBuilder builder,
                     @Value("${app.services.project}") String projectServiceUrl,
                     @Value("${app.services.execution}") String executionServiceUrl,
                     @Value("${app.services.collab}") String collabServiceUrl) {
        this.restTemplate = builder.build();
        this.projectServiceUrl = projectServiceUrl;
        this.executionServiceUrl = executionServiceUrl;
        this.collabServiceUrl = collabServiceUrl;
    }

    public Map<String, Object> getProjectStats() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(projectServiceUrl + "/api/projects/admin/stats", Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Failed to fetch project stats: " + e.getMessage());
            return Map.of();
        }
    }

    public Map<String, Object> getExecutionStats() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(executionServiceUrl + "/api/execution/admin/stats", Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Failed to fetch execution stats: " + e.getMessage());
            return Map.of();
        }
    }

    public Map<String, Object> getCollabStats() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(collabServiceUrl + "/api/collab/admin/stats", Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Failed to fetch collab stats: " + e.getMessage());
            return Map.of();
        }
    }
}
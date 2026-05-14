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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Test
    void shouldGetProjectStats() {
        AnalyticsClient client = new AnalyticsClient(restTemplateBuilder, "http://project-service", "http://exec-service", "http://collab-service");
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);

        when(restTemplate.getForEntity(eq("http://project-service/admin/stats"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("total", 10)));

        Map<String, Object> result = client.getProjectStats();

        assertNotNull(result);
        assertEquals(10, result.get("total"));
    }

    @Test
    void shouldGetExecutionStats() {
        AnalyticsClient client = new AnalyticsClient(restTemplateBuilder, "http://project-service", "http://exec-service", "http://collab-service");
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);

        when(restTemplate.getForEntity(eq("http://exec-service/admin/stats"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("executions", 50)));

        Map<String, Object> result = client.getExecutionStats();

        assertNotNull(result);
        assertEquals(50, result.get("executions"));
    }

    @Test
    void shouldGetCollabStats() {
        AnalyticsClient client = new AnalyticsClient(restTemplateBuilder, "http://project-service", "http://exec-service", "http://collab-service");
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);

        when(restTemplate.getForEntity(eq("http://collab-service/admin/stats"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("sessions", 25)));

        Map<String, Object> result = client.getCollabStats();

        assertNotNull(result);
        assertEquals(25, result.get("sessions"));
    }

    @Test
    void shouldReturnEmptyMapOnError() {
        AnalyticsClient client = new AnalyticsClient(restTemplateBuilder, "http://project-service", "http://exec-service", "http://collab-service");
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);

        when(restTemplate.getForEntity(eq("http://project-service/admin/stats"), eq(Map.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        Map<String, Object> result = client.getProjectStats();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
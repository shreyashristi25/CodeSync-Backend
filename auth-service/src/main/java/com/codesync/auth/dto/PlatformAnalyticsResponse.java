package com.codesync.auth.dto;

import java.util.Map;

public record PlatformAnalyticsResponse(
    long totalUsers,
    long totalProjects,
    long totalExecutions,
    long totalSessions,
    Map<String, Long> languageBreakdown
) {
}
package com.codesync.collab.service;

import com.codesync.collab.model.CollabSession;
import com.codesync.collab.repository.CollabSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupService {
    private final CollabSessionRepository sessionRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupInactiveSessions() {
        Instant threshold = Instant.now().minus(30, ChronoUnit.MINUTES);
        
        List<CollabSession> sessions = sessionRepository.findAll();
        
        for (CollabSession session : sessions) {
            if (session.getIsActive() == null || !session.getIsActive()) {
                continue;
            }
            
            Instant lastActivity = session.getLastActivityAt();
            if (lastActivity == null) {
                lastActivity = session.getCreatedAt();
            }
            
            if (lastActivity.isBefore(threshold)) {
                log.info("Auto-ending inactive session: {}", session.getId());
                session.setIsActive(false);
                sessionRepository.save(session);
            }
        }
    }
}
package com.codesync.collab.repository;

import com.codesync.collab.model.CollabSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollabSessionRepository extends JpaRepository<CollabSession, Long> {
    Optional<CollabSession> findByFileId(Long fileId);
    Optional<CollabSession> findBySessionUuid(String sessionUuid);
}

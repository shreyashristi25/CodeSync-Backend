package com.codesync.collab.repository;

import com.codesync.collab.model.CollabHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollabHistoryRepository extends JpaRepository<CollabHistory, Long> {
    List<CollabHistory> findByFileIdOrderByEndedAtDesc(Long fileId);
    List<CollabHistory> findByProjectIdOrderByEndedAtDesc(Long projectId);
    List<CollabHistory> findByStartedByUserIdOrParticipantsContainingOrderByEndedAtDesc(String userId, String userIdSearch);
}
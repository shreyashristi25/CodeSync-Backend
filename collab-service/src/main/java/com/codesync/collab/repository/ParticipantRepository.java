package com.codesync.collab.repository;

import com.codesync.collab.model.Participant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findBySession_IdAndUserId(Long sessionId, String userId);

    @Modifying
    @Query("DELETE FROM Participant p WHERE p.session.id = :sessionId AND p.userId = :userId")
    void deleteBySession_IdAndUserId(@Param("sessionId") Long sessionId, @Param("userId") String userId);

    List<Participant> findAllBySession_Id(Long sessionId);

    @Modifying
    @Query("DELETE FROM Participant p WHERE p.session.id = :sessionId")
    void deleteAllBySession_Id(@Param("sessionId") Long sessionId);
}

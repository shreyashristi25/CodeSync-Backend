package com.codesync.project.repository;

import com.codesync.project.entity.ProjectInvite;
import com.codesync.project.entity.ProjectInvite.InviteStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, Long> {
    Optional<ProjectInvite> findByInviteToken(String inviteToken);
    List<ProjectInvite> findByProjectIdAndStatus(Long projectId, InviteStatus status);
    List<ProjectInvite> findByUserIdAndStatus(Long userId, InviteStatus status);
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}
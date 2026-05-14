package com.codesync.auth.repository;

import com.codesync.auth.entity.AdminAccessRequest;
import com.codesync.auth.entity.AdminRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAccessRequestRepository extends JpaRepository<AdminAccessRequest, Long> {
    Optional<AdminAccessRequest> findByUserIdAndStatus(Long userId, AdminRequestStatus status);
    List<AdminAccessRequest> findByStatusOrderByCreatedAtDesc(AdminRequestStatus status);
}

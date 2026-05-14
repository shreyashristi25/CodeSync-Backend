package com.codesync.version.repository;

import com.codesync.version.entity.Snapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {
    List<Snapshot> findByFileIdOrderByTimestampDesc(Long fileId);

    List<Snapshot> findByFileIdAndBranchIdOrderByTimestampDesc(Long fileId, Long branchId);

    List<Snapshot> findByBranchIdOrderByTimestampDesc(Long branchId);

    Optional<Snapshot> findByCommitHash(String commitHash);
}

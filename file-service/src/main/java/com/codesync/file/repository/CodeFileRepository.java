package com.codesync.file.repository;

import com.codesync.file.entity.CodeFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeFileRepository extends JpaRepository<CodeFile, Long> {
    List<CodeFile> findByProjectIdAndDeletedFalse(Long projectId);
    
    List<CodeFile> findByProjectIdAndBranchIdAndDeletedFalse(Long projectId, Long branchId);

    Optional<CodeFile> findByIdAndDeletedFalse(Long id);

    boolean existsByProjectIdAndBranchIdAndPathAndDeletedFalse(Long projectId, Long branchId, String path);
}

package com.codesync.project.repository;

import com.codesync.project.entity.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerId(Long ownerId);
    
    List<Project> findByIsPublicTrue();
}

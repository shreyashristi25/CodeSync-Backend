package com.codesync.project.service;

import com.codesync.project.dto.CreateProjectRequest;
import com.codesync.project.dto.InviteCollaboratorRequest;
import com.codesync.project.dto.InviteResponse;
import com.codesync.project.entity.Project;
import com.codesync.project.entity.ProjectInvite;
import com.codesync.project.repository.ProjectRepository;
import com.codesync.project.repository.ProjectInviteRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectInviteRepository projectInviteRepository;

    public Project createProject(CreateProjectRequest request) {
        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .ownerId(request.ownerId())
                .isPublic(request.isPublic() != null ? request.isPublic() : false)
                .language(request.language() != null ? request.language() : "TypeScript")
                .createdAt(Instant.now())
                .build();
        return projectRepository.save(project);
    }

    public List<Project> listProjects(Long ownerId) {
        if (ownerId == null) {
            return projectRepository.findAll();
        }
        return projectRepository.findByOwnerId(ownerId);
    }

    public List<Project> getPublicProjects() {
        return projectRepository.findByIsPublicTrue();
    }

    public Optional<Project> getProject(Long id) {
        return projectRepository.findById(id);
    }

    public Optional<Project> updateProject(Long id, CreateProjectRequest request) {
        return projectRepository.findById(id).map(project -> {
            project.setName(request.name());
            project.setDescription(request.description());
            if (request.isPublic() != null) {
                project.setIsPublic(request.isPublic());
            }
            return projectRepository.save(project);
        });
    }

    @Transactional
    public boolean deleteProject(Long id) {
        if (projectRepository.existsById(id)) {
            projectRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Map<String, Object> getStats() {
        List<Project> allProjects = projectRepository.findAll();
        long total = allProjects.size();
        long publicCount = allProjects.stream().filter(Project::getIsPublic).count();
        long privateCount = total - publicCount;

        Map<String, Long> languageBreakdown = allProjects.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getLanguage() != null ? p.getLanguage() : "Unknown",
                        Collectors.counting()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProjects", total);
        stats.put("publicProjects", publicCount);
        stats.put("privateProjects", privateCount);
        stats.put("languageBreakdown", languageBreakdown);
        return stats;
    }

    @Transactional
    public Optional<Project> starProject(Long id) {
        return projectRepository.findById(id).map(project -> {
            project.setStarCount(project.getStarCount() + 1);
            return projectRepository.save(project);
        });
    }

    @Transactional
    public boolean unstarProject(Long id) {
        return projectRepository.findById(id).map(project -> {
            if (project.getStarCount() > 0) {
                project.setStarCount(project.getStarCount() - 1);
                projectRepository.save(project);
            }
            return true;
        }).orElse(false);
    }

    @Transactional
    public Optional<Project> forkProject(Long id, Long newOwnerId) {
        return projectRepository.findById(id).map(original -> {
            Project forked = Project.builder()
                    .name(original.getName() + " (Fork)")
                    .description(original.getDescription())
                    .ownerId(newOwnerId)
                    .isPublic(false)
                    .language(original.getLanguage())
                    .createdAt(Instant.now())
                    .forkedFromProjectId(original.getId())
                    .forkCount(0)
                    .starCount(0)
                    .contributors(null)
                    .build();
            
            original.setForkCount(original.getForkCount() + 1);
            projectRepository.save(original);
            
            return projectRepository.save(forked);
        });
    }

    @Transactional
    public boolean forceDeleteProject(Long id) {
        if (projectRepository.existsById(id)) {
            projectRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public InviteResponse inviteCollaborator(Long projectId, InviteCollaboratorRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (projectInviteRepository.existsByProjectIdAndUserId(projectId, request.userId())) {
            throw new RuntimeException("User is already a collaborator or invited");
        }
        
        String inviteToken = UUID.randomUUID().toString();
        ProjectInvite invite = ProjectInvite.builder()
                .projectId(projectId)
                .invitedByUserId(project.getOwnerId())
                .userId(request.userId())
                .inviteToken(inviteToken)
                .message(request.message())
                .build();
        
        ProjectInvite saved = projectInviteRepository.save(invite);
        return toInviteResponse(saved);
    }

    @Transactional
    public boolean acceptInvite(String inviteToken, Long userId) {
        ProjectInvite invite = projectInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("Invite not found"));
        
        if (!invite.getUserId().equals(userId)) {
            throw new RuntimeException("Invite not for this user");
        }
        
        if (invite.getStatus() != ProjectInvite.InviteStatus.PENDING) {
            throw new RuntimeException("Invite already processed");
        }
        
        invite.setStatus(ProjectInvite.InviteStatus.ACCEPTED);
        projectInviteRepository.save(invite);
        
        addCollaboratorToProject(invite.getProjectId(), userId);
        return true;
    }

    @Transactional
    public boolean declineInvite(String inviteToken, Long userId) {
        ProjectInvite invite = projectInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("Invite not found"));
        
        if (!invite.getUserId().equals(userId)) {
            throw new RuntimeException("Invite not for this user");
        }
        
        invite.setStatus(ProjectInvite.InviteStatus.DECLINED);
        projectInviteRepository.save(invite);
        return true;
    }

    public List<InviteResponse> getInvitesForProject(Long projectId) {
        return projectInviteRepository.findByProjectIdAndStatus(projectId, ProjectInvite.InviteStatus.PENDING)
                .stream()
                .map(this::toInviteResponse)
                .toList();
    }

    public List<InviteResponse> getInvitesForUser(Long userId) {
        return projectInviteRepository.findByUserIdAndStatus(userId, ProjectInvite.InviteStatus.PENDING)
                .stream()
                .map(this::toInviteResponse)
                .toList();
    }

    private void addCollaboratorToProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return;
        
        String collaborators = project.getCollaborators();
        if (collaborators == null || collaborators.isEmpty()) {
            collaborators = "[]";
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<Long> list = mapper.readValue(collaborators, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Long>>() {});
            if (!list.contains(userId)) {
                list.add(userId);
                project.setCollaborators(mapper.writeValueAsString(list));
                projectRepository.save(project);
            }
        } catch (Exception e) {
            // log error
        }
    }

    private InviteResponse toInviteResponse(ProjectInvite invite) {
        return new InviteResponse(
                invite.getId(),
                invite.getProjectId(),
                invite.getInvitedByUserId(),
                invite.getUserId(),
                invite.getStatus().name(),
                invite.getMessage(),
                invite.getCreatedAt(),
                invite.getExpiresAt(),
                "/projects/invite/" + invite.getInviteToken()
        );
    }
}

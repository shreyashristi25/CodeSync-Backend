package com.codesync.project.controller;

import com.codesync.project.dto.CreateProjectRequest;
import com.codesync.project.dto.InviteCollaboratorRequest;
import com.codesync.project.dto.InviteResponse;
import com.codesync.project.entity.Project;
import com.codesync.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<Project> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @GetMapping
    public ResponseEntity<List<Project>> listProjects(@RequestParam(required = false) Long ownerId) {
        return ResponseEntity.ok(projectService.listProjects(ownerId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<Project>> getPublicProjects() {
        return ResponseEntity.ok(projectService.getPublicProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        return projectService.getProject(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Long id, @RequestBody CreateProjectRequest request) {
        return projectService.updateProject(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        if (projectService.deleteProject(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getProjectStats() {
        return ResponseEntity.ok(projectService.getStats());
    }

    @PostMapping("/{id}/star")
    public ResponseEntity<Project> starProject(@PathVariable Long id) {
        return projectService.starProject(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/star")
    public ResponseEntity<Void> unstarProject(@PathVariable Long id) {
        if (projectService.unstarProject(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/fork")
    public ResponseEntity<Project> forkProject(@PathVariable Long id, @RequestParam Long ownerId) {
        return projectService.forkProject(id, ownerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/{id}/force")
    public ResponseEntity<Void> forceDeleteProject(@PathVariable Long id) {
        if (projectService.forceDeleteProject(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<InviteResponse> inviteCollaborator(
            @PathVariable Long id,
            @Valid @RequestBody InviteCollaboratorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.inviteCollaborator(id, request));
    }

    @PostMapping("/invite/{token}/accept")
    public ResponseEntity<Void> acceptInvite(
            @PathVariable String token,
            @RequestParam Long userId) {
        projectService.acceptInvite(token, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invite/{token}/decline")
    public ResponseEntity<Void> declineInvite(
            @PathVariable String token,
            @RequestParam Long userId) {
        projectService.declineInvite(token, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/invites")
    public ResponseEntity<List<InviteResponse>> getProjectInvites(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getInvitesForProject(id));
    }

    @GetMapping("/invites")
    public ResponseEntity<List<InviteResponse>> getUserInvites(@RequestParam Long userId) {
        return ResponseEntity.ok(projectService.getInvitesForUser(userId));
    }
}

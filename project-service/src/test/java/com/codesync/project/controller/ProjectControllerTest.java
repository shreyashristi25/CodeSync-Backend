package com.codesync.project.controller;

import com.codesync.project.dto.CreateProjectRequest;
import com.codesync.project.dto.InviteCollaboratorRequest;
import com.codesync.project.dto.InviteResponse;
import com.codesync.project.entity.Project;
import com.codesync.project.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectController projectController;

    @Test
    void shouldCreateProject() {
        CreateProjectRequest request = new CreateProjectRequest("Test Project", "A test project", 1L, false, "java");

        Project project = Project.builder()
                .id(1L)
                .name("Test Project")
                .description("A test project")
                .isPublic(false)
                .createdAt(java.time.Instant.now())
                .build();

        when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(project);

        ResponseEntity<Project> result = projectController.createProject(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Test Project", result.getBody().getName());
    }

    @Test
    void shouldListProjects() {
        List<Project> projects = List.of(
                Project.builder().id(1L).name("Project 1").build(),
                Project.builder().id(2L).name("Project 2").build()
        );

        when(projectService.listProjects(null)).thenReturn(projects);

        ResponseEntity<List<Project>> result = projectController.listProjects(null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
    }

    @Test
    void shouldListProjectsByOwner() {
        List<Project> projects = List.of(
                Project.builder().id(1L).name("My Project").build()
        );

        when(projectService.listProjects(1L)).thenReturn(projects);

        ResponseEntity<List<Project>> result = projectController.listProjects(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void shouldGetPublicProjects() {
        List<Project> projects = List.of(
                Project.builder().id(1L).name("Public Project").isPublic(true).build()
        );

        when(projectService.getPublicProjects()).thenReturn(projects);

        ResponseEntity<List<Project>> result = projectController.getPublicProjects();

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void shouldGetProject() {
        Project project = Project.builder()
                .id(1L)
                .name("Test Project")
                .build();

        when(projectService.getProject(1L)).thenReturn(Optional.of(project));

        ResponseEntity<Project> result = projectController.getProject(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Test Project", result.getBody().getName());
    }

    @Test
    void shouldReturn404WhenProjectNotFound() {
        when(projectService.getProject(999L)).thenReturn(Optional.empty());

        ResponseEntity<Project> result = projectController.getProject(999L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void shouldUpdateProject() {
        CreateProjectRequest request = new CreateProjectRequest("Updated Name", "Updated description", 1L, true, "java");

        Project updatedProject = Project.builder()
                .id(1L)
                .name("Updated Name")
                .description("Updated description")
                .isPublic(true)
                .build();

        when(projectService.updateProject(anyLong(), any(CreateProjectRequest.class)))
                .thenReturn(Optional.of(updatedProject));

        ResponseEntity<Project> result = projectController.updateProject(1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Updated Name", result.getBody().getName());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentProject() {
        CreateProjectRequest request = new CreateProjectRequest("Updated Name", "Updated desc", 1L, false, null);

        when(projectService.updateProject(anyLong(), any(CreateProjectRequest.class)))
                .thenReturn(Optional.empty());

        ResponseEntity<Project> result = projectController.updateProject(999L, request);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void shouldDeleteProject() {
        when(projectService.deleteProject(1L)).thenReturn(true);

        ResponseEntity<Void> result = projectController.deleteProject(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentProject() {
        when(projectService.deleteProject(999L)).thenReturn(false);

        ResponseEntity<Void> result = projectController.deleteProject(999L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void shouldInviteCollaborator() {
        InviteCollaboratorRequest request = new InviteCollaboratorRequest(2L, "Join my project");

        InviteResponse response = new InviteResponse(1L, 1L, 1L, 1L, "PENDING", "Join my project", 
                LocalDateTime.now(), LocalDateTime.now().plusDays(7), "http://invite/link");

        when(projectService.inviteCollaborator(anyLong(), any(InviteCollaboratorRequest.class)))
                .thenReturn(response);

        ResponseEntity<InviteResponse> result = projectController.inviteCollaborator(1L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().id());
    }

    @Test
    void shouldAcceptInvite() {
        when(projectService.acceptInvite("token123", 1L)).thenReturn(true);

        ResponseEntity<Void> result = projectController.acceptInvite("token123", 1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void shouldGetProjectInvites() {
        List<InviteResponse> invites = List.of(
                new InviteResponse(1L, 1L, 1L, 1L, "PENDING", "Join my project", 
                        LocalDateTime.now(), LocalDateTime.now().plusDays(7), "http://invite/link")
        );

        when(projectService.getInvitesForProject(1L)).thenReturn(invites);

        ResponseEntity<List<InviteResponse>> result = projectController.getProjectInvites(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void shouldGetUserInvites() {
        List<InviteResponse> invites = List.of(
                new InviteResponse(1L, 1L, 1L, 1L, "PENDING", "Join my project", 
                        LocalDateTime.now(), LocalDateTime.now().plusDays(7), "http://invite/link")
        );

        when(projectService.getInvitesForUser(1L)).thenReturn(invites);

        ResponseEntity<List<InviteResponse>> result = projectController.getUserInvites(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }
}
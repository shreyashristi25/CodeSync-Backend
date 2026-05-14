package com.codesync.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codesync.project.dto.CreateProjectRequest;
import com.codesync.project.dto.InviteCollaboratorRequest;
import com.codesync.project.dto.InviteResponse;
import com.codesync.project.entity.Project;
import com.codesync.project.entity.ProjectInvite;
import com.codesync.project.repository.ProjectRepository;
import com.codesync.project.repository.ProjectInviteRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectInviteRepository projectInviteRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldCreateProject() {
        CreateProjectRequest request = new CreateProjectRequest("alpha", "description", 1L, false, "TypeScript");
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(10L);
            return project;
        });
        Project created = projectService.createProject(request);
        assertEquals(10L, created.getId());
        assertEquals("alpha", created.getName());
    }

    @Test
    void shouldListProjectsByOwner() {
        when(projectRepository.findByOwnerId(1L))
                .thenReturn(List.of(Project.builder().id(1L).name("P").description("D").ownerId(1L).createdAt(Instant.now()).build()));
        List<Project> projects = projectService.listProjects(1L);
        assertEquals(1, projects.size());
        verify(projectRepository).findByOwnerId(1L);
    }

    @Test
    void shouldListAllProjectsWhenOwnerIdIsNull() {
        when(projectRepository.findAll()).thenReturn(List.of(Project.builder().id(1L).build()));
        
        List<Project> projects = projectService.listProjects(null);
        
        assertEquals(1, projects.size());
        verify(projectRepository).findAll();
    }

    @Test
    void shouldGetPublicProjects() {
        when(projectRepository.findByIsPublicTrue()).thenReturn(List.of(Project.builder().id(1L).isPublic(true).build()));
        
        List<Project> projects = projectService.getPublicProjects();
        
        assertEquals(1, projects.size());
    }

    @Test
    void shouldGetProjectById() {
        Project project = Project.builder().id(1L).name("Test").build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        
        Optional<Project> result = projectService.getProject(1L);
        
        assertTrue(result.isPresent());
        assertEquals("Test", result.get().getName());
    }

    @Test
    void shouldUpdateProject() {
        Project existing = Project.builder().id(1L).name("Old").description("Old desc").build();
        CreateProjectRequest request = new CreateProjectRequest("New", "New desc", 1L, true, "Java");
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Optional<Project> result = projectService.updateProject(1L, request);
        
        assertTrue(result.isPresent());
        assertEquals("New", result.get().getName());
    }

    @Test
    void shouldDeleteProject() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        
        boolean result = projectService.deleteProject(1L);
        
        assertTrue(result);
        verify(projectRepository).deleteById(1L);
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentProject() {
        when(projectRepository.existsById(999L)).thenReturn(false);
        
        boolean result = projectService.deleteProject(999L);
        
        assertFalse(result);
    }

    @Test
    void shouldGetStats() {
        List<Project> projects = List.of(
            Project.builder().id(1L).isPublic(true).language("Java").build(),
            Project.builder().id(2L).isPublic(false).language("TypeScript").build()
        );
        when(projectRepository.findAll()).thenReturn(projects);
        
        var stats = projectService.getStats();
        
        assertEquals(2L, stats.get("totalProjects"));
        assertEquals(1L, stats.get("publicProjects"));
        assertEquals(1L, stats.get("privateProjects"));
    }

    @Test
    void shouldStarProject() {
        Project project = Project.builder().id(1L).starCount(5).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Optional<Project> result = projectService.starProject(1L);
        
        assertTrue(result.isPresent());
        assertEquals(6, result.get().getStarCount());
    }

    @Test
    void shouldUnstarProject() {
        Project project = Project.builder().id(1L).starCount(5).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = projectService.unstarProject(1L);
        
        assertTrue(result);
        assertEquals(4, project.getStarCount());
    }

    @Test
    void shouldForkProject() {
        Project original = Project.builder()
                .id(1L).name("Original").description("Test").ownerId(1L)
                .isPublic(true).language("Java").forkCount(0).starCount(0).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(original));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });
        
        Optional<Project> result = projectService.forkProject(1L, 2L);
        
        assertTrue(result.isPresent());
        assertTrue(result.get().getName().contains("Fork"));
        assertEquals(2L, result.get().getOwnerId());
        assertEquals(1, original.getForkCount());
    }

    @Test
    void shouldForceDeleteProject() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        
        boolean result = projectService.forceDeleteProject(1L);
        
        assertTrue(result);
        verify(projectRepository).deleteById(1L);
    }

    @Test
    void shouldInviteCollaborator() {
        Project project = Project.builder().id(1L).ownerId(1L).build();
        InviteCollaboratorRequest request = new InviteCollaboratorRequest(2L, "Join us");
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectInviteRepository.existsByProjectIdAndUserId(1L, 2L)).thenReturn(false);
        when(projectInviteRepository.save(any(ProjectInvite.class))).thenAnswer(invocation -> {
            ProjectInvite invite = invocation.getArgument(0);
            invite.setId(1L);
            return invite;
        });
        
        InviteResponse result = projectService.inviteCollaborator(1L, request);
        
        assertNotNull(result);
        assertEquals(1L, result.projectId());
    }

    @Test
    void shouldThrowWhenInvitingExistingCollaborator() {
        Project project = Project.builder().id(1L).ownerId(1L).build();
        InviteCollaboratorRequest request = new InviteCollaboratorRequest(2L, "Join us");
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectInviteRepository.existsByProjectIdAndUserId(1L, 2L)).thenReturn(true);
        
        assertThrows(RuntimeException.class, () -> projectService.inviteCollaborator(1L, request));
    }

    @Test
    void shouldAcceptInvite() {
        ProjectInvite invite = ProjectInvite.builder()
                .id(1L).projectId(1L).userId(2L).status(ProjectInvite.InviteStatus.PENDING)
                .inviteToken("token123").build();
        Project project = Project.builder().id(1L).collaborators("[]").build();
        
        when(projectInviteRepository.findByInviteToken("token123")).thenReturn(Optional.of(invite));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectInviteRepository.save(any(ProjectInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = projectService.acceptInvite("token123", 2L);
        
        assertTrue(result);
    }

    @Test
    void shouldDeclineInvite() {
        ProjectInvite invite = ProjectInvite.builder()
                .id(1L).userId(2L).status(ProjectInvite.InviteStatus.PENDING)
                .inviteToken("token123").build();
        
        when(projectInviteRepository.findByInviteToken("token123")).thenReturn(Optional.of(invite));
        when(projectInviteRepository.save(any(ProjectInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = projectService.declineInvite("token123", 2L);
        
        assertTrue(result);
        assertEquals(ProjectInvite.InviteStatus.DECLINED, invite.getStatus());
    }

    @Test
    void shouldGetInvitesForProject() {
        ProjectInvite invite = ProjectInvite.builder().id(1L).projectId(1L).build();
        when(projectInviteRepository.findByProjectIdAndStatus(1L, ProjectInvite.InviteStatus.PENDING))
                .thenReturn(List.of(invite));
        
        List<InviteResponse> result = projectService.getInvitesForProject(1L);
        
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetInvitesForUser() {
        ProjectInvite invite = ProjectInvite.builder().id(1L).userId(2L).build();
        when(projectInviteRepository.findByUserIdAndStatus(2L, ProjectInvite.InviteStatus.PENDING))
                .thenReturn(List.of(invite));
        
        List<InviteResponse> result = projectService.getInvitesForUser(2L);
        
        assertEquals(1, result.size());
    }
}

package com.codesync.project.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ProjectTest {

    @Test
    void shouldCreateProjectWithBuilder() {
        Project project = Project.builder()
                .id(1L)
                .name("Test Project")
                .description("A test project")
                .isPublic(false)
                .ownerId(100L)
                .build();

        assertNotNull(project);
        assertEquals("Test Project", project.getName());
        assertEquals("A test project", project.getDescription());
        assertEquals(false, project.getIsPublic());
        assertEquals(100L, project.getOwnerId());
    }

    @Test
    void shouldSetProjectFields() {
        Project project = new Project();
        project.setId(2L);
        project.setName("Another Project");
        project.setIsPublic(true);
        project.setOwnerId(200L);

        assertEquals(2L, project.getId());
        assertEquals("Another Project", project.getName());
        assertEquals(true, project.getIsPublic());
        assertEquals(200L, project.getOwnerId());
    }
}
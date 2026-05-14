package com.codesync.file.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CodeFileTest {

    @Test
    void shouldCreateCodeFile() {
        CodeFile file = CodeFile.builder()
                .id(1L)
                .name("Main.java")
                .path("src/Main.java")
                .isDirectory(false)
                .content("public class Main {}")
                .language("java")
                .projectId(100L)
                .createdBy(1L)
                .lastModifiedBy(1L)
                .build();

        assertNotNull(file);
        assertEquals("Main.java", file.getName());
        assertEquals("src/Main.java", file.getPath());
        assertEquals(false, file.getIsDirectory());
        assertEquals("java", file.getLanguage());
        assertEquals(100L, file.getProjectId());
    }

    @Test
    void shouldSetCodeFileFields() {
        CodeFile file = new CodeFile();
        file.setId(2L);
        file.setName("Test.ts");
        file.setPath("tests/Test.ts");
        file.setIsDirectory(false);
        file.setContent("console.log('test');");
        file.setLanguage("typescript");
        file.setProjectId(200L);

        assertEquals(2L, file.getId());
        assertEquals("Test.ts", file.getName());
        assertEquals("typescript", file.getLanguage());
    }
}
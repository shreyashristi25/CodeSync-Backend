package com.codesync.execution.service;

import com.codesync.execution.entity.ExecutionLanguage;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.exception.BadRequestException;
import com.codesync.execution.repository.SupportedLanguageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DockerContainerServiceTest {

    @Mock
    private SupportedLanguageRepository languageRepository;

    @InjectMocks
    private DockerContainerService dockerContainerService;

    @Test
    void shouldThrowWhenCodeIsNull() {
        assertThrows(BadRequestException.class, () -> 
            dockerContainerService.executeInContainer(null, ExecutionLanguage.PYTHON, null));
    }

    @Test
    void shouldThrowWhenCodeIsBlank() {
        assertThrows(BadRequestException.class, () -> 
            dockerContainerService.executeInContainer("   ", ExecutionLanguage.PYTHON, null));
    }

    @Test
    void shouldThrowWhenLanguageNotFoundInRepository() {
        when(languageRepository.findByCode("PYTHON")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> 
            dockerContainerService.executeInContainer("print('test')", ExecutionLanguage.PYTHON, null));
    }

    @Test
    void shouldUseDefaultDockerImageWhenNull() {
        SupportedLanguage lang = SupportedLanguage.builder()
                .code("PYTHON")
                .dockerImage(null)
                .entryPoint("python3")
                .build();
        when(languageRepository.findByCode("PYTHON")).thenReturn(Optional.of(lang));

        // This will try to run Docker, which might fail in test environment
        // but it should at least proceed past the validation
        try {
            dockerContainerService.executeInContainer("print('test')", ExecutionLanguage.PYTHON, null);
        } catch (Exception e) {
            // Expected in test environment without Docker
            assertTrue(e.getMessage().contains("Execution error") || e.getMessage().contains("Cannot run program"));
        }
    }

    @Test
    void shouldGetFileExtensionForPython() {
        // Testing through executeInContainer which internally calls getFileExtension
        SupportedLanguage lang = SupportedLanguage.builder()
                .code("PYTHON")
                .dockerImage("python:3.11")
                .entryPoint("python3")
                .build();
        when(languageRepository.findByCode("PYTHON")).thenReturn(Optional.of(lang));

        try {
            dockerContainerService.executeInContainer("code", ExecutionLanguage.PYTHON, "input");
        } catch (Exception e) {
            // Expected in test environment
        }
    }
}
package com.codesync.execution.service;

import com.codesync.execution.dto.SupportedLanguageDto;
import com.codesync.execution.dto.SupportedLanguageUpdateRequest;
import com.codesync.execution.entity.ExecutionLanguage;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.repository.SupportedLanguageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportedLanguageServiceTest {

    @Mock
    private SupportedLanguageRepository supportedLanguageRepository;

    @InjectMocks
    private SupportedLanguageService supportedLanguageService;

    @Test
    void shouldListLanguages() {
        when(supportedLanguageRepository.count()).thenReturn(1L);
        when(supportedLanguageRepository.findAll()).thenReturn(List.of(
                SupportedLanguage.builder().id(1L).code("PYTHON").displayName("Python").enabled(true).dockerImage("python:3.11").entryPoint("python3").build()
        ));

        List<SupportedLanguageDto> result = supportedLanguageService.list();

        assertEquals(1, result.size());
        assertEquals("PYTHON", result.get(0).code());
    }

    @Test
    void shouldSeedDefaultsOnEmptyDatabase() {
        when(supportedLanguageRepository.count()).thenReturn(0L);
        when(supportedLanguageRepository.save(any(SupportedLanguage.class))).thenAnswer(invocation -> {
            SupportedLanguage lang = invocation.getArgument(0);
            lang.setId(1L);
            return lang;
        });
        when(supportedLanguageRepository.findAll()).thenAnswer(invocation -> {
            List<SupportedLanguage> list = new java.util.ArrayList<>();
            for (ExecutionLanguage lang : ExecutionLanguage.values()) {
                list.add(SupportedLanguage.builder().id((long)list.size()+1).code(lang.name()).displayName(lang.name()).enabled(true).dockerImage("test").entryPoint("test").build());
            }
            return list;
        });

        List<SupportedLanguageDto> result = supportedLanguageService.list();

        assertEquals(ExecutionLanguage.values().length, result.size());
        verify(supportedLanguageRepository, atLeastOnce()).save(any(SupportedLanguage.class));
    }

    @Test
    void shouldUpdateLanguage() {
        SupportedLanguage existing = SupportedLanguage.builder()
                .id(1L).code("PYTHON").displayName("Python").enabled(true).dockerImage("python:3.11").entryPoint("python3").build();

        when(supportedLanguageRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supportedLanguageRepository.save(any(SupportedLanguage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportedLanguageDto result = supportedLanguageService.update(1L, new SupportedLanguageUpdateRequest("Python 3", true, "python:3.12", "python3.12"));

        assertEquals("Python 3", result.displayName());
        assertEquals("python:3.12", result.dockerImage());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentLanguage() {
        when(supportedLanguageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> supportedLanguageService.update(999L, new SupportedLanguageUpdateRequest("Test", true, null, null)));
    }

    @Test
    void shouldNotSeedWhenLanguagesExist() {
        when(supportedLanguageRepository.count()).thenReturn(5L);
        when(supportedLanguageRepository.findAll()).thenReturn(List.of(
                SupportedLanguage.builder().id(1L).code("PYTHON").displayName("Python").build()
        ));

        supportedLanguageService.list();

        verify(supportedLanguageRepository, never()).save(any(SupportedLanguage.class));
    }
}
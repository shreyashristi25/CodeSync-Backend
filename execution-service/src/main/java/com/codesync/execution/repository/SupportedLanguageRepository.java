package com.codesync.execution.repository;

import com.codesync.execution.entity.SupportedLanguage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportedLanguageRepository extends JpaRepository<SupportedLanguage, Long> {
    Optional<SupportedLanguage> findByCode(String code);
}

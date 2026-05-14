package com.codesync.file.service;

import com.codesync.file.dto.CodeFileTreeNode;
import com.codesync.file.dto.CreateCodeFileRequest;
import com.codesync.file.dto.UpdateCodeFileRequest;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.BadRequestException;
import com.codesync.file.exception.NotFoundException;
import com.codesync.file.repository.CodeFileRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    private final CodeFileRepository codeFileRepository;

    @Transactional
    public CodeFile create(CreateCodeFileRequest request) {
        // DEBUG: Remove existence check to isolate the issue
        // if (codeFileRepository.existsByProjectIdAndPathAndDeletedFalse(request.getProjectId(), request.getPath())) {
        //     throw new BadRequestException("Path already exists for project");
        // }
        String content = request.getContent() == null ? "" : request.getContent();
        CodeFile file = CodeFile.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .path(request.getPath())
                .isDirectory(request.getIsDirectory())
                .parentId(request.getParentId())
                .content(content)
                .language(request.getLanguage())
                .createdBy(request.getCreatedBy())
                .lastModifiedBy(request.getCreatedBy())
                .branchId(request.getBranchId())
                .deleted(false)
                .build();
        return codeFileRepository.save(file);
    }

    public CodeFile get(Long id) {
        return codeFileRepository
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("File not found"));
    }

    @Transactional
    public CodeFile update(Long id, UpdateCodeFileRequest request) {
        CodeFile file = get(id);
        log.info("UPDATE FILE: id={}, name={}, path={}, content={}", id, request.getName(), request.getPath(), request.getContent());
        log.info("  current file: name={}, path={}", file.getName(), file.getPath());
        
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            if (!newName.equals(file.getName())) {
                String newPath = file.getPath().replace(file.getName(), newName);
                log.info("  renaming to: name={}, path={}", newName, newPath);
                if (codeFileRepository.existsByProjectIdAndBranchIdAndPathAndDeletedFalse(file.getProjectId(), file.getBranchId(), newPath)) {
                    throw new BadRequestException("Path already exists for project in current branch");
                }
                file.setName(newName);
                file.setPath(newPath);
                codeFileRepository.flush();
            }
        }
        
        if (request.getPath() != null && !request.getPath().equals(file.getPath())) {
            if (codeFileRepository.existsByProjectIdAndBranchIdAndPathAndDeletedFalse(file.getProjectId(), file.getBranchId(), request.getPath())) {
                throw new BadRequestException("Path already exists for project in current branch");
            }
            file.setPath(request.getPath());
            codeFileRepository.flush();
        }
        
        if (request.getContent() != null) {
            file.setContent(request.getContent());
            codeFileRepository.flush();
        }
        if (request.getLanguage() != null) {
            file.setLanguage(request.getLanguage());
            codeFileRepository.flush();
        }
        if (request.getLastModifiedBy() != null) {
            file.setLastModifiedBy(request.getLastModifiedBy());
            codeFileRepository.flush();
        }
        
        return codeFileRepository.save(file);
    }

    @Transactional
    public void softDelete(Long id) {
        CodeFile file = codeFileRepository.findById(id).orElseThrow(() -> new NotFoundException("File not found"));
        file.setDeleted(true);
        codeFileRepository.save(file);
    }

    public List<CodeFileTreeNode> tree(Long projectId, Long branchId) {
        List<CodeFile> files = (branchId != null) 
            ? codeFileRepository.findByProjectIdAndBranchIdAndDeletedFalse(projectId, branchId)
            : codeFileRepository.findByProjectIdAndDeletedFalse(projectId);
        Map<Long, CodeFileTreeNode> nodes = new HashMap<>();
        for (CodeFile f : files) {
            nodes.put(
                    f.getId(),
                    CodeFileTreeNode.builder()
                            .id(f.getId())
                            .name(f.getName())
                            .path(f.getPath())
                            .parentId(f.getParentId())
                            .projectId(f.getProjectId())
                            .content(f.getContent())
                            .isDirectory(f.getIsDirectory())
                            .language(f.getLanguage())
                            .createdBy(f.getCreatedBy())
                            .lastModifiedBy(f.getLastModifiedBy())
                            .createdAt(f.getCreatedAt())
                            .updatedAt(f.getUpdatedAt())
                            .children(new ArrayList<>())
                            .build());
        }
        List<CodeFileTreeNode> roots = new ArrayList<>();
        for (CodeFileTreeNode node : nodes.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || !nodes.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodes.get(parentId).getChildren().add(node);
            }
        }
        return roots;
    }

    public List<CodeFile> listByProject(Long projectId, Long branchId) {
        if (branchId != null) {
            return codeFileRepository.findByProjectIdAndBranchIdAndDeletedFalse(projectId, branchId);
        }
        return codeFileRepository.findByProjectIdAndDeletedFalse(projectId);
    }
}

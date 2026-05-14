package com.codesync.version.service;

import com.codesync.version.dto.BranchResponse;
import com.codesync.version.dto.CreateBranchRequest;
import com.codesync.version.dto.CreatePullRequestRequest;
import com.codesync.version.dto.CreateRepositoryRequest;
import com.codesync.version.dto.CreateSnapshotRequest;
import com.codesync.version.dto.CreateTagRequest;
import com.codesync.version.dto.DiffResponse;
import com.codesync.version.dto.MergePullRequestRequest;
import com.codesync.version.dto.PullRequestResponse;
import com.codesync.version.dto.RepositoryResponse;
import com.codesync.version.dto.RestoreSnapshotRequest;
import com.codesync.version.dto.SnapshotDetailResponse;
import com.codesync.version.dto.SnapshotSummaryResponse;
import com.codesync.version.dto.TagResponse;
import com.codesync.version.entity.Branch;
import com.codesync.version.entity.PullRequest;
import com.codesync.version.entity.Repository;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.entity.Tag;
import com.codesync.version.exception.BadRequestException;
import com.codesync.version.exception.NotFoundException;
import com.codesync.version.repository.BranchRepository;
import com.codesync.version.repository.PullRequestRepository;
import com.codesync.version.repository.RepositoryRepository;
import com.codesync.version.repository.SnapshotRepository;
import com.codesync.version.repository.TagRepository;
import com.codesync.version.util.CommitHashUtil;
import com.codesync.version.util.MyersDiff;
import java.time.Instant;
import java.util.List;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class VersionService {
    private final SnapshotRepository snapshotRepository;
    private final TagRepository tagRepository;
    private final BranchRepository branchRepository;
    private final RepositoryRepository repositoryRepository;
    private final PullRequestRepository pullRequestRepository;
    private final RestTemplate restTemplate;
    private final EntityManager entityManager;

    @Value("${file.service.url:http://localhost:8083}")
    private String fileServiceUrl;

    @Transactional
    public SnapshotDetailResponse createSnapshot(CreateSnapshotRequest request) {
        if (request.parentHash() != null
                && snapshotRepository.findByCommitHash(request.parentHash()).isEmpty()) {
            throw new BadRequestException("Parent snapshot not found");
        }

        Long branchId = request.branchId();
        if (branchId == null) {
            branchId = getOrCreateDefaultBranchForFile(request.fileId());
        }

        Instant ts = Instant.now();
        String hash = CommitHashUtil.compute(
                request.fileId(), request.parentHash(), request.fullContent(), ts);

        Snapshot snap = Snapshot.builder()
                .commitHash(hash)
                .fileId(request.fileId())
                .parentHash(request.parentHash())
                .fullContent(request.fullContent())
                .timestamp(ts)
                .commitMessage(request.commitMessage())
                .authorId(request.authorId())
                .branchId(branchId)
                .build();
        snapshotRepository.save(snap);

        updateBranchLatestSnapshot(branchId, hash);

        return SnapshotDetailResponse.fromEntity(snap);
    }

    private Long getOrCreateDefaultBranchForFile(Long fileId) {
        // Fetch file to get its projectId
        CodeFileResponse file = restTemplate.getForObject(fileServiceUrl + "/api/files/" + fileId, CodeFileResponse.class);
        Long projectId = (file != null) ? file.projectId() : 1L;

        Branch defaultBranch = branchRepository.findByProjectIdAndName(projectId, "main")
                .orElseGet(() -> {
                    try {
                        Branch b = Branch.builder()
                                .name("main")
                                .projectId(projectId)
                                .isDefault(true)
                                .createdAt(Instant.now())
                                .build();
                        return branchRepository.save(b);
                    } catch (DataIntegrityViolationException ex) {
                        return branchRepository.findByProjectIdAndName(projectId, "main")
                                .orElseThrow(() -> new BadRequestException("Main branch already exists for project " + projectId));
                    }
                });
        return defaultBranch.getId();
    }

    private record CodeFileResponse(Long id, Long projectId) {}

    private void updateBranchLatestSnapshot(Long branchId, String hash) {
        branchRepository.findById(branchId).ifPresent(branch -> {
            branch.setLatestSnapshotHash(hash);
            branchRepository.save(branch);
        });
    }

    @Transactional
    public void checkoutBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
        
        List<Snapshot> snapshots = getLatestSnapshotsForBranch(branchId);
        System.out.println("Checking out branch " + branch.getName() + " with " + snapshots.size() + " files.");
        
        for (Snapshot snap : snapshots) {
            try {
                restTemplate.put(
                    fileServiceUrl + "/api/files/" + snap.getFileId(),
                    java.util.Map.of("content", snap.getFullContent()));
            } catch (Exception e) {
                System.err.println("Failed to restore file " + snap.getFileId() + ": " + e.getMessage());
            }
        }
    }

    private List<Snapshot> getLatestSnapshotsForBranch(Long branchId) {
        List<Snapshot> all = snapshotRepository.findByBranchIdOrderByTimestampDesc(branchId);
        java.util.Map<Long, Snapshot> latestPerFile = new java.util.HashMap<>();
        for (Snapshot s : all) {
            latestPerFile.putIfAbsent(s.getFileId(), s);
        }
        return new java.util.ArrayList<>(latestPerFile.values());
    }

    public List<SnapshotSummaryResponse> listHistory(Long fileId) {
        return snapshotRepository.findByFileIdOrderByTimestampDesc(fileId).stream()
                .map(SnapshotSummaryResponse::fromEntity)
                .toList();
    }

    public List<SnapshotSummaryResponse> listHistoryByBranch(Long fileId, Long branchId) {
        return snapshotRepository.findByFileIdAndBranchIdOrderByTimestampDesc(fileId, branchId).stream()
                .map(SnapshotSummaryResponse::fromEntity)
                .toList();
    }

    public SnapshotDetailResponse getSnapshot(String commitHash) {
        Snapshot snap = snapshotRepository
                .findByCommitHash(commitHash)
                .orElseThrow(() -> new NotFoundException("Snapshot not found"));
        return SnapshotDetailResponse.fromEntity(snap);
    }

    public DiffResponse diff(String fromHash, String toHash) {
        Snapshot from = snapshotRepository
                .findByCommitHash(fromHash)
                .orElseThrow(() -> new NotFoundException("From snapshot not found"));
        Snapshot to = snapshotRepository
                .findByCommitHash(toHash)
                .orElseThrow(() -> new NotFoundException("To snapshot not found"));
        if (!from.getFileId().equals(to.getFileId())) {
            throw new BadRequestException("Snapshots belong to different files");
        }
        String unified = MyersDiff.unifiedDiff(
                from.getFullContent(), to.getFullContent(), "a/" + fromHash, "b/" + toHash);
        return DiffResponse.builder()
                .fromHash(fromHash)
                .toHash(toHash)
                .unifiedDiff(unified)
                .build();
    }

    @Transactional
    public SnapshotDetailResponse restore(RestoreSnapshotRequest request) {
        Snapshot target = snapshotRepository
                .findByCommitHash(request.snapshotHash())
                .orElseThrow(() -> new NotFoundException("Snapshot not found"));

        String restoredContent = target.getFullContent();
        Long fileId = request.fileId();

        restTemplate.put(
                fileServiceUrl + "/api/files/" + fileId,
                new FileUpdateRequest(restoredContent));

        Long branchId = target.getBranchId();
        if (branchId == null) {
            branchId = getOrCreateDefaultBranchForFile(fileId);
        }

        Snapshot latest = snapshotRepository
                .findByBranchIdOrderByTimestampDesc(branchId)
                .stream()
                .findFirst()
                .orElse(null);

        String parentHash = latest != null ? latest.getCommitHash() : null;

        Instant ts = Instant.now();
        String newHash = CommitHashUtil.compute(fileId, parentHash, restoredContent, ts);
        String commitMessage = request.commitMessage() != null
                ? request.commitMessage()
                : "Restored from " + request.snapshotHash().substring(0, 7);

        Snapshot newSnap = Snapshot.builder()
                .commitHash(newHash)
                .fileId(fileId)
                .parentHash(parentHash)
                .fullContent(restoredContent)
                .timestamp(ts)
                .commitMessage(commitMessage)
                .authorId(request.authorId())
                .branchId(branchId)
                .build();
        snapshotRepository.save(newSnap);

        updateBranchLatestSnapshot(branchId, newHash);

        return SnapshotDetailResponse.fromEntity(newSnap);
    }

    @Transactional
    public BranchResponse createBranch(CreateBranchRequest request) {
        if (branchRepository.findByProjectIdAndName(request.projectId(), request.name()).isPresent()) {
            throw new BadRequestException("Branch already exists: " + request.name());
        }

        boolean isDefault = branchRepository.findByProjectId(request.projectId()).isEmpty();

        Branch branch = Branch.builder()
                .name(request.name())
                .projectId(request.projectId())
                .isDefault(isDefault)
                .createdAt(Instant.now())
                .build();
        
        if (request.sourceBranchId() != null) {
            Branch source = branchRepository.findById(request.sourceBranchId())
                    .orElseThrow(() -> new NotFoundException("Source branch not found"));
            branch.setLatestSnapshotHash(source.getLatestSnapshotHash());
        }
        
        branch = branchRepository.save(branch);
        
        // If created from source, inherit the latest snapshots
        if (request.sourceBranchId() != null) {
            List<Snapshot> sourceSnaps = getLatestSnapshotsForBranch(request.sourceBranchId());
            if (sourceSnaps.isEmpty()) {
                // If source branch is empty (no commits), try to capture current live state
                System.out.println("Source branch " + request.sourceBranchId() + " is empty. Capturing live state for new branch " + request.name());
                captureLiveStateToBranch(request.projectId(), request.sourceBranchId(), branch.getId());
            } else {
                for (Snapshot s : sourceSnaps) {
                    Snapshot inherited = Snapshot.builder()
                            .commitHash(CommitHashUtil.compute(s.getFileId(), s.getCommitHash(), s.getFullContent(), Instant.now()))
                            .fileId(s.getFileId())
                            .parentHash(s.getCommitHash())
                            .fullContent(s.getFullContent())
                            .timestamp(Instant.now())
                            .commitMessage("Created branch " + request.name() + " from " + s.getCommitHash().substring(0, 7))
                            .authorId(s.getAuthorId())
                            .branchId(branch.getId())
                            .build();
                    snapshotRepository.save(inherited);
                }
            }
        }
        
        return BranchResponse.fromEntity(branch);
    }

    private void captureLiveStateToBranch(Long projectId, Long sourceBranchId, Long targetBranchId) {
        try {
            String url = fileServiceUrl + "/api/files?projectId=" + projectId;
            if (sourceBranchId != null) {
                url += "&branchId=" + sourceBranchId;
            }
            CodeFile[] files = restTemplate.getForObject(url, CodeFile[].class);
            if (files != null) {
                for (CodeFile f : files) {
                    if (Boolean.TRUE.equals(f.getIsDirectory())) continue;
                    
                    // Create NEW file in file-service for the new branch
                    try {
                        restTemplate.postForObject(fileServiceUrl + "/api/files", 
                            java.util.Map.of(
                                "projectId", projectId,
                                "name", f.getName(),
                                "path", f.getPath(),
                                "isDirectory", false,
                                "content", f.getContent() != null ? f.getContent() : "",
                                "language", f.getLanguage() != null ? f.getLanguage() : "",
                                "createdBy", f.getLastModifiedBy() != null ? f.getLastModifiedBy() : 0,
                                "branchId", targetBranchId
                            ), java.util.Map.class);
                    } catch (Exception e) {
                        System.err.println("Failed to clone file to new branch: " + e.getMessage());
                    }

                    // Also create a snapshot for the new branch
                    String hash = CommitHashUtil.compute(f.getId(), null, f.getContent(), Instant.now());
                    Snapshot snap = Snapshot.builder()
                            .commitHash(hash)
                            .fileId(f.getId())
                            .fullContent(f.getContent())
                            .timestamp(Instant.now())
                            .commitMessage("Initial snapshot for branch")
                            .authorId(f.getLastModifiedBy())
                            .branchId(targetBranchId)
                            .build();
                    snapshotRepository.save(snap);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to capture live state: " + e.getMessage());
        }
    }

    private static class CodeFile {
        private Long id;
        private String name;
        private String path;
        private String content;
        private Boolean isDirectory;
        private String language;
        private Long lastModifiedBy;
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getPath() { return path; }
        public String getContent() { return content; }
        public Boolean getIsDirectory() { return isDirectory; }
        public String getLanguage() { return language; }
        public Long getLastModifiedBy() { return lastModifiedBy; }
    }

    public List<BranchResponse> listBranches(Long projectId) {
        return branchRepository.findByProjectId(projectId).stream()
                .map(BranchResponse::fromEntity)
                .toList();
    }

    public BranchResponse getBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
        return BranchResponse.fromEntity(branch);
    }

    public BranchResponse getDefaultBranch(Long projectId) {
        Branch branch = branchRepository.findByProjectIdAndIsDefaultTrue(projectId)
                .orElseThrow(() -> new NotFoundException("No default branch found"));
        return BranchResponse.fromEntity(branch);
    }

    @Transactional
    public RepositoryResponse createRepository(CreateRepositoryRequest request) {
        if (repositoryRepository.findByProjectIdAndName(request.projectId(), request.name()).isPresent()) {
            throw new BadRequestException("Repository already exists: " + request.name());
        }

        Branch mainBranch = branchRepository.findByProjectIdAndName(request.projectId(), "main")
                .orElseGet(() -> {
                    Branch newBranch = branchRepository.save(Branch.builder()
                            .name("main")
                            .projectId(request.projectId())
                            .isDefault(true)
                            .createdAt(Instant.now())
                            .build());
                    entityManager.flush();
                    entityManager.clear();
                    return branchRepository.findByProjectIdAndName(request.projectId(), "main")
                            .orElse(newBranch);
                });

        Repository repo = Repository.builder()
                .name(request.name())
                .projectId(request.projectId())
                .defaultBranchId(mainBranch.getId())
                .isPublic(request.isPublic() != null ? request.isPublic() : false)
                .createdAt(Instant.now())
                .build();
        repo = repositoryRepository.save(repo);
        return RepositoryResponse.fromEntity(repo);
    }

    public List<RepositoryResponse> listRepositories(Long projectId) {
        return repositoryRepository.findByProjectId(projectId).stream()
                .map(RepositoryResponse::fromEntity)
                .toList();
    }

    public RepositoryResponse getRepository(Long id) {
        Repository repo = repositoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Repository not found"));
        return RepositoryResponse.fromEntity(repo);
    }

    @Transactional
    public PullRequestResponse createPullRequest(CreatePullRequestRequest request) {
        PullRequest pr = PullRequest.builder()
                .repositoryId(request.repositoryId())
                .title(request.title())
                .description(request.description())
                .sourceBranchId(request.sourceBranchId())
                .targetBranchId(request.targetBranchId())
                .authorId(request.authorId())
                .status(PullRequest.PullRequestStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        pr = pullRequestRepository.save(pr);
        return PullRequestResponse.fromEntity(pr);
    }

    public List<PullRequestResponse> listPullRequests(Long repositoryId) {
        return pullRequestRepository.findByRepositoryId(repositoryId).stream()
                .map(PullRequestResponse::fromEntity)
                .toList();
    }

    public List<PullRequestResponse> listOpenPullRequests(Long repositoryId) {
        return pullRequestRepository.findByRepositoryIdAndStatus(repositoryId, PullRequest.PullRequestStatus.OPEN).stream()
                .map(PullRequestResponse::fromEntity)
                .toList();
    }

    public PullRequestResponse getPullRequest(Long id) {
        PullRequest pr = pullRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pull request not found"));
        return PullRequestResponse.fromEntity(pr);
    }

    @Transactional
    public PullRequestResponse mergePullRequest(MergePullRequestRequest request) {
        PullRequest pr = pullRequestRepository.findById(request.pullRequestId())
                .orElseThrow(() -> new NotFoundException("Pull request not found"));

        if (pr.getStatus() != PullRequest.PullRequestStatus.OPEN) {
            throw new BadRequestException("Pull request is not open");
        }

        Branch sourceBranch = branchRepository.findById(pr.getSourceBranchId())
                .orElseThrow(() -> new NotFoundException("Source branch not found"));
        Branch targetBranch = branchRepository.findById(pr.getTargetBranchId())
                .orElseThrow(() -> new NotFoundException("Target branch not found"));

        List<Snapshot> sourceSnapshots = snapshotRepository.findByBranchIdOrderByTimestampDesc(pr.getSourceBranchId());
        Snapshot latestSource = sourceSnapshots.isEmpty() ? null : sourceSnapshots.get(0);
        if (latestSource == null) {
            throw new BadRequestException("Source branch has no snapshots to merge");
        }

        List<Snapshot> targetSnapshots = snapshotRepository.findByBranchIdOrderByTimestampDesc(pr.getTargetBranchId());
        String parentHash = targetSnapshots.isEmpty() ? null : targetSnapshots.get(0).getCommitHash();

        Instant ts = Instant.now();
        String newHash = CommitHashUtil.compute(
                latestSource.getFileId(), parentHash, latestSource.getFullContent(), ts);

        Snapshot mergedSnap = Snapshot.builder()
                .commitHash(newHash)
                .fileId(latestSource.getFileId())
                .parentHash(parentHash)
                .fullContent(latestSource.getFullContent())
                .timestamp(ts)
                .commitMessage("Merged from " + sourceBranch.getName() + ": " + pr.getTitle())
                .authorId(request.authorId())
                .branchId(targetBranch.getId())
                .build();
        snapshotRepository.save(mergedSnap);

        updateBranchLatestSnapshot(targetBranch.getId(), newHash);

        pr.setStatus(PullRequest.PullRequestStatus.MERGED);
        pr.setMergedAt(Instant.now());
        pr.setUpdatedAt(Instant.now());
        pullRequestRepository.save(pr);

        return PullRequestResponse.fromEntity(pr);
    }

    @Transactional
    public PullRequestResponse closePullRequest(Long id, Long authorId) {
        PullRequest pr = pullRequestRepository.findByIdAndAuthorId(id, authorId)
                .orElseThrow(() -> new NotFoundException("Pull request not found or unauthorized"));

        if (pr.getStatus() != PullRequest.PullRequestStatus.OPEN) {
            throw new BadRequestException("Pull request is not open");
        }

        pr.setStatus(PullRequest.PullRequestStatus.CLOSED);
        pr.setUpdatedAt(Instant.now());
        pullRequestRepository.save(pr);

        return PullRequestResponse.fromEntity(pr);
    }

    private record FileUpdateRequest(String content) {}

    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        if (tagRepository.existsByName(request.name())) {
            throw new BadRequestException("Tag already exists: " + request.name());
        }

        snapshotRepository.findByCommitHash(request.commitHash())
                .orElseThrow(() -> new NotFoundException("Snapshot not found: " + request.commitHash()));

        Tag tag = Tag.builder()
                .name(request.name())
                .commitHash(request.commitHash())
                .description(request.description())
                .createdAt(Instant.now())
                .build();
        tag = tagRepository.save(tag);
        return TagResponse.fromEntity(tag);
    }

    public List<TagResponse> listTags() {
        return tagRepository.findAll().stream()
                .map(TagResponse::fromEntity)
                .toList();
    }

    public List<TagResponse> listTagsByCommitHash(String commitHash) {
        return tagRepository.findByCommitHash(commitHash).stream()
                .map(TagResponse::fromEntity)
                .toList();
    }

    public TagResponse getTag(String name) {
        Tag tag = tagRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Tag not found: " + name));
        return TagResponse.fromEntity(tag);
    }

    @Transactional
    public void deleteTag(String name) {
        Tag tag = tagRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Tag not found: " + name));
        tagRepository.delete(tag);
    }
}
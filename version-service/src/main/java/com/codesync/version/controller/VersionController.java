package com.codesync.version.controller;

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
import com.codesync.version.service.VersionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/version")
@RequiredArgsConstructor
public class VersionController {
    private final VersionService versionService;

    @PostMapping("/snapshots")
    public ResponseEntity<SnapshotDetailResponse> create(@Valid @RequestBody CreateSnapshotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.createSnapshot(request));
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<SnapshotSummaryResponse>> list(
            @RequestParam Long fileId,
            @RequestParam(required = false) Long branchId) {
        if (branchId != null) {
            return ResponseEntity.ok(versionService.listHistoryByBranch(fileId, branchId));
        }
        return ResponseEntity.ok(versionService.listHistory(fileId));
    }

    @GetMapping("/snapshots/{commitHash}")
    public ResponseEntity<SnapshotDetailResponse> get(@PathVariable String commitHash) {
        return ResponseEntity.ok(versionService.getSnapshot(commitHash));
    }

    @GetMapping("/diffs")
    public ResponseEntity<DiffResponse> diff(
            @RequestParam String fromHash, @RequestParam String toHash) {
        return ResponseEntity.ok(versionService.diff(fromHash, toHash));
    }

    @PostMapping("/restore")
    public ResponseEntity<SnapshotDetailResponse> restore(@Valid @RequestBody RestoreSnapshotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.restore(request));
    }

    @PostMapping("/branches")
    public ResponseEntity<BranchResponse> createBranch(@Valid @RequestBody CreateBranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.createBranch(request));
    }

    @GetMapping("/branches")
    public ResponseEntity<List<BranchResponse>> listBranches(@RequestParam Long projectId) {
        return ResponseEntity.ok(versionService.listBranches(projectId));
    }

    @GetMapping("/branches/{id}")
    public ResponseEntity<BranchResponse> getBranch(@PathVariable Long id) {
        return ResponseEntity.ok(versionService.getBranch(id));
    }

    @PostMapping("/branches/{id}/checkout")
    public ResponseEntity<Void> checkout(@PathVariable Long id) {
        versionService.checkoutBranch(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/branches/default")
    public ResponseEntity<BranchResponse> getDefaultBranch(@RequestParam Long projectId) {
        return ResponseEntity.ok(versionService.getDefaultBranch(projectId));
    }

    @PostMapping("/repositories")
    public ResponseEntity<RepositoryResponse> createRepository(@Valid @RequestBody CreateRepositoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.createRepository(request));
    }

    @GetMapping("/repositories")
    public ResponseEntity<List<RepositoryResponse>> listRepositories(@RequestParam Long projectId) {
        return ResponseEntity.ok(versionService.listRepositories(projectId));
    }

    @GetMapping("/repositories/{id}")
    public ResponseEntity<RepositoryResponse> getRepository(@PathVariable Long id) {
        return ResponseEntity.ok(versionService.getRepository(id));
    }

    @PostMapping("/pull-requests")
    public ResponseEntity<PullRequestResponse> createPullRequest(@Valid @RequestBody CreatePullRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.createPullRequest(request));
    }

    @GetMapping("/pull-requests")
    public ResponseEntity<List<PullRequestResponse>> listPullRequests(@RequestParam Long repositoryId) {
        return ResponseEntity.ok(versionService.listPullRequests(repositoryId));
    }

    @GetMapping("/pull-requests/open")
    public ResponseEntity<List<PullRequestResponse>> listOpenPullRequests(@RequestParam Long repositoryId) {
        return ResponseEntity.ok(versionService.listOpenPullRequests(repositoryId));
    }

    @GetMapping("/pull-requests/{id}")
    public ResponseEntity<PullRequestResponse> getPullRequest(@PathVariable Long id) {
        return ResponseEntity.ok(versionService.getPullRequest(id));
    }

    @PostMapping("/pull-requests/merge")
    public ResponseEntity<PullRequestResponse> mergePullRequest(@Valid @RequestBody MergePullRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.mergePullRequest(request));
    }

    @PostMapping("/pull-requests/{id}/close")
    public ResponseEntity<PullRequestResponse> closePullRequest(
            @PathVariable Long id,
            @RequestParam Long authorId) {
        return ResponseEntity.ok(versionService.closePullRequest(id, authorId));
    }

    @PostMapping("/tags")
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.createTag(request));
    }

    @GetMapping("/tags")
    public ResponseEntity<List<TagResponse>> listTags() {
        return ResponseEntity.ok(versionService.listTags());
    }

    @GetMapping("/tags/by-commit/{commitHash}")
    public ResponseEntity<List<TagResponse>> listTagsByCommit(@PathVariable String commitHash) {
        return ResponseEntity.ok(versionService.listTagsByCommitHash(commitHash));
    }

    @GetMapping("/tags/{name}")
    public ResponseEntity<TagResponse> getTag(@PathVariable String name) {
        return ResponseEntity.ok(versionService.getTag(name));
    }

    @DeleteMapping("/tags/{name}")
    public ResponseEntity<Void> deleteTag(@PathVariable String name) {
        versionService.deleteTag(name);
        return ResponseEntity.noContent().build();
    }
}
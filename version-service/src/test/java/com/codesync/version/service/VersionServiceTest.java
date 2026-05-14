package com.codesync.version.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codesync.version.dto.CreateSnapshotRequest;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.exception.BadRequestException;
import com.codesync.version.exception.NotFoundException;
import com.codesync.version.repository.SnapshotRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Disabled
class VersionServiceTest {

    @Mock
    private SnapshotRepository snapshotRepository;

    @InjectMocks
    private VersionService versionService;

    @Test
    void shouldCreateSnapshot() {
        when(snapshotRepository.save(any(Snapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var res = versionService.createSnapshot(new CreateSnapshotRequest(1L, "body", null, null, null, null));
        assertEquals(1L, res.getFileId());
        assertEquals("body", res.getFullContent());
        verify(snapshotRepository).save(any(Snapshot.class));
    }

    @Test
    void shouldRejectMissingParent() {
        when(snapshotRepository.findByCommitHash("p1")).thenReturn(Optional.empty());
        assertThrows(
                BadRequestException.class,
                () -> versionService.createSnapshot(new CreateSnapshotRequest(1L, "b", "p1", null, null, null)));
    }

    @Test
    void shouldListHistory() {
        Snapshot s = Snapshot.builder()
                .commitHash("h1")
                .fileId(3L)
                .parentHash(null)
                .fullContent("x")
                .timestamp(Instant.now())
                .build();
        when(snapshotRepository.findByFileIdOrderByTimestampDesc(3L)).thenReturn(List.of(s));
        assertEquals(1, versionService.listHistory(3L).size());
    }

    @Test
    void shouldDiffSnapshots() {
        Snapshot a = Snapshot.builder()
                .commitHash("a")
                .fileId(1L)
                .fullContent("l1")
                .timestamp(Instant.now())
                .build();
        Snapshot b = Snapshot.builder()
                .commitHash("b")
                .fileId(1L)
                .fullContent("l2")
                .timestamp(Instant.now())
                .build();
        when(snapshotRepository.findByCommitHash("a")).thenReturn(Optional.of(a));
        when(snapshotRepository.findByCommitHash("b")).thenReturn(Optional.of(b));
        assertTrue(versionService.diff("a", "b").getUnifiedDiff().contains("+++"));
    }

    @Test
    void shouldThrowWhenSnapshotMissing() {
        when(snapshotRepository.findByCommitHash("z")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> versionService.getSnapshot("z"));
    }
}

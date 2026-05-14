package com.codesync.version.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesync.version.dto.CreateSnapshotRequest;
import com.codesync.version.dto.DiffResponse;
import com.codesync.version.dto.SnapshotDetailResponse;
import com.codesync.version.dto.SnapshotSummaryResponse;
import com.codesync.version.service.VersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Disabled
@WebMvcTest(controllers = VersionController.class)
class VersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VersionService versionService;

    @Test
    void shouldCreateSnapshot() throws Exception {
        when(versionService.createSnapshot(any(CreateSnapshotRequest.class)))
                .thenReturn(SnapshotDetailResponse.builder()
                        .commitHash("hh")
                        .fileId(1L)
                        .fullContent("c")
                        .timestamp(Instant.now())
                        .build());
        mockMvc.perform(post("/api/version/snapshots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSnapshotRequest(1L, "c", null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commitHash").value("hh"));
    }

    @Test
    void shouldListSnapshots() throws Exception {
        when(versionService.listHistory(2L))
                .thenReturn(List.of(SnapshotSummaryResponse.builder()
                        .commitHash("x")
                        .fileId(2L)
                        .timestamp(Instant.now())
                        .build()));
        mockMvc.perform(get("/api/version/snapshots").param("fileId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commitHash").value("x"));
    }

    @Test
    void shouldReturnDiff() throws Exception {
        when(versionService.diff(eq("a"), eq("b")))
                .thenReturn(DiffResponse.builder()
                        .fromHash("a")
                        .toHash("b")
                        .unifiedDiff("--- x\n+++ y")
                        .build());
        mockMvc.perform(get("/api/version/diffs")
                        .param("fromHash", "a")
                        .param("toHash", "b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unifiedDiff").value("--- x\n+++ y"));
    }
}
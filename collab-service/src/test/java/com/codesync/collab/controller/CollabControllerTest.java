package com.codesync.collab.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesync.collab.dto.CollabSessionResponse;
import com.codesync.collab.dto.JoinCollabRequest;
import com.codesync.collab.dto.LeaveCollabRequest;
import com.codesync.collab.redis.CollabRedisPublisher;
import com.codesync.collab.service.CollabService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Disabled
@WebMvcTest(controllers = CollabController.class)
class CollabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CollabService collabService;

    @MockBean
    private CollabRedisPublisher collabRedisPublisher;

    @Test
    void shouldJoinViaHttp() throws Exception {
        when(collabService.join(eq(7L), any(JoinCollabRequest.class)))
                .thenReturn(CollabSessionResponse.builder()
                        .sessionId(1L)
                        .fileId(7L)
                        .participants(List.of())
                        .build());
        mockMvc.perform(post("/api/collab/files/7/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinCollabRequest("u1", "Dev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(1));
    }

    @Test
    void shouldLeaveViaHttp() throws Exception {
        mockMvc.perform(post("/api/collab/files/8/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveCollabRequest("u1"))))
                .andExpect(status().isNoContent());
        verify(collabService).leave(8L, "u1");
    }
}
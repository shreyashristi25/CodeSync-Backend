package com.codesync.collab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollabBroadcastEnvelope {
    private Long fileId;
    private CollabMessageType type;
    private String userId;
    private Map<String, Object> payload;

    public CollabBroadcastEnvelope() {}

    public CollabBroadcastEnvelope(Long fileId, CollabMessageType type, String userId, Map<String, Object> payload) {
        this.fileId = fileId;
        this.type = type;
        this.userId = userId;
        this.payload = payload;
    }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public CollabMessageType getType() { return type; }
    public void setType(CollabMessageType type) { this.type = type; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public static CollabBroadcastEnvelopeBuilder builder() { return new CollabBroadcastEnvelopeBuilder(); }

    public static class CollabBroadcastEnvelopeBuilder {
        private Long fileId;
        private CollabMessageType type;
        private String userId;
        private Map<String, Object> payload;

        public CollabBroadcastEnvelopeBuilder fileId(Long fileId) { this.fileId = fileId; return this; }
        public CollabBroadcastEnvelopeBuilder type(CollabMessageType type) { this.type = type; return this; }
        public CollabBroadcastEnvelopeBuilder userId(String userId) { this.userId = userId; return this; }
        public CollabBroadcastEnvelopeBuilder payload(Map<String, Object> payload) { this.payload = payload; return this; }
        public CollabBroadcastEnvelope build() { return new CollabBroadcastEnvelope(fileId, type, userId, payload); }
    }
}
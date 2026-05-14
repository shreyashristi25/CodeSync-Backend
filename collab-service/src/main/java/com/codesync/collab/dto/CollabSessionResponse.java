package com.codesync.collab.dto;

import java.util.List;
import java.util.stream.Collectors;

public class CollabSessionResponse {
    private Long sessionId;
    private String sessionUuid;
    private String joinLink;
    private Long fileId;
    private String hostUserId;
    private List<ParticipantView> participants;

    public CollabSessionResponse() {}

    public CollabSessionResponse(Long sessionId, String sessionUuid, Long fileId, String hostUserId, List<ParticipantView> participants) {
        this.sessionId = sessionId;
        this.sessionUuid = sessionUuid;
        this.fileId = fileId;
        this.hostUserId = hostUserId;
        this.participants = participants;
        this.joinLink = "/collab/join/" + sessionUuid;
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getSessionUuid() { return sessionUuid; }
    public void setSessionUuid(String sessionUuid) { this.sessionUuid = sessionUuid; }
    public String getJoinLink() { return joinLink; }
    public void setJoinLink(String joinLink) { this.joinLink = joinLink; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public String getHostUserId() { return hostUserId; }
    public void setHostUserId(String hostUserId) { this.hostUserId = hostUserId; }
    public List<ParticipantView> getParticipants() { return participants; }
    public void setParticipants(List<ParticipantView> participants) { this.participants = participants; }

    public static CollabSessionResponseBuilder builder() { return new CollabSessionResponseBuilder(); }

    public static class CollabSessionResponseBuilder {
        private Long sessionId;
        private String sessionUuid;
        private Long fileId;
        private String hostUserId;
        private List<ParticipantView> participants;

        public CollabSessionResponseBuilder sessionId(Long sessionId) { this.sessionId = sessionId; return this; }
        public CollabSessionResponseBuilder sessionUuid(String sessionUuid) { this.sessionUuid = sessionUuid; return this; }
        public CollabSessionResponseBuilder fileId(Long fileId) { this.fileId = fileId; return this; }
        public CollabSessionResponseBuilder hostUserId(String hostUserId) { this.hostUserId = hostUserId; return this; }
        public CollabSessionResponseBuilder participants(List<ParticipantView> participants) { this.participants = participants; return this; }
        public CollabSessionResponse build() { return new CollabSessionResponse(sessionId, sessionUuid, fileId, hostUserId, participants); }

        public static class ParticipantViewBuilder {
            private String userId;
            private String displayName;

            public ParticipantViewBuilder userId(String userId) { this.userId = userId; return this; }
            public ParticipantViewBuilder displayName(String displayName) { this.displayName = displayName; return this; }
            public ParticipantView build() { return new ParticipantView(userId, displayName); }
        }
    }

    public static class ParticipantView {
        private String userId;
        private String displayName;

        public ParticipantView() {}

        public ParticipantView(String userId, String displayName) {
            this.userId = userId;
            this.displayName = displayName;
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public static ParticipantViewBuilder builder() { return new ParticipantViewBuilder(); }

        public static class ParticipantViewBuilder {
            private String userId;
            private String displayName;

            public ParticipantViewBuilder userId(String userId) { this.userId = userId; return this; }
            public ParticipantViewBuilder displayName(String displayName) { this.displayName = displayName; return this; }
            public ParticipantView build() { return new ParticipantView(userId, displayName); }
        }
    }
}
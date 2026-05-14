package com.codesync.collab.redis;

import com.codesync.collab.dto.CollabBroadcastEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CollabRedisSubscriber implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(CollabRedisSubscriber.class);

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null) {
            log.debug("Received null message from Redis");
            return;
        }
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            log.debug("Received Redis message: {}", json);
            
            CollabBroadcastEnvelope envelope = objectMapper.readValue(json, CollabBroadcastEnvelope.class);
            if (envelope.getFileId() == null) {
                log.debug("Envelope has no fileId, skipping");
                return;
            }
            
            String destination = "/topic/collab/" + envelope.getFileId();
            log.debug("Forwarding message to WebSocket topic: {}", destination);
            messagingTemplate.convertAndSend(destination, envelope);
            log.debug("Message forwarded successfully");
        } catch (Exception e) {
            log.warn("Failed to dispatch collab redis message: {}", e.getMessage());
        }
    }
}

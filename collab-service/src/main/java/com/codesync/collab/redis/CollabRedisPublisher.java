package com.codesync.collab.redis;

import com.codesync.collab.dto.CollabBroadcastEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CollabRedisPublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${codesync.collab.redis-channel}")
    private String channel;

    public void publish(CollabBroadcastEnvelope envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            log.debug("Publishing to Redis channel {}: {}", channel, json);
            stringRedisTemplate.convertAndSend(channel, json);
            log.debug("Published successfully to channel {}", channel);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize collab envelope", e);
            throw new IllegalStateException("Failed to serialize collab envelope", e);
        }
    }
}

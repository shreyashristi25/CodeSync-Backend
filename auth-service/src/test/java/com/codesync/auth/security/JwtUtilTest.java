package com.codesync.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    @Test
    void shouldGenerateAndValidateToken() {
        JwtUtil jwtUtil = new JwtUtil("12345678901234567890123456789012", 3600000);
        String token = jwtUtil.generateToken("test@example.com", Map.of("role", "USER"));
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("test@example.com", jwtUtil.extractSubject(token));
        assertEquals("USER", jwtUtil.extractAllClaims(token).get("role"));
    }
}

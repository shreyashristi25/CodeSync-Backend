package com.codesync.auth.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BadRequestExceptionTest {

    @Test
    void shouldThrowBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, 
            () -> { throw new BadRequestException("Invalid request"); });
        
        assertEquals("Invalid request", ex.getMessage());
    }
}

class UnauthorizedExceptionTest {

    @Test
    void shouldThrowUnauthorizedException() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
            () -> { throw new UnauthorizedException("Unauthorized access"); });
        
        assertEquals("Unauthorized access", ex.getMessage());
    }
}
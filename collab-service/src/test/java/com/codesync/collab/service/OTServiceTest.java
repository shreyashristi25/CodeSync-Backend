package com.codesync.collab.service;

import com.codesync.collab.service.OTService.DocumentOperation;
import com.codesync.collab.service.OTService.OperationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OTServiceTest {

    @InjectMocks
    private OTService otService;

    @Test
    void shouldInsertText() {
        otService.setDocument(1L, "Hello");
        
        DocumentOperation op = DocumentOperation.builder()
                .userId("user1")
                .type(OperationType.INSERT)
                .position(5)
                .text(" World")
                .timestamp(System.currentTimeMillis())
                .build();

        DocumentOperation result = otService.applyOperation(1L, op);

        assertNotNull(result);
    }

    @Test
    void shouldGetDocument() {
        otService.setDocument(1L, "Test content");

        String result = otService.getDocument(1L);

        assertEquals("Test content", result);
    }

    @Test
    void shouldReturnEmptyStringForUnknownDocument() {
        String result = otService.getDocument(999L);

        assertEquals("", result);
    }

    @Test
    void shouldTransformInsertWithInsert() {
        DocumentOperation op1 = DocumentOperation.builder()
                .userId("user1")
                .type(OperationType.INSERT)
                .position(10)
                .text(" inserted")
                .build();

        DocumentOperation op2 = DocumentOperation.builder()
                .userId("user2")
                .type(OperationType.INSERT)
                .position(5)
                .text("text")
                .build();

        DocumentOperation result = otService.transform(op1, op2);

        assertNotNull(result);
        assertTrue(result.getPosition() >= 5);
    }

    @Test
    void shouldTransformInsertWithDelete() {
        DocumentOperation op1 = DocumentOperation.builder()
                .userId("user1")
                .type(OperationType.INSERT)
                .position(10)
                .text(" inserted")
                .build();

        DocumentOperation op2 = DocumentOperation.builder()
                .userId("user2")
                .type(OperationType.DELETE)
                .position(3)
                .length(4)
                .build();

        DocumentOperation result = otService.transform(op1, op2);

        assertNotNull(result);
    }

    @Test
    void shouldTransformDeleteWithInsert() {
        DocumentOperation op1 = DocumentOperation.builder()
                .userId("user1")
                .type(OperationType.DELETE)
                .position(10)
                .length(5)
                .build();

        DocumentOperation op2 = DocumentOperation.builder()
                .userId("user2")
                .type(OperationType.INSERT)
                .position(5)
                .text("text")
                .build();

        DocumentOperation result = otService.transform(op1, op2);

        assertNotNull(result);
    }

    @Test
    void shouldTransformDeleteWithDelete() {
        DocumentOperation op1 = DocumentOperation.builder()
                .userId("user1")
                .type(OperationType.DELETE)
                .position(10)
                .length(5)
                .build();

        DocumentOperation op2 = DocumentOperation.builder()
                .userId("user2")
                .type(OperationType.DELETE)
                .position(3)
                .length(4)
                .build();

        DocumentOperation result = otService.transform(op1, op2);

        assertNotNull(result);
    }

    @Test
    void shouldReturnOriginalWhenDifferentTypes() {
        DocumentOperation op1 = DocumentOperation.builder()
                .userId("user1")
                .type(OperationType.INSERT)
                .position(10)
                .text("text")
                .build();

        DocumentOperation op2 = DocumentOperation.builder()
                .userId("user2")
                .type(OperationType.FORMAT)
                .position(5)
                .text("bold")
                .build();

        DocumentOperation result = otService.transform(op1, op2);

        assertEquals(op1.getPosition(), result.getPosition());
    }

    @Test
    void shouldApplyReplaceOperation() {
        otService.setDocument(1L, "Hello World");
        
        DocumentOperation op = DocumentOperation.builder()
                .userId("user1")
                .type(OperationType.REPLACE)
                .position(0)
                .text("Hi")
                .build();

        otService.applyOperation(1L, op);

        String result = otService.getDocument(1L);
        assertNotNull(result);
    }
}
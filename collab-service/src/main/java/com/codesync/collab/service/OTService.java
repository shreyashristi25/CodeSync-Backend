package com.codesync.collab.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

@Service
public class OTService {
    private final ConcurrentHashMap<Long, DocumentState> documentStates = new ConcurrentHashMap<>();

    public DocumentOperation applyOperation(Long fileId, DocumentOperation operation) {
        DocumentState state = documentStates.computeIfAbsent(fileId, k -> new DocumentState());
        
        synchronized (state) {
            List<DocumentOperation> concurrentOps = new ArrayList<>(state.pendingOps);
            state.pendingOps.clear();
            
            DocumentOperation transformed = operation;
            for (DocumentOperation concurrent : concurrentOps) {
                if (!concurrent.getUserId().equals(operation.getUserId())) {
                    transformed = transform(transformed, concurrent);
                }
            }
            
            state.document = applyToDocument(state.document, transformed);
            state.lastOperation = transformed;
            
            return transformed;
        }
    }

    public String getDocument(Long fileId) {
        DocumentState state = documentStates.get(fileId);
        return state != null ? state.document : "";
    }

    public DocumentOperation transform(DocumentOperation op1, DocumentOperation op2) {
        if (op1.getType() != op2.getType()) {
            return op1;
        }

        if (op1.getType() == OperationType.INSERT && op2.getType() == OperationType.INSERT) {
            if (op1.getPosition() >= op2.getPosition()) {
                return op1.withPosition(op1.getPosition() + op2.getText().length());
            }
            return op1;
        }

        if (op1.getType() == OperationType.INSERT && op2.getType() == OperationType.DELETE) {
            if (op1.getPosition() > op2.getPosition()) {
                return op1.withPosition(Math.max(op2.getPosition(), op1.getPosition() - op2.getLength()));
            }
            return op1;
        }

        if (op1.getType() == OperationType.DELETE && op2.getType() == OperationType.INSERT) {
            if (op1.getPosition() >= op2.getPosition()) {
                return op1.withPosition(op1.getPosition() + op2.getText().length());
            }
            return op1;
        }

        if (op1.getType() == OperationType.DELETE && op2.getType() == OperationType.DELETE) {
            int op1Start = op1.getPosition();
            int op1End = op1.getPosition() + op1.getLength();
            int op2Start = op2.getPosition();
            int op2End = op2.getPosition() + op2.getLength();

            if (op1Start >= op2End) {
                return op1.withPosition(op1.getPosition() - op2.getLength());
            }
            if (op1End <= op2Start) {
                return op1;
            }
            if (op1Start >= op2Start && op1End <= op2End) {
                return op1.withPosition(op2Start).withLength(0);
            }
            if (op1Start < op2Start) {
                return op1.withLength(op2Start - op1Start);
            }
            return op1.withPosition(op2Start).withLength(op1.getPosition() + op1.getLength() - op2End);
        }

        return op1;
    }

    private String applyToDocument(String doc, DocumentOperation op) {
        if (doc == null) doc = "";
        
        switch (op.getType()) {
            case INSERT:
                int pos = Math.min(op.getPosition(), doc.length());
                return doc.substring(0, pos) + op.getText() + doc.substring(pos);
            case DELETE:
                int start = Math.min(op.getPosition(), doc.length());
                int end = Math.min(start + op.getLength(), doc.length());
                return doc.substring(0, start) + doc.substring(end);
            case REPLACE:
                return op.getText();
            default:
                return doc;
        }
    }

    public void setDocument(Long fileId, String document) {
        documentStates.computeIfAbsent(fileId, k -> new DocumentState()).document = document;
    }

    @Data
    @Builder
    public static class DocumentOperation {
        private String userId;
        private OperationType type;
        private int position;
        private int length;
        private String text;
        private long timestamp;

        public DocumentOperation withPosition(int newPosition) {
            return DocumentOperation.builder()
                    .userId(this.userId)
                    .type(this.type)
                    .position(newPosition)
                    .length(this.length)
                    .text(this.text)
                    .timestamp(this.timestamp)
                    .build();
        }

        public DocumentOperation withLength(int newLength) {
            return DocumentOperation.builder()
                    .userId(this.userId)
                    .type(this.type)
                    .position(this.position)
                    .length(newLength)
                    .text(this.text)
                    .timestamp(this.timestamp)
                    .build();
        }
    }

    public enum OperationType {
        INSERT,
        DELETE,
        REPLACE,
        FORMAT
    }

    private static class DocumentState {
        String document = "";
        DocumentOperation lastOperation;
        List<DocumentOperation> pendingOps = new ArrayList<>();
    }
}
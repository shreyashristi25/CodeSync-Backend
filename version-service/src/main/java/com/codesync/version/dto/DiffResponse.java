package com.codesync.version.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiffResponse {
    private String fromHash;
    private String toHash;
    private String unifiedDiff;
}

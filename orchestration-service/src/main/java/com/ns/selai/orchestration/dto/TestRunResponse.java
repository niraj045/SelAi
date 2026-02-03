package com.ns.selai.orchestration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRunResponse {
    private Long id;
    private Long projectId;
    private String url; // Added for completeness
    private String status;
    private String browser;
    private String testType; // Added for completeness
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer totalTests;
    private Integer passedTests;
    private Integer failedTests;
    private String errorMessage;
}

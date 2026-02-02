package com.ns.selai.orchestration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRunResponse {
    private Long id;
    private Long projectId;
    private String status;
    private String browser;
    private String startedAt;
    private String completedAt;
    private Integer totalTests;
    private Integer passedTests;
    private Integer failedTests;
    private String errorMessage;
}

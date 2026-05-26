package com.ns.selai.reporting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportGenerateRequest {

    @NotNull(message = "Test run ID is required")
    private Long testRunId;

    private String projectName;

    @Size(max = 1000, message = "URL must not exceed 1000 characters")
    private String url;

    private String status;
    private Integer totalTests;
    private Integer passedTests;
    private Integer failedTests;
    private String errorMessage;

    @Valid
    private List<TestCaseResult> testCases;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResult {
        private String name;
        private String status;
        private String errorMessage;
        private String screenshotPath;
        private Long executionTimeMs;
    }
}

package com.ns.selai.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private Long testRunId;
    private String projectName;
    private String url;
    private String status;
    private Integer totalTests;
    private Integer passedTests;
    private Integer failedTests;
    private Double passRate;
    private String errorMessage;
    private LocalDateTime generatedAt;
    private LocalDateTime updatedAt;
    private String htmlUrl;
    private String markdownUrl;
    private String pdfUrl;
}

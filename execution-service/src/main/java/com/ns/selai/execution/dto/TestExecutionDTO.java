package com.ns.selai.execution.dto;

import com.ns.selai.execution.model.TestExecution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestExecutionDTO {
    private Long id;
    private Long testRunId;
    private String testName;
    private String testDescription;
    private TestExecution.TestExecutionStatus status;
    private String errorMessage;
    private String screenshotPath;
    private String resultDetails;
    private Long executionTimeMs;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime executedAt;
}

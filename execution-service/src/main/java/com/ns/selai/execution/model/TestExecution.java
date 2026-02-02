package com.ns.selai.execution.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_run_id", nullable = false)
    private Long testRunId;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "test_description", columnDefinition = "TEXT")
    private String testDescription;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TestExecutionStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "screenshot_path")
    private String screenshotPath;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @CreationTimestamp
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    public enum TestExecutionStatus {
        PENDING,
        RUNNING,
        PASSED,
        FAILED,
        SKIPPED
    }
}

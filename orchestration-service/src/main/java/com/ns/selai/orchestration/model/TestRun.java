package com.ns.selai.orchestration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TestRunStatus status;

    @Column(name = "browser")
    private String browser;

    @CreationTimestamp
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_tests")
    private Integer totalTests;

    @Column(name = "passed_tests")
    private Integer passedTests;

    @Column(name = "failed_tests")
    private Integer failedTests;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Add relationship later if needed
    // @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL)
    // private List<TestExecution> testExecutions;

    public enum TestRunStatus {
        PENDING,
        RUNNING,
        PASSED,
        FAILED,
        STOPPED
    }
}

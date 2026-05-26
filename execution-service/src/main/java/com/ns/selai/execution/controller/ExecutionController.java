package com.ns.selai.execution.controller;

import com.ns.selai.execution.dto.ExecutionRequest;
import com.ns.selai.execution.dto.ExecutionResponse;
import com.ns.selai.execution.dto.TestExecutionDTO;
import com.ns.selai.execution.service.TestExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for Execution Service to receive test cases and start execution.
 */
@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
@Slf4j
public class ExecutionController {

    private final TestExecutionService testExecutionService;

    /**
     * Receives test cases from Orchestration Service and starts their execution.
     * POST /api/execute
     */
    @PostMapping
    public ResponseEntity<ExecutionResponse> executeTests(@Valid @RequestBody ExecutionRequest request) {
        log.info("REST request to execute tests for test run ID: {}", request.getTestRunId());

        testExecutionService.executeTestRun(request);

        return new ResponseEntity<>(
                ExecutionResponse.builder()
                        .testRunId(request.getTestRunId())
                        .status("ACCEPTED")
                        .message("Test execution initiated for run ID: " + request.getTestRunId())
                        .build(),
                HttpStatus.ACCEPTED);
    }

    @GetMapping("/run/{testRunId}")
    public ResponseEntity<List<TestExecutionDTO>> getExecutionsForRun(@PathVariable Long testRunId) {
        log.info("REST request to get executions for test run ID: {}", testRunId);
        return ResponseEntity.ok(testExecutionService.getExecutionsForRun(testRunId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestExecutionDTO> getExecution(@PathVariable Long id) {
        log.info("REST request to get execution ID: {}", id);
        return ResponseEntity.ok(testExecutionService.getExecution(id));
    }
}

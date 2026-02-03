package com.ns.selai.execution.controller;

import com.ns.selai.execution.dto.ExecutionRequest;
import com.ns.selai.execution.dto.ExecutionResponse;
import com.ns.selai.execution.service.TestExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ExecutionResponse> executeTests(@RequestBody ExecutionRequest request) {
        log.info("REST request to execute tests for test run ID: {}", request.getTestRunId());

        // Execute tests in a separate thread to avoid blocking the HTTP response
        // For simplicity, calling directly here. In a real-world scenario, this might
        // be async or use a message queue.
        testExecutionService.executeTestRun(request.getTestRunId(), request.getTestCases());

        return new ResponseEntity<>(
                new ExecutionResponse("Test execution initiated for run ID: " + request.getTestRunId()),
                HttpStatus.ACCEPTED);
    }
}
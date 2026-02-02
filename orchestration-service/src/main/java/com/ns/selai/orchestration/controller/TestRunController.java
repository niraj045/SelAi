package com.ns.selai.orchestration.controller;

import com.ns.selai.orchestration.dto.TestRunRequest;
import com.ns.selai.orchestration.dto.TestRunResponse;
import com.ns.selai.orchestration.service.TestOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Test Run Orchestration
 */
@RestController
@RequestMapping("/api/test-runs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TestRunController {

    private final TestOrchestrationService orchestrationService;

    /**
     * Start a new test run
     * POST /api/test-runs
     */
    @PostMapping
    public ResponseEntity<TestRunResponse> startTestRun(@RequestBody TestRunRequest request) {
        log.info("REST request to start test run for project: {}", request.getProjectId());
        TestRunResponse response = orchestrationService.startTestRun(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get test run by ID
     * GET /api/test-runs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TestRunResponse> getTestRun(@PathVariable Long id) {
        log.info("REST request to get test run: {}", id);
        TestRunResponse response = orchestrationService.getTestRun(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all test runs for a project
     * GET /api/test-runs/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TestRunResponse>> getTestRunsByProject(@PathVariable Long projectId) {
        log.info("REST request to get test runs for project: {}", projectId);
        List<TestRunResponse> responses = orchestrationService.getTestRunsByProject(projectId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Stop a running test run
     * POST /api/test-runs/{id}/stop
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stopTestRun(@PathVariable Long id) {
        log.info("REST request to stop test run: {}", id);
        orchestrationService.stopTestRun(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Callback endpoint for Execution Service to update results
     * POST /api/test-runs/{id}/results
     */
    @PostMapping("/{id}/results")
    public ResponseEntity<Void> updateResults(
            @PathVariable Long id,
            @RequestBody ResultsUpdate update) {
        log.info("Updating results for test run {}: passed={}, failed={}",
                id, update.passed(), update.failed());
        orchestrationService.updateTestRunResults(id, update.passed(), update.failed());
        return ResponseEntity.ok().build();
    }

    // Record for results update
    record ResultsUpdate(int passed, int failed) {
    }
}

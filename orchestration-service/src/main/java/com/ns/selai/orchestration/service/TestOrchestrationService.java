package com.ns.selai.orchestration.service;

import com.ns.selai.orchestration.client.AiEngineClient;
import com.ns.selai.orchestration.client.ExecutionServiceClient;
import com.ns.selai.orchestration.dto.TestRunRequest;
import com.ns.selai.orchestration.dto.TestRunResponse;
import com.ns.selai.orchestration.dto.ai.AiAnalysisResponse;
import com.ns.selai.orchestration.model.TestRun;
import com.ns.selai.orchestration.model.TestRun.TestRunStatus;
import com.ns.selai.orchestration.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core Orchestration Service - The Brain of the System
 * 
 * Responsibilities:
 * 1. Receive test run requests
 * 2. Call Python AI Engine for test generation
 * 3. Send generated tests to Execution Service
 * 4. Monitor and update test run status
 * 5. Coordinate failure handling and retry logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestOrchestrationService {

	private final TestRunRepository testRunRepository;
	private final AiEngineClient aiEngineClient;
	private final ExecutionServiceClient executionServiceClient;

	/**
	 * Start a new test run
	 * This is the main entry point for test execution
	 */
	@Transactional
	public TestRunResponse startTestRun(TestRunRequest request) {
		log.info("=== Starting new test run for project: {} ===", request.getProjectId());

		// Step 1: Create test run record with PENDING status
		TestRun testRun = TestRun.builder()
				.projectId(request.getProjectId())
				.status(TestRunStatus.PENDING)
				.browser(request.getBrowser() != null ? request.getBrowser() : "chrome")
				.totalTests(0)
				.passedTests(0)
				.failedTests(0)
				.build();

		testRun = testRunRepository.save(testRun);
		log.info("Test run created with ID: {}", testRun.getId());

		// Step 2: Asynchronously process the test run
		processTestRunAsync(testRun.getId(), request);

		return convertToResponse(testRun);
	}

	/**
	 * Process test run asynchronously
	 * This runs in a separate thread to avoid blocking the API response
	 */
	@Async
	public void processTestRunAsync(Long testRunId, TestRunRequest request) {
		log.info("=== Processing test run {} asynchronously ===", testRunId);

		try {
			// Update status to RUNNING
			updateTestRunStatus(testRunId, TestRunStatus.RUNNING);

			// Step 1: Call Python AI Engine to analyze URL and generate tests
			log.info("Step 1: Calling AI Engine to analyze: {}", request.getUrl());
			AiAnalysisResponse aiResponse = aiEngineClient.analyzeAndGenerateTests(
					request.getUrl(),
					request.getBrowser(),
					request.getTestType());

			if (aiResponse == null || aiResponse.getTests() == null || aiResponse.getTests().isEmpty()) {
				log.error("AI Engine returned no test cases");
				updateTestRunWithError(testRunId, "AI Engine returned no test cases");
				return;
			}

			log.info("AI Engine generated {} test cases", aiResponse.getTests().size());

			// Update total tests count
			TestRun testRun = testRunRepository.findById(testRunId).orElseThrow();
			testRun.setTotalTests(aiResponse.getTests().size());
			testRunRepository.save(testRun);

			// Step 2: Send tests to Execution Service
			log.info("Step 2: Sending {} tests to Execution Service", aiResponse.getTests().size());
			executionServiceClient.executeTests(testRunId, aiResponse);

			log.info("=== Test run {} processing complete ===", testRunId);

		} catch (Exception e) {
			log.error("Error processing test run {}: ", testRunId, e);
			updateTestRunWithError(testRunId, e.getMessage());
		}
	}

	/**
	 * Get test run by ID
	 */
	public TestRunResponse getTestRun(Long id) {
		log.info("Fetching test run: {}", id);
		TestRun testRun = testRunRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Test run not found: " + id));
		return convertToResponse(testRun);
	}

	/**
	 * Get all test runs for a project
	 */
	public List<TestRunResponse> getTestRunsByProject(Long projectId) {
		log.info("Fetching test runs for project: {}", projectId);
		return testRunRepository.findByProjectIdOrderByStartedAtDesc(projectId)
				.stream()
				.map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	/**
	 * Stop a running test run
	 */
	@Transactional
	public void stopTestRun(Long id) {
		log.info("Stopping test run: {}", id);
		TestRun testRun = testRunRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Test run not found: " + id));

		if (testRun.getStatus() == TestRunStatus.RUNNING) {
			testRun.setStatus(TestRunStatus.STOPPED);
			testRun.setCompletedAt(LocalDateTime.now());
			testRunRepository.save(testRun);
			log.info("Test run {} stopped", id);
		}
	}

	/**
	 * Update test run status
	 */
	@Transactional
	public void updateTestRunStatus(Long testRunId, TestRunStatus status) {
		log.info("Updating test run {} status to: {}", testRunId, status);
		TestRun testRun = testRunRepository.findById(testRunId).orElseThrow();
		testRun.setStatus(status);

		if (status == TestRunStatus.PASSED || status == TestRunStatus.FAILED || status == TestRunStatus.STOPPED) {
			testRun.setCompletedAt(LocalDateTime.now());
		}

		testRunRepository.save(testRun);
	}

	/**
	 * Update test run with execution results
	 * `
	 */
	@Transactional
	public void updateTestRunResults(Long testRunId, int passed, int failed) {
		log.info("Updating test run {} results: passed={}, failed={}", testRunId, passed, failed);
		TestRun testRun = testRunRepository.findById(testRunId).orElseThrow();

		testRun.setPassedTests(passed);
		testRun.setFailedTests(failed);
		testRun.setStatus(failed > 0 ? TestRunStatus.FAILED : TestRunStatus.PASSED);
		testRun.setCompletedAt(LocalDateTime.now());

		testRunRepository.save(testRun);
	}

	/**
	 * Update test run with error
	 */
	@Transactional
	public void updateTestRunWithError(Long testRunId, String errorMessage) {
		log.error("Test run {} failed with error: {}", testRunId, errorMessage);
		TestRun testRun = testRunRepository.findById(testRunId).orElseThrow();

		testRun.setStatus(TestRunStatus.FAILED);
		testRun.setErrorMessage(errorMessage);
		testRun.setCompletedAt(LocalDateTime.now());

		testRunRepository.save(testRun);
	}

	/**
	 * Convert TestRun entity to Response DTO
	 */
	private TestRunResponse convertToResponse(TestRun testRun) {
		return TestRunResponse.builder()
				.id(testRun.getId())
				.projectId(testRun.getProjectId())
				.status(testRun.getStatus().name())
				.browser(testRun.getBrowser())
				.startedAt(testRun.getStartedAt() != null ? testRun.getStartedAt().toString() : null)
				.completedAt(testRun.getCompletedAt() != null ? testRun.getCompletedAt().toString() : null)
				.totalTests(testRun.getTotalTests())
				.passedTests(testRun.getPassedTests())
				.failedTests(testRun.getFailedTests())
				.errorMessage(testRun.getErrorMessage())
				.build();
	}
}

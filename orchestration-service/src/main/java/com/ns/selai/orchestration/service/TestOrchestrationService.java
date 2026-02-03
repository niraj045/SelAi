package com.ns.selai.orchestration.service;

import com.ns.selai.orchestration.client.AiEngineClient;
import com.ns.selai.orchestration.client.ExecutionServiceClient;
import com.ns.selai.orchestration.dto.TestRunRequest;
import com.ns.selai.orchestration.dto.TestRunResponse;
import com.ns.selai.orchestration.dto.ai.AiAnalysisResponse;
import com.ns.selai.orchestration.model.TestRun;
import com.ns.selai.orchestration.model.TestRun.TestRunStatus;
import com.ns.selai.orchestration.dto.ExternalServiceException;
import com.ns.selai.orchestration.dto.TestRunNotFoundException;
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestOrchestrationService {

	private final TestRunRepository testRunRepository;
	private final AiEngineClient aiEngineClient;
	private final ExecutionServiceClient executionServiceClient;

	@Transactional
	public TestRunResponse startTestRun(TestRunRequest request) {
		log.info("=== Starting new test run for project: {} ===", request.getProjectId());

		TestRun testRun = TestRun.builder()
				.projectId(request.getProjectId())
				.url(request.getUrl())
				.testType(request.getTestType())
				.status(TestRunStatus.PENDING)
				.browser(request.getBrowser() != null ? request.getBrowser() : "chrome")
				.totalTests(0)
				.passedTests(0)
				.failedTests(0)
				.build();

		testRun = testRunRepository.save(testRun);
		log.info("Test run created with ID: {}", testRun.getId());

		processTestRunAsync(testRun.getId(), request);

		return convertToResponse(testRun);
	}

	@Async
	public void processTestRunAsync(Long testRunId, TestRunRequest request) {
		log.info("=== Processing test run {} asynchronously ===", testRunId);

		try {
			updateTestRunStatus(testRunId, TestRunStatus.RUNNING);

			log.info("Step 1: Calling AI Engine to analyze URL: {}, Browser: {}, TestType: {}", request.getUrl(),
					request.getBrowser(), request.getTestType());
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

			TestRun testRun = testRunRepository.findById(testRunId)
					.orElseThrow(() -> new TestRunNotFoundException("Test run not found with ID: " + testRunId));
			testRun.setTotalTests(aiResponse.getTests().size());
			testRunRepository.save(testRun);

			log.info("Step 2: Sending {} tests to Execution Service", aiResponse.getTests().size());
			executionServiceClient.executeTests(testRunId, aiResponse);

			log.info("=== Test run {} processing complete ===", testRunId);

		} catch (TestRunNotFoundException e) {
			log.error("Test run {} not found during async processing: {}", testRunId, e.getMessage());
		} catch (Exception e) {
			log.error("Error processing test run {}: ", testRunId, e);
			updateTestRunWithError(testRunId, e.getMessage());
		}
	}

	public TestRunResponse getTestRun(Long id) {
		log.info("Fetching test run: {}", id);
		TestRun testRun = testRunRepository.findById(id)
				.orElseThrow(() -> new TestRunNotFoundException("Test run not found with ID: " + id));
		return convertToResponse(testRun);
	}

	public List<TestRunResponse> getTestRunsByProject(Long projectId) {
		log.info("Fetching test runs for project: {}", projectId);
		return testRunRepository.findByProjectIdOrderByStartedAtDesc(projectId)
				.stream()
				.map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	@Transactional
	public void stopTestRun(Long id) {
		log.info("Stopping test run: {}", id);
		TestRun testRun = testRunRepository.findById(id)
				.orElseThrow(() -> new TestRunNotFoundException("Test run not found with ID: " + id));

		if (testRun.getStatus() == TestRunStatus.RUNNING) {
			testRun.setStatus(TestRunStatus.STOPPED);
			testRun.setCompletedAt(LocalDateTime.now());
			testRunRepository.save(testRun);
			log.info("Test run {} stopped", id);
		}
	}

	@Transactional
	public void updateTestRunStatus(Long testRunId, TestRunStatus status) {
		log.info("Updating test run {} status to: {}", testRunId, status);
		TestRun testRun = testRunRepository.findById(testRunId)
				.orElseThrow(() -> new TestRunNotFoundException("Test run not found with ID: " + testRunId));
		testRun.setStatus(status);

		if (status == TestRunStatus.PASSED || status == TestRunStatus.FAILED || status == TestRunStatus.STOPPED) {
			testRun.setCompletedAt(LocalDateTime.now());
		}

		testRunRepository.save(testRun);
	}

	@Transactional
	public void updateTestRunResults(Long testRunId, int passed, int failed) {
		log.info("Updating test run {} results: passed={}, failed={}", testRunId, passed, failed);
		TestRun testRun = testRunRepository.findById(testRunId)
				.orElseThrow(() -> new TestRunNotFoundException("Test run not found with ID: " + testRunId));

		testRun.setPassedTests(passed);
		testRun.setFailedTests(failed);
		testRun.setStatus(failed > 0 ? TestRunStatus.FAILED : TestRunStatus.PASSED);
		testRun.setCompletedAt(LocalDateTime.now());

		testRunRepository.save(testRun);
	}

	@Transactional
	public void updateTestRunWithError(Long testRunId, String errorMessage) {
		log.error("Test run {} failed with error: {}", testRunId, errorMessage);
		TestRun testRun = testRunRepository.findById(testRunId)
				.orElseThrow(() -> new TestRunNotFoundException("Test run not found with ID: " + testRunId));

		testRun.setStatus(TestRunStatus.FAILED);
		testRun.setErrorMessage(errorMessage);
		testRun.setCompletedAt(LocalDateTime.now());

		testRunRepository.save(testRun);
	}

	private TestRunResponse convertToResponse(TestRun testRun) {
		return TestRunResponse.builder()
				.id(testRun.getId())
				.projectId(testRun.getProjectId())
				.url(testRun.getUrl())
				.status(testRun.getStatus().name())
				.browser(testRun.getBrowser())
				.testType(testRun.getTestType())
				.startedAt(testRun.getStartedAt())
				.completedAt(testRun.getCompletedAt())
				.totalTests(testRun.getTotalTests())
				.passedTests(testRun.getPassedTests())
				.failedTests(testRun.getFailedTests())
				.errorMessage(testRun.getErrorMessage())
				.build();
	}
}

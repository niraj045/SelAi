package com.ns.selai.orchestration.service;

import com.ns.selai.orchestration.client.AiEngineClient;
import com.ns.selai.orchestration.client.ExecutionServiceClient;
import com.ns.selai.orchestration.client.ReportingServiceClient;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
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
	private final ReportingServiceClient reportingServiceClient;
	@Qualifier("testRunTaskExecutor")
	private final Executor testRunTaskExecutor;

	@Transactional
	public TestRunResponse startTestRun(TestRunRequest request) {
		log.info("=== Starting new test run for project: {} ===", request.getProjectId());

		TestRun testRun = TestRun.builder()
				.projectId(request.getProjectId())
				.url(request.getUrl())
				.testType(request.getTestType())
				.status(TestRunStatus.PENDING)
				.browser(request.getBrowser() != null ? request.getBrowser() : "chrome")
				.userPrompt(request.getUserPrompt())
				.totalTests(0)
				.passedTests(0)
				.failedTests(0)
				.build();

		testRun = testRunRepository.save(testRun);
		log.info("Test run created with ID: {}", testRun.getId());

		Long testRunId = testRun.getId();
		scheduleTestRunProcessing(testRunId, request);

		return convertToResponse(testRun);
	}

	private void scheduleTestRunProcessing(Long testRunId, TestRunRequest request) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					enqueueTestRunProcessing(testRunId, request);
				}
			});
			return;
		}

		enqueueTestRunProcessing(testRunId, request);
	}

	private void enqueueTestRunProcessing(Long testRunId, TestRunRequest request) {
		testRunTaskExecutor.execute(() -> processTestRunAsync(testRunId, request));
	}

	public void processTestRunAsync(Long testRunId, TestRunRequest request) {
		log.info("=== Processing test run {} asynchronously ===", testRunId);

		try {
			updateTestRunStatus(testRunId, TestRunStatus.RUNNING);

			log.info("Step 1: Calling AI Engine to analyze URL: {}, Browser: {}, TestType: {}", request.getUrl(),
					request.getBrowser(), request.getTestType());
			AiAnalysisResponse aiResponse = aiEngineClient.analyzeAndGenerateTests(
					request.getUrl(),
					request.getBrowser(),
					request.getTestType(),
					request.getUserPrompt());

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
			executionServiceClient.executeTests(testRunId, testRun.getBrowser(), aiResponse);

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

	public List<TestRunResponse> getAllTestRuns() {
		log.info("Fetching all test runs");
		return testRunRepository.findAllByOrderByStartedAtDesc()
				.stream()
				.map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	@Transactional
	public void stopTestRun(Long id) {
		log.info("Stopping test run: {}", id);
		TestRun testRun = testRunRepository.findById(id)
				.orElseThrow(() -> new TestRunNotFoundException("Test run not found with ID: " + id));

		if (testRun.getStatus() == TestRunStatus.PENDING || testRun.getStatus() == TestRunStatus.RUNNING) {
			testRun.setStatus(TestRunStatus.STOPPED);
			testRun.setCompletedAt(LocalDateTime.now());
			testRunRepository.save(testRun);
			log.info("Test run {} stopped", id);
		} else {
			log.info("Test run {} is already terminal with status {}", id, testRun.getStatus());
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
		updateTestRunResults(testRunId, null, passed, failed, null, null);
	}

	@Transactional
	public void updateTestRunResults(Long testRunId, Integer total, int passed, int failed, String status,
			String errorMessage) {
		log.info("Updating test run {} results: total={}, passed={}, failed={}, status={}",
				testRunId, total, passed, failed, status);
		TestRun testRun = testRunRepository.findById(testRunId)
				.orElseThrow(() -> new TestRunNotFoundException("Test run not found with ID: " + testRunId));

		if (total != null) {
			testRun.setTotalTests(total);
		}
		testRun.setPassedTests(passed);
		testRun.setFailedTests(failed);
		testRun.setStatus(resolveFinalStatus(status, failed));
		testRun.setErrorMessage(errorMessage);
		testRun.setCompletedAt(LocalDateTime.now());

		TestRun savedTestRun = testRunRepository.save(testRun);
		reportingServiceClient.generateReport(savedTestRun);
	}

	@Transactional
	public void updateTestRunWithError(Long testRunId, String errorMessage) {
		log.error("Test run {} failed with error: {}", testRunId, errorMessage);
		TestRun testRun = testRunRepository.findById(testRunId)
				.orElseThrow(() -> new TestRunNotFoundException("Test run not found with ID: " + testRunId));

		testRun.setStatus(TestRunStatus.FAILED);
		testRun.setErrorMessage(errorMessage);
		testRun.setCompletedAt(LocalDateTime.now());

		TestRun savedTestRun = testRunRepository.save(testRun);
		reportingServiceClient.generateReport(savedTestRun);
	}

	private TestRunStatus resolveFinalStatus(String status, int failed) {
		if (status != null) {
			try {
				return TestRunStatus.valueOf(status.toUpperCase());
			} catch (IllegalArgumentException e) {
				log.warn("Unknown result status '{}'. Falling back to pass/fail counts", status);
			}
		}
		return failed > 0 ? TestRunStatus.FAILED : TestRunStatus.PASSED;
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
				.userPrompt(testRun.getUserPrompt())
				.build();
	}
}

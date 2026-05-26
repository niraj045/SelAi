package com.ns.selai.execution.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.selai.execution.client.OrchestrationCallbackClient;
import com.ns.selai.execution.dto.ExecutionRequest;
import com.ns.selai.execution.dto.TestExecutionDTO;
import com.ns.selai.execution.model.TestExecution;
import com.ns.selai.execution.repository.TestExecutionRepository;
import com.ns.selai.execution.selenium.BrowserManager;
import com.ns.selai.execution.selenium.ScreenshotService;
import com.ns.selai.execution.selenium.StepExecutor;
import com.ns.selai.execution.selenium.StepExecutor.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestExecutionService {

    private final TestExecutionRepository testExecutionRepository;
    private final BrowserManager browserManager;
    private final StepExecutor stepExecutor;
    private final ScreenshotService screenshotService;
    private final OrchestrationCallbackClient orchestrationCallbackClient;
    private final ObjectMapper objectMapper;

    @Value("${execution.browser.default:chrome}")
    private String defaultBrowser;

    @Async
    public void executeTestRun(ExecutionRequest request) {
        Long testRunId = request.getTestRunId();
        List<ExecutionRequest.TestCase> testCases = request.getTestCases();
        if (testCases == null) {
            testCases = List.of();
        }
        int totalTests = testCases != null ? testCases.size() : 0;

        log.info("Starting execution for test run ID: {} with {} test cases", testRunId, totalTests);

        WebDriver driver = null;
        int passedCount = 0;
        int failedCount = 0;
        String fatalError = null;

        try {
            String browser = StringUtils.hasText(request.getBrowser()) ? request.getBrowser() : defaultBrowser;
            driver = browserManager.getWebDriver(browser);

            for (ExecutionRequest.TestCase testCase : testCases) {
                long startTime = System.currentTimeMillis();
                TestExecution testExecution = TestExecution.builder()
                        .testRunId(testRunId)
                        .testName(testCase.getName())
                        .testDescription(testCase.getDescription())
                        .status(TestExecution.TestExecutionStatus.RUNNING)
                        .startedAt(LocalDateTime.now())
                        .build();
                testExecution = testExecutionRepository.save(testExecution);

                try {
                    log.info("Executing test case: {}", testCase.getName());

                    List<StepResult> stepResults = stepExecutor.executeSteps(driver, testCase.getSteps(), testRunId);
                    testExecution.setResultDetails(writeStepResults(stepResults));
                    testExecution.setScreenshotPath(lastScreenshotPath(stepResults));

                    StepResult failedStep = stepResults.stream()
                            .filter(stepResult -> !stepResult.isSuccess())
                            .findFirst()
                            .orElse(null);

                    if (failedStep == null) {
                        testExecution.setStatus(TestExecution.TestExecutionStatus.PASSED);
                        passedCount++;
                    } else {
                        testExecution.setStatus(TestExecution.TestExecutionStatus.FAILED);
                        testExecution.setErrorMessage(failedStep.getMessage());
                        failedCount++;
                    }
                } catch (Exception e) {
                    log.error("Test case '{}' failed: {}", testCase.getName(), e.getMessage());
                    testExecution.setStatus(TestExecution.TestExecutionStatus.FAILED);
                    testExecution.setErrorMessage(e.getMessage());
                    testExecution.setScreenshotPath(screenshotService.captureScreenshot(
                            driver,
                            testRunId,
                            testCase.getName().replaceAll("\\s+", "_") + "_FAIL"));
                    failedCount++;
                } finally {
                    testExecution.setCompletedAt(LocalDateTime.now());
                    testExecution.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                    testExecutionRepository.save(testExecution);
                }
            }
        } catch (Exception e) {
            fatalError = e.getMessage();
            failedCount = Math.max(failedCount, totalTests - passedCount);
            log.error("Execution failed before completing test run {}: {}", testRunId, e.getMessage(), e);
        } finally {
            if (driver != null) {
                browserManager.quitWebDriver(driver);
            }
            log.info("Completed execution for test run ID: {}. Passed: {}, Failed: {}", testRunId, passedCount,
                    failedCount);

            orchestrationCallbackClient.publishResults(request,
                    OrchestrationCallbackClient.ExecutionSummary.builder()
                            .testRunId(testRunId)
                            .total(totalTests)
                            .passed(passedCount)
                            .failed(failedCount)
                            .status(fatalError != null || failedCount > 0 ? "FAILED" : "PASSED")
                            .errorMessage(fatalError)
                            .build());
        }
    }

    @Transactional(readOnly = true)
    public List<TestExecutionDTO> getExecutionsForRun(Long testRunId) {
        return testExecutionRepository.findByTestRunIdOrderByExecutedAtAsc(testRunId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TestExecutionDTO getExecution(Long id) {
        TestExecution execution = testExecutionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found with ID: " + id));
        return toDTO(execution);
    }

    private String writeStepResults(List<StepResult> stepResults) {
        try {
            return objectMapper.writeValueAsString(stepResults);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize step results: {}", e.getMessage());
            return "[]";
        }
    }

    private String lastScreenshotPath(List<StepResult> stepResults) {
        if (stepResults == null || stepResults.isEmpty()) {
            return null;
        }

        for (int i = stepResults.size() - 1; i >= 0; i--) {
            String screenshotPath = stepResults.get(i).getScreenshotPath();
            if (StringUtils.hasText(screenshotPath)) {
                return screenshotPath;
            }
        }
        return null;
    }

    private TestExecutionDTO toDTO(TestExecution execution) {
        return TestExecutionDTO.builder()
                .id(execution.getId())
                .testRunId(execution.getTestRunId())
                .testName(execution.getTestName())
                .testDescription(execution.getTestDescription())
                .status(execution.getStatus())
                .errorMessage(execution.getErrorMessage())
                .screenshotPath(execution.getScreenshotPath())
                .resultDetails(execution.getResultDetails())
                .executionTimeMs(execution.getExecutionTimeMs())
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .executedAt(execution.getExecutedAt())
                .build();
    }
}

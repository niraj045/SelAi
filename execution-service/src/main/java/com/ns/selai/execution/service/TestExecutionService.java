package com.ns.selai.execution.service;

import com.ns.selai.execution.dto.ExecutionRequest;
import com.ns.selai.execution.model.TestExecution;
import com.ns.selai.execution.repository.TestExecutionRepository;
import com.ns.selai.execution.selenium.BrowserManager;
import com.ns.selai.execution.selenium.ScreenshotService;
import com.ns.selai.execution.selenium.StepExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void executeTestRun(Long testRunId, List<ExecutionRequest.TestCase> testCases) {
        log.info("Starting execution for test run ID: {} with {} test cases", testRunId, testCases.size());

        WebDriver driver = null;
        int passedCount = 0;
        int failedCount = 0;

        try {
            driver = browserManager.getWebDriver("chrome");
            stepExecutor.setWebDriver(driver);
            screenshotService.setWebDriver(driver);

            for (ExecutionRequest.TestCase testCase : testCases) {
                TestExecution testExecution = TestExecution.builder()
                        .testRunId(testRunId)
                        .testName(testCase.getName())
                        .testDescription(testCase.getDescription())
                        .status(TestExecution.TestExecutionStatus.PENDING)
                        .executedAt(LocalDateTime.now())
                        .build();
                testExecution = testExecutionRepository.save(testExecution);

                try {
                    log.info("Executing test case: {}", testCase.getName());
                    stepExecutor.executeSteps(testCase.getSteps());
                    testExecution.setStatus(TestExecution.TestExecutionStatus.PASSED);
                    passedCount++;
                } catch (Exception e) {
                    log.error("Test case '{}' failed: {}", testCase.getName(), e.getMessage());
                    testExecution.setStatus(TestExecution.TestExecutionStatus.FAILED);
                    testExecution.setErrorMessage(e.getMessage());
                    testExecution.setScreenshotPath(screenshotService
                            .captureScreenshot(testRunId + "_" + testCase.getName().replaceAll("\\s+", "_") + "_FAIL"));
                    failedCount++;
                } finally {
                    testExecutionRepository.save(testExecution);
                }
            }
        } finally {
            if (driver != null) {
                browserManager.quitWebDriver(driver);
            }
            log.info("Completed execution for test run ID: {}. Passed: {}, Failed: {}", testRunId, passedCount,
                    failedCount);
        }
    }
}
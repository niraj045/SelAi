package com.ns.selai.execution.selenium;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service to capture and save screenshots during test execution
 */
@Service
@Slf4j
public class ScreenshotService {

    @Value("${screenshot.storage.path:./screenshots}")
    private String screenshotBasePath;

    /**
     * Capture screenshot and save to storage
     */
    public String captureScreenshot(WebDriver driver, Long testRunId, String stepName) {
        try {
            // Create directory if not exists
            String testRunDir = screenshotBasePath + "/test-run-" + testRunId;
            Files.createDirectories(Paths.get(testRunDir));

            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String filename = String.format("%s_%s.png", stepName.replaceAll("[^a-zA-Z0-9]", "_"), timestamp);
            String fullPath = testRunDir + "/" + filename;

            // Capture screenshot
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File(fullPath));

            log.info("Screenshot captured: {}", fullPath);
            return fullPath;

        } catch (Exception e) {
            log.error("Failed to capture screenshot: ", e);
            return null;
        }
    }

    /**
     * Capture screenshot with custom name
     */
    public String captureScreenshot(WebDriver driver, String fullPath) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File(fullPath));
            log.info("Screenshot captured: {}", fullPath);
            return fullPath;
        } catch (Exception e) {
            log.error("Failed to capture screenshot: ", e);
            return null;
        }
    }

    /**
     * Get screenshot as byte array (for API response)
     */
    public byte[] getScreenshotAsBytes(String screenshotPath) {
        try {
            Path path = Paths.get(screenshotPath);
            return Files.readAllBytes(path);
        } catch (Exception e) {
            log.error("Failed to read screenshot file: ", e);
            return null;
        }
    }

    /**
     * Delete all screenshots for a test run (cleanup)
     */
    public void deleteTestRunScreenshots(Long testRunId) {
        try {
            String testRunDir = screenshotBasePath + "/test-run-" + testRunId;
            Path path = Paths.get(testRunDir);
            if (Files.exists(path)) {
                FileUtils.deleteDirectory(path.toFile());
                log.info("Deleted screenshots for test run: {}", testRunId);
            }
        } catch (Exception e) {
            log.error("Failed to delete screenshots: ", e);
        }
    }
}

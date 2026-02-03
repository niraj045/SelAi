package com.ns.selai.execution.selenium;

import com.ns.selai.execution.dto.ExecutionRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Executes individual Selenium test steps
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StepExecutor {

    private final ScreenshotService screenshotService;
    private WebDriver driver;

    public void setWebDriver(WebDriver driver) {
        this.driver = driver;
    }

    public void executeSteps(List<ExecutionRequest.TestStep> steps) {
        for (ExecutionRequest.TestStep step : steps) {
            executeStep(this.driver, step, 0L);
        }
    }

    public StepResult executeStep(WebDriver driver, ExecutionRequest.TestStep step, Long testRunId) {
        log.info("Executing step: {} - {}", step.getAction(), step.getSelector());

        long startTime = System.currentTimeMillis();
        StepResult result = new StepResult();
        result.setAction(step.getAction());
        result.setSelector(step.getSelector());

        try {
            switch (step.getAction().toLowerCase()) {
                case "open_url":
                    executeOpenUrl(driver, step.getUrl());
                    break;
                case "click":
                    executeClick(driver, step.getSelector());
                    break;
                case "type":
                    executeType(driver, step.getSelector(), step.getValue());
                    break;
                case "submit":
                    executeSubmit(driver, step.getSelector());
                    break;
                case "wait":
                    executeWait(step.getValue());
                    break;
                case "assert_text":
                    executeAssertText(driver, step.getSelector(), step.getExpectedText());
                    break;
                case "assert_element_present":
                    executeAssertElementPresent(driver, step.getSelector());
                    break;
                case "scroll":
                    executeScroll(driver, step.getSelector());
                    break;
                case "select_dropdown":
                    executeSelectDropdown(driver, step.getSelector(), step.getValue());
                    break;
                case "clear":
                    executeClear(driver, step.getSelector());
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown action: " + step.getAction());
            }

            result.setSuccess(true);
            result.setMessage("Step executed successfully");

            String screenshotPath = screenshotService.captureScreenshot(driver, testRunId, step.getAction());
            result.setScreenshotPath(screenshotPath);

        } catch (Exception e) {
            log.error("Step execution failed: ", e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            result.setErrorType(e.getClass().getSimpleName());

            try {
                String errorScreenshot = screenshotService.captureScreenshot(driver, testRunId,
                        step.getAction() + "_ERROR");
                result.setScreenshotPath(errorScreenshot);
            } catch (Exception screenshotError) {
                log.error("Failed to capture error screenshot: ", screenshotError);
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        result.setExecutionTimeMs(executionTime);

        return result;
    }

    private void executeOpenUrl(WebDriver driver, String url) {
        driver.get(url);
    }

    private void executeClick(WebDriver driver, String selector) {
        WebElement element = findElement(driver, selector);
        scrollToElement(driver, element);
        element.click();
    }

    private void executeType(WebDriver driver, String selector, String value) {
        WebElement element = findElement(driver, selector);
        scrollToElement(driver, element);
        element.clear();
        element.sendKeys(value);
    }

    private void executeSubmit(WebDriver driver, String selector) {
        WebElement element = findElement(driver, selector);
        element.submit();
    }

    private void executeWait(String seconds) throws InterruptedException {
        int waitTime = Integer.parseInt(seconds);
        Thread.sleep(waitTime * 1000L);
    }

    private void executeAssertText(WebDriver driver, String selector, String expectedText) {
        WebElement element = findElement(driver, selector);
        String actualText = element.getText();
        if (!actualText.contains(expectedText)) {
            throw new AssertionError(
                    String.format("Text mismatch. Expected: '%s', Actual: '%s'", expectedText, actualText));
        }
    }

    private void executeAssertElementPresent(WebDriver driver, String selector) {
        findElement(driver, selector);
    }

    private void executeScroll(WebDriver driver, String selector) {
        WebElement element = findElement(driver, selector);
        scrollToElement(driver, element);
    }

    private void executeSelectDropdown(WebDriver driver, String selector, String value) {
        WebElement element = findElement(driver, selector);
        Select select = new Select(element);
        select.selectByVisibleText(value);
    }

    private void executeClear(WebDriver driver, String selector) {
        WebElement element = findElement(driver, selector);
        element.clear();
    }

    private WebElement findElement(WebDriver driver, String selector) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        By locator = (selector.startsWith("//") || selector.startsWith("(")) ? By.xpath(selector)
                : By.cssSelector(selector);
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    private void scrollToElement(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult {
        private String action;
        private String selector;
        private boolean success;
        private String message;
        private String errorType;
        private String screenshotPath;
        private Long executionTimeMs;
    }
}

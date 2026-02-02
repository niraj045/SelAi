package com.ns.selai.execution.selenium;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Executes individual Selenium test steps
 * Maps AI-generated actions to Selenium commands
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StepExecutor {

    private final ScreenshotService screenshotService;

    /**
     * Execute a single test step
     */
    public StepResult executeStep(WebDriver driver, TestStep step, Long testRunId) {
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

            // Capture screenshot after successful execution
            String screenshotPath = screenshotService.captureScreenshot(driver, testRunId, step.getAction());
            result.setScreenshotPath(screenshotPath);

        } catch (Exception e) {
            log.error("Step execution failed: ", e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            result.setErrorType(e.getClass().getSimpleName());

            // Capture error screenshot
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

    /**
     * Open URL
     */
    private void executeOpenUrl(WebDriver driver, String url) {
        log.info("Opening URL: {}", url);
        driver.get(url);
    }

    /**
     * Click element
     */
    private void executeClick(WebDriver driver, String selector) {
        WebElement element = findElement(driver, selector);
        scrollToElement(driver, element);
        element.click();
        log.info("Clicked element: {}", selector);
    }

    /**
     * Type text into element
     */
    private void executeType(WebDriver driver, String selector, String value) {
        WebElement element = findElement(driver, selector);
        scrollToElement(driver, element);
        element.clear();
        element.sendKeys(value);
        log.info("Typed '{}' into element: {}", value, selector);
    }

    /**
     * Submit form
     */
    private void executeSubmit(WebDriver driver, String selector) {
        WebElement element = findElement(driver, selector);
        element.submit();
        log.info("Submitted form: {}", selector);
    }

    /**
     * Wait for specified time (in seconds)
     */
    private void executeWait(String seconds) throws InterruptedException {
        int waitTime = Integer.parseInt(seconds);
        Thread.sleep(waitTime * 1000L);
        log.info("Waited for {} seconds", waitTime);
    }

    /**
     * Assert text content
     */
    private void executeAssertText(WebDriver driver, String selector, String expectedText) {
        WebElement element = findElement(driver, selector);
        String actualText = element.getText();

        if (!actualText.contains(expectedText)) {
            throw new AssertionError(
                    String.format("Text mismatch. Expected: '%s', Actual: '%s'", expectedText, actualText));
        }
        log.info("Text assertion passed: {}", expectedText);
    }

    /**
     * Assert element is present
     */
    private void executeAssertElementPresent(WebDriver driver, String selector) {
        findElement(driver, selector);
        log.info("Element present: {}", selector);
    }

    /**
     * Scroll to element
     */
    private void executeScroll(WebDriver driver, String selector) {
        WebElement element = findElement(driver, selector);
        scrollToElement(driver, element);
        log.info("Scrolled to element: {}", selector);
    }

    /**
     * Select dropdown option
     */
    private void executeSelectDropdown(WebDriver driver, String selector, String value) {
        WebElement element = findElement(driver, selector);
        Select select = new Select(element);
        select.selectByVisibleText(value);
        log.info("Selected '{}' from dropdown: {}", value, selector);
    }

    /**
     * Clear input field
     */
    private void executeClear(WebDriver driver, String selector) {
        WebElement element = findElement(driver, selector);
        element.clear();
        log.info("Cleared element: {}", selector);
    }

    /**
     * Find element with wait (supports CSS and XPath)
     */
    private WebElement findElement(WebDriver driver, String selector) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        By locator;
        if (selector.startsWith("//") || selector.startsWith("(")) {
            // XPath
            locator = By.xpath(selector);
        } else {
            // CSS Selector
            locator = By.cssSelector(selector);
        }

        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Scroll element into view
     */
    private void scrollToElement(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        try {
            Thread.sleep(500); // Small delay after scroll
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Test Step model
     */
    public static class TestStep {
        private String action;
        private String selector;
        private String value;
        private String url;
        private String expectedText;

        // Getters and setters
        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getSelector() {
            return selector;
        }

        public void setSelector(String selector) {
            this.selector = selector;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getExpectedText() {
            return expectedText;
        }

        public void setExpectedText(String expectedText) {
            this.expectedText = expectedText;
        }
    }

    /**
     * Step Result model
     */
    public static class StepResult {
        private String action;
        private String selector;
        private boolean success;
        private String message;
        private String errorType;
        private String screenshotPath;
        private Long executionTimeMs;

        // Getters and setters
        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getSelector() {
            return selector;
        }

        public void setSelector(String selector) {
            this.selector = selector;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getErrorType() {
            return errorType;
        }

        public void setErrorType(String errorType) {
            this.errorType = errorType;
        }

        public String getScreenshotPath() {
            return screenshotPath;
        }

        public void setScreenshotPath(String screenshotPath) {
            this.screenshotPath = screenshotPath;
        }

        public Long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public void setExecutionTimeMs(Long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }
    }
}

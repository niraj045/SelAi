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
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
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

    public List<StepResult> executeSteps(List<ExecutionRequest.TestStep> steps) {
        return executeSteps(this.driver, steps, 0L);
    }

    public List<StepResult> executeSteps(WebDriver driver, List<ExecutionRequest.TestStep> steps, Long testRunId) {
        if (driver == null) {
            throw new IllegalStateException("WebDriver is not initialized");
        }

        List<StepResult> results = new ArrayList<>();
        for (ExecutionRequest.TestStep step : steps) {
            StepResult result = executeStep(driver, step, testRunId);
            results.add(result);

            if (!result.isSuccess()) {
                break;
            }
        }
        return results;
    }

    public StepResult executeStep(WebDriver driver, ExecutionRequest.TestStep step, Long testRunId) {
        log.info("Executing step: {} - {}", step.getAction(), step.getSelector());

        long startTime = System.currentTimeMillis();
        StepResult result = new StepResult();
        result.setAction(step.getAction());
        result.setSelector(step.getSelector());

        try {
            switch (normalizeAction(step.getAction())) {
                case "open":
                case "open_url":
                case "navigate":
                case "go_to":
                    executeOpenUrl(driver, step.getUrl());
                    break;
                case "click":
                    executeClick(driver, step.getSelector());
                    break;
                case "input":
                case "send_keys":
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
                case "assert_text_contains":
                    executeAssertText(driver, step.getSelector(), step.getExpectedText());
                    break;
                case "assert":
                    executeAssert(driver, step);
                    break;
                case "assert_element_present":
                case "wait_for_element":
                    executeAssertElementPresent(driver, step.getSelector());
                    break;
                case "assert_url_contains":
                    executeAssertUrlContains(driver, step.getValue());
                    break;
                case "scroll":
                    executeScroll(driver, step.getSelector());
                    break;
                case "select_dropdown":
                case "select":
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

        } catch (Exception | AssertionError e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
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
        requireText(url, "URL is required for open_url step");
        driver.get(url);
    }

    private void executeClick(WebDriver driver, String selector) {
        WebElement element = findElement(driver, selector);
        scrollToElement(driver, element);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            // Fixed headers/navbars can intercept clicks; use JS click as fallback
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
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
        int waitTime = StringUtils.hasText(seconds) ? Integer.parseInt(seconds) : 1;
        Thread.sleep(waitTime * 1000L);
    }

    private void executeAssertText(WebDriver driver, String selector, String expectedText) {
        requireText(expectedText, "Expected text is required for assert_text step");
        WebElement element = findElement(driver, selector);
        String actualText = element.getText();
        if (!actualText.contains(expectedText)) {
            throw new AssertionError(
                    String.format("Text mismatch. Expected: '%s', Actual: '%s'", expectedText, actualText));
        }
    }

    private void executeAssert(WebDriver driver, ExecutionRequest.TestStep step) {
        if (StringUtils.hasText(step.getExpectedText())) {
            executeAssertText(driver, step.getSelector(), step.getExpectedText());
            return;
        }

        if (StringUtils.hasText(step.getValue())) {
            executeAssertText(driver, step.getSelector(), step.getValue());
            return;
        }

        executeAssertElementPresent(driver, step.getSelector());
    }

    private void executeAssertUrlContains(WebDriver driver, String expectedValue) {
        requireText(expectedValue, "Expected value is required for assert_url_contains step");
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl == null || !currentUrl.contains(expectedValue)) {
            throw new AssertionError(
                    String.format("URL mismatch. Expected URL containing '%s', Actual: '%s'",
                            expectedValue, currentUrl));
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
        requireText(selector, "Selector is required for this step");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        By locator = resolveLocator(selector);
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    private By resolveLocator(String selector) {
        String trimmedSelector = selector.trim();

        if (trimmedSelector.startsWith("//") || trimmedSelector.startsWith("(")) {
            return By.xpath(trimmedSelector);
        }
        if (trimmedSelector.startsWith("xpath=")) {
            return By.xpath(trimmedSelector.substring("xpath=".length()));
        }
        if (trimmedSelector.startsWith("css=")) {
            return By.cssSelector(trimmedSelector.substring("css=".length()));
        }
        if (trimmedSelector.startsWith("id=")) {
            return By.id(trimmedSelector.substring("id=".length()));
        }
        if (trimmedSelector.startsWith("name=")) {
            return By.name(trimmedSelector.substring("name=".length()));
        }
        if (trimmedSelector.startsWith("linkText=")) {
            return By.linkText(trimmedSelector.substring("linkText=".length()));
        }
        if (trimmedSelector.startsWith("text=")) {
            String text = trimmedSelector.substring("text=".length()).replace("'", "\\'");
            return By.xpath("//*[contains(normalize-space(.), '" + text + "')]");
        }
        return By.cssSelector(trimmedSelector);
    }

    private void scrollToElement(WebDriver driver, WebElement element) {
        // Scroll element into view with 100px top offset to clear fixed headers/navbars
        ((JavascriptExecutor) driver).executeScript(
            "var rect = arguments[0].getBoundingClientRect();" +
            "var offset = rect.top + window.pageYOffset - 100;" +
            "window.scrollTo({top: offset, behavior: 'instant'});",
            element);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String normalizeAction(String action) {
        requireText(action, "Step action is required");
        return action.trim().toLowerCase();
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
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

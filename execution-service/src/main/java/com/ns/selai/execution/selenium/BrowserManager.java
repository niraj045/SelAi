package com.ns.selai.execution.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebDriver instances for test execution
 * - Creates browser instances
 * - Manages driver lifecycle
 * - Supports Chrome, Firefox, Edge
 * - Headless mode support
 */
@Component
@Slf4j
public class BrowserManager {

    private final Map<Long, WebDriver> activeDrivers = new ConcurrentHashMap<>();

    /**
     * Get or create WebDriver for a test run
     */
    public WebDriver getDriver(Long testRunId, String browser) {
        if (activeDrivers.containsKey(testRunId)) {
            return activeDrivers.get(testRunId);
        }

        WebDriver driver = createDriver(browser);
        activeDrivers.put(testRunId, driver);
        log.info("Created {} driver for test run: {}", browser, testRunId);

        return driver;
    }

    /**
     * Create a new WebDriver instance based on browser type
     */
    private WebDriver createDriver(String browser) {
        WebDriver driver;

        switch (browser.toLowerCase()) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized");
                chromeOptions.addArguments("--disable-notifications");
                chromeOptions.addArguments("--disable-popup-blocking");
                // Uncomment for headless mode in production
                // chromeOptions.addArguments("--headless=new");
                driver = new ChromeDriver(chromeOptions);
                break;

            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                // firefoxOptions.addArguments("--headless");
                driver = new FirefoxDriver(firefoxOptions);
                break;

            case "edge":
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                driver = new EdgeDriver(edgeOptions);
                break;

            default:
                log.warn("Unknown browser: {}, defaulting to Chrome", browser);
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver();
        }

        // Set timeouts
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));

        return driver;
    }

    /**
     * Close and remove driver for a test run
     */
    public void closeDriver(Long testRunId) {
        WebDriver driver = activeDrivers.remove(testRunId);
        if (driver != null) {
            try {
                driver.quit();
                log.info("Closed driver for test run: {}", testRunId);
            } catch (Exception e) {
                log.error("Error closing driver for test run {}: ", testRunId, e);
            }
        }
    }

    /**
     * Close all active drivers (cleanup)
     */
    public void closeAllDrivers() {
        log.info("Closing all active drivers: {}", activeDrivers.size());
        activeDrivers.forEach((testRunId, driver) -> {
            try {
                driver.quit();
            } catch (Exception e) {
                log.error("Error closing driver for test run {}: ", testRunId, e);
            }
        });
        activeDrivers.clear();
    }
}

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebDriver instances for test execution
 */
@Component
@Slf4j
public class BrowserManager {

    private final Map<Long, WebDriver> activeDrivers = new ConcurrentHashMap<>();

    @Value("${execution.browser.default:chrome}")
    private String defaultBrowser;

    @Value("${execution.browser.headless:true}")
    private boolean headless;

    @Value("${execution.browser.window.width:1920}")
    private int windowWidth;

    @Value("${execution.browser.window.height:1080}")
    private int windowHeight;

    @Value("${execution.browser.timeout.implicit:10}")
    private int implicitTimeoutSeconds;

    @Value("${execution.browser.timeout.page-load:30}")
    private int pageLoadTimeoutSeconds;

    @Value("${execution.browser.timeout.script:30}")
    private int scriptTimeoutSeconds;

    public WebDriver getWebDriver(String browser) {
        return createDriver(browser);
    }

    public void quitWebDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                log.error("Error closing driver: ", e);
            }
        }
    }

    public WebDriver getDriver(Long testRunId, String browser) {
        if (activeDrivers.containsKey(testRunId)) {
            return activeDrivers.get(testRunId);
        }
        WebDriver driver = createDriver(browser);
        activeDrivers.put(testRunId, driver);
        return driver;
    }

    private WebDriver createDriver(String browser) {
        String normalizedBrowser = StringUtils.hasText(browser) ? browser.toLowerCase() : defaultBrowser;
        WebDriver driver;

        switch (normalizedBrowser) {
            case "chrome":
                driver = createChromeDriver();
                break;
            case "firefox":
                driver = createFirefoxDriver();
                break;
            case "edge":
                driver = createEdgeDriver();
                break;
            default:
                log.warn("Unsupported browser '{}'. Falling back to Chrome", browser);
                driver = createChromeDriver();
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitTimeoutSeconds));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeoutSeconds));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(scriptTimeoutSeconds));
        return driver;
    }

    private WebDriver createChromeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-notifications");
        chromeOptions.addArguments("--disable-popup-blocking");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--window-size=" + windowWidth + "," + windowHeight);
        if (headless) {
            chromeOptions.addArguments("--headless=new");
        } else {
            chromeOptions.addArguments("--start-maximized");
        }
        return new ChromeDriver(chromeOptions);
    }

    private WebDriver createFirefoxDriver() {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        if (headless) {
            firefoxOptions.addArguments("--headless");
        }
        firefoxOptions.addArguments("--width=" + windowWidth);
        firefoxOptions.addArguments("--height=" + windowHeight);
        return new FirefoxDriver(firefoxOptions);
    }

    private WebDriver createEdgeDriver() {
        WebDriverManager.edgedriver().setup();
        EdgeOptions edgeOptions = new EdgeOptions();
        edgeOptions.addArguments("--disable-dev-shm-usage");
        edgeOptions.addArguments("--no-sandbox");
        edgeOptions.addArguments("--window-size=" + windowWidth + "," + windowHeight);
        if (headless) {
            edgeOptions.addArguments("--headless=new");
        }
        return new EdgeDriver(edgeOptions);
    }

    public void closeDriver(Long testRunId) {
        WebDriver driver = activeDrivers.remove(testRunId);
        if (driver != null) {
            quitWebDriver(driver);
        }
    }

    public void closeAllDrivers() {
        activeDrivers.forEach((testRunId, driver) -> quitWebDriver(driver));
        activeDrivers.clear();
    }
}

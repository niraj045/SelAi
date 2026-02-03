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
 */
@Component
@Slf4j
public class BrowserManager {

    private final Map<Long, WebDriver> activeDrivers = new ConcurrentHashMap<>();

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
        WebDriver driver;
        switch (browser.toLowerCase()) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized");
                chromeOptions.addArguments("--disable-notifications");
                chromeOptions.addArguments("--disable-popup-blocking");
                chromeOptions.addArguments("--headless=new");
                driver = new ChromeDriver(chromeOptions);
                break;
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.addArguments("--headless");
                driver = new FirefoxDriver(firefoxOptions);
                break;
            case "edge":
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--headless");
                driver = new EdgeDriver(edgeOptions);
                break;
            default:
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver();
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        return driver;
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

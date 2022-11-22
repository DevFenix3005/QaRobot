package com.rebirth.qarobot.scraping.enums;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public enum Browser {

    FIREFOX(FirefoxDriver.class, "webdriver.gecko.driver", "geckodriver.exe"),
    CHROME(ChromeDriver.class, "webdriver.chrome.driver", "chromedriver.exe"),
    EDGE(EdgeDriver.class, "", "");


    private final Class<? extends WebDriver> webdriver;
    private final String sysProperty;
    private final String path2WebDriver;

    Browser(Class<? extends WebDriver> webdriver, String sysProperty, String path2WebDriver) {
        this.webdriver = webdriver;
        this.sysProperty = sysProperty;
        this.path2WebDriver = path2WebDriver;
    }

    public Class<? extends WebDriver> getWebdriver() {
        return webdriver;
    }

    public String getSysProperty() {
        return sysProperty;
    }

    public String getPath2WebDriver() {
        return path2WebDriver;
    }

}

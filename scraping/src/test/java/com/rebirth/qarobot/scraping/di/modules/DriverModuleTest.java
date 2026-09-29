package com.rebirth.qarobot.scraping.di.modules;

import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.scraping.enums.Browser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DriverModuleTest {
    @TempDir
    Path drivers;

    @Test
    void missingLocalDriverAllowsAutomaticResolution() {
        Configuracion configuracion = new Configuracion();
        assertNull(DriverModule.configuredDriver(configuracion, Browser.CHROME));
        configuracion.setWebdriverHome(drivers.toFile());
        for (Browser browser : Browser.values()) {
            assertNull(DriverModule.configuredDriver(configuracion, browser));
        }
    }

    @Test
    void existingLocalDriverRemainsSupportedForEveryBrowser() throws Exception {
        Configuracion configuracion = new Configuracion();
        configuracion.setWebdriverHome(drivers.toFile());
        for (Browser browser : Browser.values()) {
            Path executable = Files.createFile(drivers.resolve(browser.getPath2WebDriver()));
            assertEquals(executable.toFile(), DriverModule.configuredDriver(configuracion, browser));
            assertFalse(browser.getSysProperty().isBlank());
        }
    }

    @Test
    void headlessOptionsAreOptInForEveryBrowser() {
        for (Browser browser : Browser.values()) {
            String capability = switch (browser) {
                case CHROME -> "goog:chromeOptions";
                case EDGE -> "ms:edgeOptions";
                case FIREFOX -> "moz:firefoxOptions";
            };
            assertTrue(arguments(DriverModule.browserOptions(browser, true), capability)
                    .stream().anyMatch(argument -> argument.contains("headless")));
            assertTrue(arguments(DriverModule.browserOptions(browser, false), capability)
                    .stream().noneMatch(argument -> argument.contains("headless")));
        }
    }

    @Test
    void configuredWaitUsesMillisecondsAndKeepsPolling() {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(), new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> { throw new AssertionError("Unexpected driver call"); });
        Configuracion configuracion = new Configuracion();
        configuracion.setBaseTimeout(20); // 100 ms total, as with the other timeout helpers.
        var wait = DriverModule.webDriverWaitProvider(driver, configuracion);
        int[] polls = {0};
        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                assertThrows(TimeoutException.class, () -> wait.until(unused -> {
                    polls[0]++;
                    return false;
                })));
        assertTrue(polls[0] >= 2, "The wait should poll again before timing out");
    }

    @SuppressWarnings("unchecked")
    private List<String> arguments(MutableCapabilities options, String capability) {
        Object value = options.getCapability(capability);
        if (!(value instanceof Map<?, ?> map)) return List.of();
        Object args = map.get("args");
        return args == null ? List.of() : (List<String>) args;
    }
}

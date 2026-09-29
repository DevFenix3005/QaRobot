package com.rebirth.qarobot.scraping.di.modules;

import com.rebirth.qarobot.scraping.enums.Browser;
import com.rebirth.qarobot.scraping.impl.SeleniumHelperImpl;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.scraping.SeleniumHelper;

import java.io.File;
import java.time.Duration;

@Module
public abstract class DriverModule {

    private DriverModule() {
    }

    @Provides
    @ChildComponent()
    public static WebDriver webDriverProvider(Configuracion configuracion, Browser browser) {
        File executable = configuredDriver(configuracion, browser);
        MutableCapabilities options = browserOptions(browser, Boolean.getBoolean("qarobot.headless"));
        // A system property supplied by the caller takes precedence over the legacy folder.
        if (System.getProperty(browser.getSysProperty()) != null) executable = null;
        return switch (browser) {
            case CHROME -> {
                ChromeDriverService.Builder service = new ChromeDriverService.Builder();
                if (executable != null) service.usingDriverExecutable(executable);
                yield new ChromeDriver(service.build(), (ChromeOptions) options);
            }
            case EDGE -> {
                EdgeDriverService.Builder service = new EdgeDriverService.Builder();
                if (executable != null) service.usingDriverExecutable(executable);
                yield new EdgeDriver(service.build(), (EdgeOptions) options);
            }
            case FIREFOX -> {
                GeckoDriverService.Builder service = new GeckoDriverService.Builder();
                if (executable != null) service.usingDriverExecutable(executable);
                yield new FirefoxDriver(service.build(), (FirefoxOptions) options);
            }
        };
    }

    static File configuredDriver(Configuracion configuracion, Browser browser) {
        if (configuracion.getWebdriverHome() == null) return null;
        File executable = new File(configuracion.getWebdriverHome(), browser.getPath2WebDriver());
        // With no local executable, Selenium Manager resolves a driver for the installed browser.
        return executable.isFile() ? executable : null;
    }

    static MutableCapabilities browserOptions(Browser browser, boolean headless) {
        return switch (browser) {
            case CHROME -> {
                ChromeOptions options = new ChromeOptions();
                if (headless) options.addArguments("--headless", "--window-size=1280,900");
                yield options;
            }
            case EDGE -> {
                EdgeOptions options = new EdgeOptions();
                if (headless) options.addArguments("--headless", "--window-size=1280,900");
                yield options;
            }
            case FIREFOX -> {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) options.addArguments("-headless", "--width=1280", "--height=900");
                yield options;
            }
        };
    }

    @Provides
    @ChildComponent()
    public static WebDriverWait webDriverWaitProvider(WebDriver webDriver, Configuracion configuracion) {
        return new WebDriverWait(webDriver,
                Duration.ofMillis(configuracion.timeout()),
                Duration.ofMillis(100)
        );
    }

    @Binds
    @ChildComponent()
    public abstract SeleniumHelper seleniumHelperBinds(SeleniumHelperImpl seleniumHelper);


}

package com.rebirth.qarobot.scraping.di.modules;

import com.rebirth.qarobot.scraping.enums.Browser;
import com.rebirth.qarobot.scraping.impl.SeleniumHelperImpl;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.scraping.SeleniumHelper;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Module
public abstract class DriverModule {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DriverModule.class);

    private DriverModule() {
    }

    @Provides
    @ChildComponent()
    public static WebDriver webDriverProvider(Configuracion configuracion, Browser browser) {
        System.setProperty(browser.getSysProperty(), configuracion.getWebdriverHome() + File.separator + browser.getPath2WebDriver());
        try {
            return browser.getWebdriver().getDeclaredConstructor().newInstance();
        } catch (InstantiationException |
                 IllegalAccessException |
                 InvocationTargetException |
                 NoSuchMethodException e) {
            log.error("FatalError!!!", e);
            System.exit(-1);
            return null;
        }
    }

    @Provides
    @ChildComponent()
    public static WebDriverWait webDriverWaitProvider(WebDriver webDriver, Configuracion configuracion) {
        return new WebDriverWait(webDriver,
                Duration.of(5, ChronoUnit.SECONDS),
                Duration.of(configuracion.timeout(), ChronoUnit.SECONDS)
        );
    }

    @Binds
    @ChildComponent()
    public abstract SeleniumHelper seleniumHelperBinds(SeleniumHelperImpl seleniumHelper);


}

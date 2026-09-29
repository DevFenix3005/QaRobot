package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.exceptions.NotFoundWebElement;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;
import com.rebirth.qarobot.commons.models.dtos.qarobot.*;
import com.rebirth.qarobot.scraping.di.modules.DriverModule;
import com.rebirth.qarobot.scraping.enums.Browser;
import com.rebirth.qarobot.scraping.impl.SeleniumHelperImpl;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Local dynamic pages exercise readiness without external services or fixed action delays. */
@Tag("browser")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserWaitTest {
    private WebDriver driver;
    private SeleniumHelperImpl helper;
    private QaRobotContext context;

    @BeforeAll
    void startBrowser() {
        Configuracion configuration = new Configuracion();
        configuration.setBaseTimeout(1000);
        Map<PatternEnum, Pattern> patterns = new EnumMap<>(PatternEnum.class);
        patterns.put(PatternEnum.NUMBER_PATTERN, Pattern.compile("^\\d+$"));
        patterns.put(PatternEnum.MONEY_PATTERN, Pattern.compile("^[0-9,.]+$"));
        patterns.put(PatternEnum.INTERPOLATION_PATTERN,
                Pattern.compile("^(?<interpolation>\\$\\{(?<value>[\\w\\-.]+)})$"));
        context = new QaRobotContext(patterns, new DecimalFormat());
        driver = DriverModule.webDriverProvider(configuration,
                Browser.valueOf(System.getProperty("qarobot.browser", "CHROME").toUpperCase(Locale.ROOT)));
        helper = new SeleniumHelperImpl(driver, DriverModule.webDriverWaitProvider(driver, configuration),
                configuration, context, new Random(1), patterns);
    }

    @AfterAll
    void closeBrowser() {
        if (driver != null) driver.quit();
        if (context != null) context.shuwdownExecutor();
    }

    @Test
    void clickWaitsForInsertionVisibilityAndEnabledStateAndExecutesOnce() {
        page("<main id='container'></main>");
        script("""
                window.clicks = 0;
                setTimeout(() => {
                    container.innerHTML = '<button id="save" disabled style="display:none">Save</button>';
                    save.onclick = () => window.clicks++;
                }, 150);
                setTimeout(() => save.style.display = 'block', 350);
                setTimeout(() => save.disabled = false, 600);
                """);
        ClickActionType click = new ClickActionType();
        click.getSelector().add(selector("save"));
        click.setWaitTimeout(BigInteger.valueOf(2500));

        run(new ClickAction(helper), click);

        assertEquals(1L, script("return window.clicks"));
        assertEquals("ID: save", helper.getLastResolvedSelector());
        assertSame(click, helper.getCurrentAction(), "Nested actions must retain their own failure context");
    }

    @Test
    void writeWaitsUntilEditableAndReadWaitsUntilVisible() {
        page("<input id='name' disabled readonly style='display:none'><output id='result' style='display:none'>Ready</output>");
        script("""
                setTimeout(() => document.getElementById('name').style.display = 'block', 100);
                setTimeout(() => document.getElementById('name').disabled = false, 250);
                setTimeout(() => document.getElementById('name').removeAttribute('readonly'), 500);
                """);
        WriteActionType write = new WriteActionType();
        write.getSelector().add(selector("name"));
        write.setWaitTimeout(BigInteger.valueOf(2500));
        write.setValue("QaRobot");
        write.setEnter(false);
        run(new WriteAction(helper), write);
        assertEquals("QaRobot", driver.findElement(By.id("name")).getDomProperty("value"));

        script("setTimeout(() => document.getElementById('result').style.display = 'block', 400)");
        ReadActionType read = new ReadActionType();
        read.getSelector().add(selector("result"));
        read.setSet("visibleResult");
        read.setWaitTimeout(BigInteger.valueOf(2500));
        run(new ReadAction(helper), read);
        assertEquals("Ready", helper.getValueFormContext("visibleResult"));
    }

    @Test
    void chooseWaitsForEnabledControlAndDelayedEnabledOption() {
        page("<select id='environment' disabled><option>Choose</option></select>");
        script("""
                window.changes = 0;
                environment.onchange = () => window.changes++;
                setTimeout(() => environment.disabled = false, 100);
                setTimeout(() => environment.insertAdjacentHTML('beforeend', '<option disabled id="target">Pruebas</option>'), 250);
                setTimeout(() => target.disabled = false, 500);
                """);
        ChooseActionType choose = new ChooseActionType();
        choose.getSelector().add(selector("environment"));
        choose.setValue("Pruebas");
        choose.setWaitTimeout(BigInteger.valueOf(2500));
        run(new ChooseAction(helper), choose);
        assertEquals("Pruebas", driver.findElement(By.id("environment")).getDomProperty("value"));
        assertEquals(1L, script("return window.changes"));
    }

    @Test
    void fallbackSelectorsShareADeadlineAndReturnAsSoonAsOneIsReady() {
        page("<button id='hidden' style='display:none'>Hidden</button><button id='ready'>Ready</button>");
        ClickActionType click = new ClickActionType();
        click.getSelector().add(selector("missing"));
        click.getSelector().add(selector("hidden"));
        click.getSelector().add(selector("ready"));
        click.setWaitTimeout(BigInteger.valueOf(2000));
        long start = System.nanoTime();
        run(new ClickAction(helper), click);
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        assertTrue(elapsed < 1500, "An available fallback should not wait for earlier selectors: " + elapsed + " ms");
        assertEquals("ID: ready", helper.getLastResolvedSelector());
    }

    @Test
    void actionTimeoutOverridesDefaultAndReportsReadinessAndAllSelectors() {
        page("<button id='disabled' disabled>Disabled</button>");
        ClickActionType click = new ClickActionType();
        click.setId("bounded-click");
        click.getSelector().add(selector("first-missing"));
        click.getSelector().add(selector("disabled"));
        click.getSelector().add(selector("last-missing"));
        click.setWaitTimeout(BigInteger.valueOf(350));
        long start = System.nanoTime();
        NotFoundWebElement failure = assertThrows(NotFoundWebElement.class, () -> run(new ClickAction(helper), click));
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        assertTrue(elapsed >= 300 && elapsed < 1800, "One 350 ms budget should cover all selectors: " + elapsed + " ms");
        assertInstanceOf(TimeoutException.class, failure.getCause());
        assertTrue(failure.getMessage().contains("350 ms"));
        assertTrue(failure.getMessage().contains("CLICKABLE"));
        assertTrue(failure.getMessage().contains("first-missing"));
        assertTrue(failure.getMessage().contains("last-missing"));
        assertEquals("ID: disabled", helper.getLastResolvedSelector());
    }

    @Test
    void verificationCanInspectHiddenElementsAndAcceptMissingElementsInNegativeTests() {
        page("<span id='hidden' style='display:none'>Hidden</span>");
        int initialCount = context.getVerificadores().size();
        VerifyActionType hidden = new VerifyActionType();
        hidden.getSelector().add(selector("hidden"));
        hidden.setElementStatus("DISPLAYED");
        hidden.setNegative(true);
        hidden.setWaitTimeout(BigInteger.ZERO);
        run(new VerifyAction(helper, null, new DecimalFormat()), hidden);

        VerifyActionType absent = new VerifyActionType();
        absent.getSelector().add(selector("absent"));
        absent.setNegative(true);
        absent.setWaitTimeout(BigInteger.ZERO);
        run(new VerifyAction(helper, null, new DecimalFormat()), absent);

        assertEquals(initialCount + 2, context.getVerificadores().size());
        assertTrue(context.getVerificadores().get(initialCount).isOk());
        assertTrue(context.getVerificadores().get(initialCount + 1).isOk());
        assertNull(helper.getLastResolvedSelector(), "A failed lookup must not reuse the previous action's selector");
    }

    @Test
    void clickInterceptionFailsInsteadOfBeingReportedAsSuccess() {
        page("""
                <button id='save' style='position:absolute;top:40px;left:40px'>Save</button>
                <div style='position:fixed;inset:0;z-index:10;background:white'>Blocking overlay</div>
                """);
        script("window.clicks = 0; save.onclick = () => window.clicks++");
        ClickActionType click = new ClickActionType();
        click.getSelector().add(selector("save"));
        click.setWaitTimeout(BigInteger.valueOf(500));
        assertThrows(ElementClickInterceptedException.class, () -> run(new ClickAction(helper), click));
        assertEquals(0L, script("return window.clicks"));
    }

    @Test
    void legacyTimeoutRemainsAnExplicitPreActionDelay() {
        page("<button id='ready'>Ready</button>");
        ClickActionType click = new ClickActionType();
        click.getSelector().add(selector("ready"));
        click.setTimeout(BigInteger.valueOf(300));
        click.setWaitTimeout(BigInteger.ZERO);
        long start = System.nanoTime();
        run(new ClickAction(helper), click);
        assertTrue((System.nanoTime() - start) / 1_000_000 >= 300);
    }

    private void page(String body) {
        String html = "<!DOCTYPE html><meta charset='utf-8'><body>" + body + "</body>";
        driver.get("data:text/html;base64," + Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8)));
    }

    private Object script(String script) {
        return ((JavascriptExecutor) driver).executeScript(script);
    }

    private SelectorType selector(String id) {
        SelectorType selector = new SelectorType();
        selector.setBy(KindOfBy.ID);
        selector.setValue(id);
        return selector;
    }

    private <T extends BaseActionTypeWithTimeout> void run(Action<T> action, T dto) {
        if (dto.getId() == null) dto.setId(dto.getClass().getSimpleName());
        dto.setDesc("Dynamic browser readiness test");
        dto.setOrder(1);
        if (dto.getTimeout() == null) dto.setTimeout(BigInteger.ZERO);
        dto.setIterationChild(true);
        action.setAction(dto);
        action.run();
    }
}

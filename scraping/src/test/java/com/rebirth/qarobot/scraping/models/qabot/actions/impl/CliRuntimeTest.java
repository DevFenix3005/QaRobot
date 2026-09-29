package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.exceptions.StopActionException;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ClickActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.MessageActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ScriptActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.StopActionType;
import com.rebirth.qarobot.commons.utils.ShowInDialog;
import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.impl.SeleniumHelperImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class CliRuntimeTest {
    private String previousCli;
    private String previousHeadless;
    private QaRobotContext context;
    private SeleniumHelperImpl helper;
    private ScriptEngine engine;

    @BeforeEach
    void prepareCli() {
        previousCli = System.getProperty("qarobot.cli");
        previousHeadless = System.getProperty("qarobot.headless");
        System.setProperty("qarobot.cli", "true");
        // CLI must also work with a visible browser, without manipulating its position.
        System.setProperty("qarobot.headless", "false");
        Map<PatternEnum, Pattern> patterns = Map.of(
                PatternEnum.NUMBER_PATTERN, Pattern.compile("^\\d+$"),
                PatternEnum.MONEY_PATTERN, Pattern.compile("^[0-9,.]+$"));
        context = new QaRobotContext(patterns, new DecimalFormat());
        Configuracion configuration = new Configuracion();
        configuration.setBaseTimeout(100);
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                    throw new AssertionError("CLI logging must not access the browser: " + method.getName());
                });
        helper = new SeleniumHelperImpl(driver, new WebDriverWait(driver, Duration.ofSeconds(1)),
                configuration, context, new Random(1), patterns);
    }

    @AfterEach
    void cleanup() throws Exception {
        try {
            if (engine instanceof AutoCloseable closeable) closeable.close();
        } finally {
            try {
                if (context != null) context.shuwdownExecutor();
            } finally {
                restoreProperty("qarobot.cli", previousCli);
                restoreProperty("qarobot.headless", previousHeadless);
            }
        }
    }

    @Test
    void actionLoggingSkipsBrowserPositionDialogsAndTheirDelay() {
        helper.setShowInDialog(new ShowInDialog() {
            @Override
            public void run(TitleIconAndMsgPojo info) {
                fail("CLI must not open a progress dialog");
            }

            @Override
            public void hideDialog() {
                fail("CLI must not access a progress dialog");
            }
        });
        ClickActionType action = new ClickActionType();
        action.setId("click-from-terminal");
        action.setDesc("Terminal progress");
        action.setOrder(1L);

        assertTimeout(Duration.ofSeconds(2), () -> helper.actionLog(action));
        assertSame(action, helper.getCurrentAction());
    }

    @Test
    void contextNotificationsDoNotRequireAGuiCallback() {
        assertDoesNotThrow(helper::sendQaContet2View);
    }

    @Test
    void messagesDoNotRequestPositionDialogOrSleep() {
        SeleniumHelper strictHelper = (SeleniumHelper) Proxy.newProxyInstance(
                SeleniumHelper.class.getClassLoader(), new Class<?>[]{SeleniumHelper.class},
                (proxy, method, args) -> {
                    throw new AssertionError("CLI message must only log its text: " + method.getName());
                });
        MessageAction action = new MessageAction(strictHelper);
        MessageActionType message = new MessageActionType();
        message.setMsg("Running from terminal");
        message.setDesc("No interactive dialog");
        message.setTimeout(BigInteger.valueOf(60000));
        action.setAction(message);

        assertDoesNotThrow(action::execute);
    }

    @Test
    void interactiveStopFailsInsteadOfPausingTheExecutor() {
        helper.setPuaseExecutionFromStopAction(() -> fail("CLI must never enter a GUI pause"));
        StopAction action = new StopAction(helper);
        StopActionType stop = new StopActionType();
        stop.setKill(false);
        action.setAction(stop);

        IllegalStateException failure = assertTimeout(Duration.ofSeconds(2),
                () -> assertThrows(IllegalStateException.class, action::execute));
        assertTrue(failure.getMessage().contains("kill=false"));
        assertTrue(failure.getMessage().contains("GUI"));
    }

    @Test
    void explicitStopRetainsSuccessfulControlFlow() {
        StopAction action = new StopAction(helper);
        StopActionType stop = new StopActionType();
        stop.setKill(true);
        action.setAction(stop);

        StopActionException stopSignal = assertThrows(StopActionException.class, action::execute);
        assertSame(stop, stopSignal.getStopActionType());
    }

    @Test
    void scriptErrorsReachTheRunnerWithTheirOriginalCause() {
        ScriptAction action = script("broken-script", "throw new Error('CLI script failure');");

        IllegalStateException failure = assertThrows(IllegalStateException.class, action::execute);

        assertTrue(failure.getMessage().contains("broken-script"));
        ScriptException cause = assertInstanceOf(ScriptException.class, failure.getCause());
        assertTrue(cause.getMessage().contains("CLI script failure"));
    }

    @Test
    void scriptsWithoutASetAttributeDoNotNeedToReturnAValue() throws Exception {
        ScriptAction action = script("side-effect-only", "globalThis.cliProof = 42;");

        assertDoesNotThrow(action::execute);

        assertEquals(42, ((Number) engine.eval("globalThis.cliProof")).intValue());
        assertTrue(helper.getContextMap().isEmpty());
    }

    private ScriptAction script(String id, String body) {
        engine = new ScriptEngineManager(Thread.currentThread().getContextClassLoader())
                .getEngineByName("graal.js");
        assertNotNull(engine, "GraalJS must be available in the application runtime");
        ScriptAction action = new ScriptAction(helper, engine);
        ScriptActionType script = new ScriptActionType();
        script.setId(id);
        script.setBody(body);
        action.setAction(script);
        return action;
    }

    private static void restoreProperty(String key, String previous) {
        if (previous == null) System.clearProperty(key);
        else System.setProperty(key, previous);
    }
}

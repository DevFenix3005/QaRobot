package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;
import com.rebirth.qarobot.commons.models.dtos.qarobot.*;
import com.rebirth.qarobot.scraping.di.modules.DriverModule;
import com.rebirth.qarobot.scraping.enums.Browser;
import com.rebirth.qarobot.scraping.impl.SeleniumHelperImpl;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Explicitly opt in with :scraping:browserSmokeTest; the normal test task needs no browser. */
@Tag("browser")
class BrowserSmokeTest {
    @Test
    void runsQaRobotActionsAgainstALocalPage() throws Exception {
        Browser browser = Browser.valueOf(System.getProperty("qarobot.browser", "CHROME")
                .toUpperCase(Locale.ROOT));
        Configuracion configuracion = new Configuracion();
        configuracion.setBaseTimeout(1000);
        Map<PatternEnum, Pattern> patterns = patterns();
        QaRobotContext context = new QaRobotContext(patterns, new DecimalFormat());
        WebDriver driver = DriverModule.webDriverProvider(configuracion, browser);
        try {
            SeleniumHelperImpl helper = new SeleniumHelperImpl(driver,
                    DriverModule.webDriverWaitProvider(driver, configuracion), configuracion,
                    context, new Random(1), patterns);

            OpenActionType open = new OpenActionType();
            open.setUrl(Objects.requireNonNull(getClass().getResource("/browser-smoke.html"))
                    .toURI().toString());
            run(new OpenAction(helper), open);

            WriteActionType write = new WriteActionType();
            write.getSelector().add(selector(KindOfBy.ID, "name"));
            write.setValue("QaRobot");
            write.setEnter(false);
            run(new WriteAction(helper), write);

            ChooseActionType choose = new ChooseActionType();
            choose.getSelector().add(selector(KindOfBy.CSS, "#environment"));
            choose.setValue("Pruebas");
            run(new ChooseAction(helper), choose);

            ClickActionType click = new ClickActionType();
            click.getSelector().add(selector(KindOfBy.XPATH, "//button[@id='submit']"));
            run(new ClickAction(helper), click);

            ReadActionType read = new ReadActionType();
            read.getSelector().add(selector(KindOfBy.ID, "result"));
            read.setSet("result");
            run(new ReadAction(helper), read);
            assertEquals("QaRobot - Pruebas", helper.getValueFormContext("result"));

            VerifyActionType verify = new VerifyActionType();
            verify.getSelector().add(selector(KindOfBy.ID, "result"));
            verify.setValue("${result}");
            run(new VerifyAction(helper, null, new DecimalFormat()), verify);
            assertEquals(1, context.getVerificadores().size());
            assertTrue(helper.verificacionesOk());

            assertEquals(2, helper.getWebElements("items", List.of(
                    selector(KindOfBy.CSS, "#items li"), selector(KindOfBy.CSS, "button"))).size(),
                    "Alternative selectors must stop at the first match");
            helper.closeDriver();
        } finally {
            // Also release the session when an action or assertion fails.
            driver.quit();
            context.shuwdownExecutor();
        }
    }

    private <T extends BaseActionTypeWithTimeout> void run(Action<T> action, T dto) {
        dto.setId(dto.getClass().getSimpleName());
        dto.setDesc("Browser smoke test");
        dto.setOrder(1);
        dto.setTimeout(BigInteger.ZERO);
        dto.setIterationChild(true); // No desktop dialogs during this headless test.
        action.setAction(dto);
        action.run();
    }

    private SelectorType selector(KindOfBy by, String value) {
        SelectorType selector = new SelectorType();
        selector.setBy(by);
        selector.setValue(value);
        return selector;
    }

    private Map<PatternEnum, Pattern> patterns() {
        Map<PatternEnum, Pattern> patterns = new EnumMap<>(PatternEnum.class);
        patterns.put(PatternEnum.NUMBER_PATTERN, Pattern.compile("^\\d+$"));
        patterns.put(PatternEnum.MONEY_PATTERN, Pattern.compile("^[0-9,.]+$"));
        patterns.put(PatternEnum.ATTRIBUTE_PATTERN,
                Pattern.compile("^(?<label>attr):(?<key>[\\w\\-.]+)((?<eqq>([=@]))(?<value>.+))?$"));
        patterns.put(PatternEnum.INTERPOLATION_PATTERN,
                Pattern.compile("^(?<interpolation>\\$\\{(?<value>[\\w\\-.]+)})$"));
        patterns.put(PatternEnum.VERIFYELEMENT_PATTERN,
                Pattern.compile("^(?<label>attr|text)(?<attribute>:[\\w\\-.]+)?(?<eqq>=)(?<logic>eq|contain|gt|lt|ge|le|noempty)(?<payload>(?<dot>\\.)(?<content>[{}$\\w-.]+))?$"));
        return patterns;
    }
}

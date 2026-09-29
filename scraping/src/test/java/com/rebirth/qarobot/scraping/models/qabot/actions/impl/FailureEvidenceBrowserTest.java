package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.FailureEvidence;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;
import com.rebirth.qarobot.commons.models.dtos.QarobotWrapper;
import com.rebirth.qarobot.commons.models.dtos.qarobot.*;
import com.rebirth.qarobot.scraping.di.modules.DriverModule;
import com.rebirth.qarobot.scraping.enums.Browser;
import com.rebirth.qarobot.scraping.impl.QaRobotXmlImpl;
import com.rebirth.qarobot.scraping.impl.SeleniumHelperImpl;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import javax.imageio.ImageIO;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@Tag("browser")
class FailureEvidenceBrowserTest {
    @TempDir Path temp;

    @Test
    void capturesFailedVerificationBeforeLaterNavigationAndMarksTheRunFailed() throws Exception {
        Path first = Files.writeString(temp.resolve("first.html"),
                "<meta charset='utf-8'><h1 id='result'>Obtenido ñ</h1>");
        Path second = Files.writeString(temp.resolve("second.html"), "<h1>Next page</h1>");
        VerifyActionType verify = action(new VerifyActionType(), "check-result");
        verify.setElementStatus("CONTENT");
        verify.setValue("Esperado ñ");
        verify.getSelector().add(selector("result"));
        try (Run run = new Run(List.of(open(first), verify, open(second)))) {
            assertFalse(run.robot.flux(), "A failed assertion must fail the complete run");
            assertTrue(run.driver.getCurrentUrl().endsWith("second.html"), "Assertions still allow later actions");
            FailureEvidence evidence = run.helper.getFailureEvidence().get(0);
            assertTrue(evidence.getUrl().endsWith("first.html"), "Capture the page at the failed assertion");
            assertEquals("check-result", evidence.getActionId());
            assertTrue(evidence.getSelectorUsed().contains("result"));
            assertEquals("Obtenido ñ", evidence.getVerifications().get(0).getEvaluado());
            assertEquals("Esperado ñ", evidence.getVerifications().get(0).getResultado());
            assertValidScreenshot(run.output.resolve(evidence.getScreenshotPath()));
            String report = Files.readString(run.output.resolve("index.html"));
            assertTrue(report.contains("check-result"));
            assertTrue(report.contains(evidence.getScreenshotPath()));
            assertTrue(report.contains("Obtenido ñ"), "Report charset must match its UTF-8 declaration");
            assertTrue(report.contains("Esperado ñ"));
            assertTrue(Files.readString(run.output.resolve(evidence.getDetailsPath())).contains("Esperado ñ"));

            // Keep a concrete sample and render it with the same real browser used by the test.
            Path preview = Path.of("build/reports/failure-evidence-preview").toAbsolutePath();
            Files.createDirectories(preview.resolve("evidence"));
            for (String relative : List.of("index.html", evidence.getScreenshotPath(), evidence.getDetailsPath())) {
                Files.copy(run.output.resolve(relative), preview.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
            }
            try (var assets = Files.list(run.output)) {
                for (Path asset : assets.filter(path -> path.toString().endsWith(".png")).toList()) {
                    Files.copy(asset, preview.resolve(asset.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            run.driver.get(preview.resolve("index.html").toUri().toString());
            var screenshot = run.driver.findElement(By.cssSelector("img[src='" + evidence.getScreenshotPath() + "']"));
            assertTrue(screenshot.isDisplayed());
            assertEquals(Boolean.TRUE, ((JavascriptExecutor) run.driver).executeScript(
                    "return arguments[0].complete && arguments[0].naturalWidth > 0", screenshot));
            assertEquals(Boolean.TRUE, ((JavascriptExecutor) run.driver).executeScript(
                    "return document.documentElement.scrollWidth <= window.innerWidth"), "Report must fit the viewport");
            ((JavascriptExecutor) run.driver).executeScript(
                    "document.getElementById('failure-evidence-title').scrollIntoView()");
            Files.write(preview.resolve("report-preview.png"), ((TakesScreenshot) run.driver).getScreenshotAs(OutputType.BYTES));
        }
    }

    @Test
    void capturesTheNestedActionWhenAnIterationThrows() throws Exception {
        Path page = Files.writeString(temp.resolve("page.html"), "<h1>Missing button</h1>");
        ClickActionType missing = action(new ClickActionType(), "missing-child");
        missing.setWaitTimeout(BigInteger.valueOf(150));
        missing.getSelector().add(selector("missing"));
        IterationActionType iteration = action(new IterationActionType(), "outer-iteration");
        iteration.setTimes("1");
        iteration.getOpenOrDelayOrStop().add(missing);
        try (Run run = new Run(List.of(open(page), iteration))) {
            assertFalse(run.robot.flux());
            assertEquals(1, run.helper.getFailureEvidence().size(), "Do not duplicate propagated failures");
            FailureEvidence evidence = run.helper.getFailureEvidence().get(0);
            assertEquals("missing-child", evidence.getActionId());
            assertTrue(evidence.getSelectors().get(0).contains("missing"));
            assertValidScreenshot(run.output.resolve(evidence.getScreenshotPath()));
            assertTrue(Files.readString(run.output.resolve(evidence.getDetailsPath())).contains("NotFoundWebElement"));
            assertTrue(Files.readString(run.output.resolve("index.html")).contains(evidence.getScreenshotPath()));
        }
    }

    @Test
    void successfulAndSkippedChecksDoNotCreateEvidence() throws Exception {
        Path page = Files.writeString(temp.resolve("success.html"), "<h1 id='result'>OK</h1>");
        VerifyActionType verify = action(new VerifyActionType(), "success");
        verify.setElementStatus("CONTENT");
        verify.setValue("OK");
        verify.getSelector().add(selector("result"));
        VerifyActionType skipped = action(new VerifyActionType(), "skipped");
        skipped.setSkip(true);
        try (Run run = new Run(List.of(open(page), verify, skipped))) {
            assertTrue(run.robot.flux());
            assertTrue(run.helper.getFailureEvidence().isEmpty());
            assertFalse(Files.exists(run.output.resolve("evidence")));
            assertTrue(Files.isRegularFile(run.output.resolve("index.html")));
        }
    }

    private void assertValidScreenshot(Path screenshot) throws Exception {
        assertTrue(Files.size(screenshot) > 0);
        var image = ImageIO.read(screenshot.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() > 0 && image.getHeight() > 0);
    }

    private OpenActionType open(Path page) {
        OpenActionType open = action(new OpenActionType(), page.getFileName().toString());
        open.setUrl(page.toUri().toString());
        return open;
    }

    private <T extends BaseActionTypeWithTimeout> T action(T action, String id) {
        action.setId(id);
        action.setDesc("Evidence test: " + id);
        action.setOrder(1);
        action.setTimeout(BigInteger.ZERO);
        action.setIterationChild(true);
        return action;
    }

    private SelectorType selector(String id) {
        SelectorType selector = new SelectorType();
        selector.setBy(KindOfBy.ID);
        selector.setValue(id);
        return selector;
    }

    private class Run implements AutoCloseable {
        final Path output = temp.resolve("report");
        final WebDriver driver;
        final SeleniumHelperImpl helper;
        final QaRobotXmlImpl robot;

        Run(List<BaseActionType> actions) throws Exception {
            Configuracion settings = new Configuracion();
            settings.setBaseTimeout(200);
            settings.setDashboardtemplateHome(Path.of("../app/dashboardtemplate").toAbsolutePath().toFile());
            Map<PatternEnum, Pattern> patterns = new EnumMap<>(PatternEnum.class);
            patterns.put(PatternEnum.NUMBER_PATTERN, Pattern.compile("^\\d+$"));
            patterns.put(PatternEnum.MONEY_PATTERN, Pattern.compile("^[0-9,.]+$"));
            patterns.put(PatternEnum.ATTRIBUTE_PATTERN, Pattern.compile("a^"));
            patterns.put(PatternEnum.VERIFYELEMENT_PATTERN, Pattern.compile("a^"));
            patterns.put(PatternEnum.INTERPOLATION_PATTERN, Pattern.compile("^\\$\\{(?<value>[\\w.-]+)}$"));
            QaRobotContext context = new QaRobotContext(patterns, new DecimalFormat());
            driver = DriverModule.webDriverProvider(settings, Browser.valueOf(
                    System.getProperty("qarobot.browser", "CHROME").toUpperCase(Locale.ROOT)));
            helper = new SeleniumHelperImpl(driver, DriverModule.webDriverWaitProvider(driver, settings),
                    settings, context, new Random(1), patterns);
            helper.setSendQaContext2View(ignored -> {});
            Map<Class<? extends BaseActionType>, Action<? extends BaseActionType>> actionMap = new java.util.HashMap<>();
            actionMap.put(OpenActionType.class, new OpenAction(helper));
            actionMap.put(VerifyActionType.class, new VerifyAction(helper, null, new DecimalFormat()));
            actionMap.put(ClickActionType.class, new ClickAction(helper));
            actionMap.put(IterationActionType.class, new IterationAction(helper, () -> actionMap));
            Configuration freemarker = new Configuration(Configuration.VERSION_2_3_29);
            freemarker.setDirectoryForTemplateLoading(settings.getDashboardtemplateHome());
            freemarker.setDefaultEncoding("ISO-8859-1");
            freemarker.setOutputFormat(HTMLOutputFormat.INSTANCE);
            robot = new QaRobotXmlImpl(helper, actionMap, freemarker.getTemplate("index.ftl"), settings);
            QarobotWrapper wrapper = new QarobotWrapper();
            wrapper.setQarobot(new Qarobot());
            wrapper.setConfiguration(new ConfigurationType());
            wrapper.getActions().addAll(actions);
            wrapper.setXmlFile(temp.resolve("case.xml").toFile());
            wrapper.setXmlTempFile(Files.writeString(temp.resolve("expanded.xml"), "<qarobot/>").toFile());
            wrapper.setDashboardExitFile(output.toFile());
            robot.setQaRobot(wrapper);
        }

        @Override
        public void close() {
            robot.close();
            helper.shutdownExecutor();
        }
    }
}

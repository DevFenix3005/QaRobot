package com.rebirth.qarobot.app.cli;

import com.rebirth.qarobot.app.di.modules.AppModule;
import com.rebirth.qarobot.app.di.modules.PatternsModule;
import com.rebirth.qarobot.app.di.modules.XmlReaderModule;
import com.rebirth.qarobot.app.utils.QaXmlReadService;
import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.QarobotWrapper;
import com.rebirth.qarobot.scraping.enums.Browser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class XmlScenarioExecutorTest {
    @TempDir Path temp;

    @Test
    void invalidXmlProducesErrorWithoutInitializingBrowser() throws Exception {
        Path scenario = Files.writeString(temp.resolve("invalid.xml"), """
                <qarobot xmlns="https://www.qarobot.rebirth.com.mx">
                  <configuration/>
                  <unknown order="1" desc="Invalid action"/>
                </qarobot>
                """);
        var executor = new XmlScenarioExecutor(reader(), browser -> {
            throw new AssertionError("Invalid XML must be rejected before creating a browser");
        }, Browser.CHROME);
        ScenarioResult result = executor.execute(scenario, temp);
        assertEquals(ScenarioResult.Status.ERROR, result.status());
        assertTrue(result.message().contains("XML inválido"));
        assertNull(result.report());
    }

    @Test
    void parsedTemporaryXmlIsDeletedEvenWhenValidationFails() throws Exception {
        Path temporary = Files.writeString(temp.resolve("expanded.xml"), "<invalid/>");
        QarobotWrapper wrapper = new QarobotWrapper();
        wrapper.setValidXml(false);
        wrapper.setXmlTempFile(temporary.toFile());
        var reader = new QaXmlReadService(null, null, null, null, Map.of()) {
            @Override
            public QarobotWrapper read() {
                return wrapper;
            }
        };
        var executor = new XmlScenarioExecutor(reader, browser -> {
            throw new AssertionError("Invalid XML must be rejected before creating a browser");
        }, Browser.CHROME);
        assertEquals(ScenarioResult.Status.ERROR, executor.execute(temp.resolve("input.xml"), temp).status());
        assertFalse(Files.exists(temporary));
    }

    @Test
    void temporaryCleanupFailurePreservesOriginalDiagnosticAndReport() throws Exception {
        Path temporary = Files.createDirectory(temp.resolve("cannot-delete-nonempty"));
        Files.writeString(temporary.resolve("child"), "keep");
        Path report = Files.writeString(temp.resolve("index.html"), "original report");
        QarobotWrapper wrapper = new QarobotWrapper();
        wrapper.setValidXml(false);
        wrapper.addError("Original schema validation error");
        wrapper.setXmlTempFile(temporary.toFile());
        var reader = new QaXmlReadService(null, null, null, null, Map.of()) {
            @Override
            public QarobotWrapper read() {
                return wrapper;
            }
        };
        var executor = new XmlScenarioExecutor(reader, browser -> {
            throw new AssertionError("Invalid XML must be rejected before creating a browser");
        }, Browser.CHROME);
        ScenarioResult result = executor.execute(temp.resolve("input.xml"), temp);
        assertEquals(ScenarioResult.Status.ERROR, result.status());
        assertTrue(result.message().contains("Original schema validation error"));
        assertEquals(report, result.report());
        assertTrue(Files.exists(temporary));
    }

    @Test
    void xmlReaderPreservesUnicodeInValuesAndExpandedReportSource() throws Exception {
        Path scenario = Files.writeString(temp.resolve("unicode.xml"), """
                <qarobot xmlns="https://www.qarobot.rebirth.com.mx">
                  <configuration><set key="mensaje" value="Revisión 日本語 😀"/></configuration>
                  <delay desc="Pausa de revisión 日本語" order="1" timeout="0"/>
                </qarobot>
                """);
        var reader = reader();
        reader.setQaXmlFile(scenario.toFile());
        var wrapper = reader.read();
        try {
            assertTrue(wrapper.isValidXml(), () -> String.valueOf(wrapper.getErrores()));
            assertEquals("Revisión 日本語 😀", wrapper.getConfiguration().getSet().get(0).getValue());
            String expanded = Files.readString(wrapper.getXmlTempFile().toPath());
            assertTrue(expanded.contains("encoding=\"UTF-8\""));
            assertTrue(expanded.contains("Revisión 日本語"));
        } finally {
            Files.deleteIfExists(wrapper.getXmlTempFile().toPath());
        }
    }

    private QaXmlReadService reader() {
        Configuracion configuration = new Configuracion();
        configuration.setDashboardsOutputs(temp.toFile());
        return new QaXmlReadService(XmlReaderModule.documentBuilderProvider(), configuration,
                AppModule.loremProvider(), new Random(1), Map.of(
                PatternEnum.STRING_PATTERN, PatternsModule.randomStringPattern(),
                PatternEnum.INTEGER_PATTERN, PatternsModule.randomIntegerPattern()));
    }
}

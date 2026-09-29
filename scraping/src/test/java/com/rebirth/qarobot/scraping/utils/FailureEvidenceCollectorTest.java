package com.rebirth.qarobot.scraping.utils;

import com.rebirth.qarobot.commons.models.dtos.FailureEvidence;
import com.rebirth.qarobot.commons.models.dtos.Verificador;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ClickActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.KindOfBy;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SelectorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FailureEvidenceCollectorTest {
    private static final String URL = "https://example.test/pedido?cliente=José";
    private static final byte[] SCREENSHOT = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jf1sAAAAASUVORK5CYII=");

    @TempDir
    Path reportDirectory;

    @Test
    void writesScreenshotAndUtf8DetailsAtTheFailingStep() throws Exception {
        FakeBrowser browser = new FakeBrowser();
        FailureEvidenceCollector collector = new FailureEvidenceCollector(browser.driver(), reportDirectory);
        ClickActionType action = click("confirmar", "Confirmación del envío");
        IllegalStateException error = new IllegalStateException("No apareció el botón de envío",
                new IllegalArgumentException("detalle original"));

        collector.capture(action, "CSS: #enviar", error, null);

        FailureEvidence evidence = onlyEvidence(collector);
        assertEquals("confirmar", evidence.getActionId());
        assertEquals("Confirmación del envío", evidence.getDescription());
        assertEquals("ClickActionType", evidence.getActionType());
        assertEquals(URL, evidence.getUrl());
        assertEquals("CSS: #enviar", evidence.getSelectorUsed());
        assertEquals(List.of("ID: confirmar", "CSS: #enviar"), evidence.getSelectors());
        assertEquals(error.getClass().getName(), evidence.getExceptionType());
        assertEquals(error.getMessage(), evidence.getMessage());
        assertDoesNotThrow(() -> OffsetDateTime.parse(evidence.getCapturedAt()));
        assertArrayEquals(SCREENSHOT, Files.readAllBytes(reportDirectory.resolve(evidence.getScreenshotPath())));
        String details = Files.readString(reportDirectory.resolve(evidence.getDetailsPath()), StandardCharsets.UTF_8);
        assertTrue(details.contains(URL));
        assertTrue(details.contains("Confirmación del envío"));
        assertTrue(details.contains("Selector utilizado: CSS: #enviar"));
        assertTrue(details.contains("Selectores configurados: ID: confirmar; CSS: #enviar"));
        assertTrue(details.contains("java.lang.IllegalStateException: No apareció el botón de envío"));
        assertTrue(details.contains("Caused by: java.lang.IllegalArgumentException: detalle original"));
        assertTrue(details.contains("FailureEvidenceCollectorTest.writesScreenshotAndUtf8DetailsAtTheFailingStep"));
        assertTrue(evidence.getCaptureProblems().isEmpty());
        assertEquals(1, browser.screenshotCalls);
    }

    @Test
    void propagatedExceptionKeepsTheOriginalChildStepAndCapturesOnlyOnce() {
        FakeBrowser browser = new FakeBrowser();
        FailureEvidenceCollector collector = new FailureEvidenceCollector(browser.driver(), reportDirectory);
        RuntimeException failure = new IllegalStateException("Falló la acción hija");
        BaseActionType parent = new BaseActionType();
        parent.setId("iteracion");
        parent.setDesc("Iterar pedidos");

        collector.capture(click("hija", "Confirmar pedido"), "ID: confirmar", failure, null);
        collector.capture(parent, null, failure, null);

        FailureEvidence evidence = onlyEvidence(collector);
        assertEquals("hija", evidence.getActionId());
        assertEquals("Confirmar pedido", evidence.getDescription());
        assertEquals("ID: confirmar", evidence.getSelectorUsed());
        assertEquals(1, browser.screenshotCalls);
        assertEquals(1, browser.urlCalls);
    }

    @Test
    void repeatedFailedChecksInTheSameStepKeepSeparateFiles() throws Exception {
        FakeBrowser browser = new FakeBrowser();
        FailureEvidenceCollector collector = new FailureEvidenceCollector(browser.driver(), reportDirectory);
        ClickActionType action = click("repeated", "Revisar dos resultados");

        collector.capture(action, null, null, failedCheck("uno", "uno", "1", "2"));
        collector.capture(action, null, null, failedCheck("dos", "dos", "3", "4"));

        List<FailureEvidence> evidence = collector.getEvidence();
        assertEquals(2, evidence.size());
        assertNotEquals(evidence.get(0).getScreenshotPath(), evidence.get(1).getScreenshotPath());
        assertNotEquals(evidence.get(0).getDetailsPath(), evidence.get(1).getDetailsPath());
        for (FailureEvidence item : evidence) {
            assertEquals("repeated", item.getActionId());
            assertArrayEquals(SCREENSHOT, Files.readAllBytes(reportDirectory.resolve(item.getScreenshotPath())));
            assertTrue(Files.readString(reportDirectory.resolve(item.getDetailsPath()))
                    .contains("Regla: " + item.getVerifications().get(0).getRule()));
        }
        assertEquals(2, browser.screenshotCalls);
    }

    @Test
    void passingAndSkippedChecksDoNotCaptureTheBrowserOrCreateFiles() {
        FakeBrowser browser = new FakeBrowser();
        FailureEvidenceCollector collector = new FailureEvidenceCollector(browser.driver(), reportDirectory);
        Verificador passed = failedCheck("passed", "igual", "1", "1");
        passed.setOk(true);
        Verificador skipped = failedCheck("skipped", "igual", "1", "2");
        skipped.setSkip(true);

        collector.capture(click("paso", "Verificar"), null, null, passed);
        collector.capture(click("paso", "Verificar"), null, null, skipped);

        assertTrue(collector.getEvidence().isEmpty());
        assertEquals(0, browser.screenshotCalls);
        assertEquals(0, browser.urlCalls);
        assertFalse(Files.exists(reportDirectory.resolve("evidence")));
    }

    @Test
    void compositeChecksKeepSnapshotsOfFailedLeavesOnly() throws Exception {
        FakeBrowser browser = new FakeBrowser();
        FailureEvidenceCollector collector = new FailureEvidenceCollector(browser.driver(), reportDirectory);
        Verificador failed = failedCheck("importe", "importe igual", "19.99", "20.00");
        failed.setScript("importe === esperado");
        Verificador passed = failedCheck("estado", "estado igual", "listo", "listo");
        passed.setOk(true);
        Verificador skipped = failedCheck("moneda", "moneda igual", "MXN", "USD");
        skipped.setSkip(true);
        Verificador inner = Verificador.create("inner", "Valores del pedido");
        inner.addVerificacion(failed);
        inner.addVerificacion(passed);
        inner.addVerificacion(skipped);
        Verificador outer = Verificador.create("outer", "Pedido completo");
        outer.addVerificacion(inner);

        collector.capture(click("verificar", "Verificar pedido"), null, null, outer);
        failed.setEvaluado("valor cambiado después de capturar");
        failed.setResultado("otro resultado");
        failed.setRule("otra regla");
        failed.setScript("otro script");

        FailureEvidence evidence = onlyEvidence(collector);
        assertEquals(1, evidence.getVerifications().size());
        Verificador snapshot = evidence.getVerifications().get(0);
        assertNotSame(failed, snapshot);
        assertEquals("importe", snapshot.getId());
        assertEquals("Verificación importe", snapshot.getDescVerifyAccion());
        assertEquals("importe igual", snapshot.getRule());
        assertEquals("19.99", snapshot.getEvaluado());
        assertEquals("20.00", snapshot.getResultado());
        assertEquals("importe === esperado", snapshot.getScript());
        String details = Files.readString(reportDirectory.resolve(evidence.getDetailsPath()));
        assertTrue(details.contains("Regla: importe igual"));
        assertTrue(details.contains("Valor evaluado: 19.99"));
        assertTrue(details.contains("Comparacion / resultado: 20.00"));
        assertFalse(details.contains("Regla: estado igual"));
        assertFalse(details.contains("Regla: moneda igual"));
    }

    @Test
    void closedBrowserStillSavesTheOriginalFailureAndCaptureWarnings() throws Exception {
        FakeBrowser browser = new FakeBrowser();
        browser.failure = new NoSuchSessionException("La sesión ya se cerró");
        FailureEvidenceCollector collector = new FailureEvidenceCollector(browser.driver(), reportDirectory);
        RuntimeException original = new IllegalStateException("Falló la compra");

        assertDoesNotThrow(() -> collector.capture(click("compra", "Comprar"), null, original, null));

        FailureEvidence evidence = onlyEvidence(collector);
        assertEquals("Falló la compra", evidence.getMessage());
        assertEquals(IllegalStateException.class.getName(), evidence.getExceptionType());
        assertNull(evidence.getUrl());
        assertNull(evidence.getScreenshotPath());
        assertEquals(2, evidence.getCaptureProblems().size());
        String details = Files.readString(reportDirectory.resolve(evidence.getDetailsPath()));
        assertTrue(details.contains("java.lang.IllegalStateException: Falló la compra"));
        assertTrue(details.contains("No se pudo leer la URL"));
        assertTrue(details.contains("No se pudo guardar la captura"));
        assertTrue(details.contains("La sesión ya se cerró"));
    }

    @Test
    void blockedOutputDirectoryDoesNotReplaceTheOriginalFailure() throws Exception {
        Path blocked = reportDirectory.resolve("evidence");
        Files.writeString(blocked, "Existing file");
        FailureEvidenceCollector collector = new FailureEvidenceCollector(new FakeBrowser().driver(), reportDirectory);
        RuntimeException original = new IllegalArgumentException("Error original");

        assertDoesNotThrow(() -> collector.capture(click("paso", "Ejecutar"), null, original, null));

        FailureEvidence evidence = onlyEvidence(collector);
        assertEquals("Error original", evidence.getMessage());
        assertEquals(original.getClass().getName(), evidence.getExceptionType());
        assertEquals(URL, evidence.getUrl());
        assertNull(evidence.getScreenshotPath());
        assertNull(evidence.getDetailsPath());
        assertEquals(2, evidence.getCaptureProblems().size());
        assertTrue(evidence.getCaptureProblems().stream().anyMatch(problem -> problem.contains("guardar la captura")));
        assertTrue(evidence.getCaptureProblems().stream().anyMatch(problem -> problem.contains("guardar los detalles")));
        assertEquals("Existing file", Files.readString(blocked));
    }

    @Test
    void arbitraryActionIdsNeverBecomeFilePaths() throws Exception {
        Path report = reportDirectory.resolve("report");
        FailureEvidenceCollector collector = new FailureEvidenceCollector(new FakeBrowser().driver(), report);
        List<String> actionIds = List.of("../../escaped", "..\\..\\escaped", "C:\\temp\\outside", "CON:<>|?*");
        for (String actionId : actionIds) {
            collector.capture(click(actionId, "Acción con identificador libre"), null,
                    new IllegalStateException("Fallo"), null);
        }

        assertEquals(actionIds.size(), collector.getEvidence().size());
        Path expectedDirectory = report.resolve("evidence").toAbsolutePath().normalize();
        for (int i = 0; i < actionIds.size(); i++) {
            FailureEvidence evidence = collector.getEvidence().get(i);
            assertEquals(actionIds.get(i), evidence.getActionId());
            assertTrue(evidence.getScreenshotPath().matches("evidence/failure-\\d+\\.png"));
            assertTrue(evidence.getDetailsPath().matches("evidence/failure-\\d+\\.txt"));
            for (String relativePath : List.of(evidence.getScreenshotPath(), evidence.getDetailsPath())) {
                Path output = report.resolve(relativePath).toAbsolutePath().normalize();
                assertEquals(expectedDirectory, output.getParent());
                assertTrue(Files.isRegularFile(output));
            }
        }
        try (var files = Files.walk(reportDirectory)) {
            assertEquals(actionIds.size() * 2L, files.filter(Files::isRegularFile).count());
        }
    }

    private static FailureEvidence onlyEvidence(FailureEvidenceCollector collector) {
        assertEquals(1, collector.getEvidence().size());
        return collector.getEvidence().get(0);
    }

    private static ClickActionType click(String id, String description) {
        ClickActionType action = new ClickActionType();
        action.setId(id);
        action.setDesc(description);
        SelectorType primary = new SelectorType();
        primary.setBy(KindOfBy.ID);
        primary.setValue("confirmar");
        SelectorType fallback = new SelectorType();
        fallback.setBy(KindOfBy.CSS);
        fallback.setValue("#enviar");
        action.getSelector().addAll(List.of(primary, fallback));
        return action;
    }

    private static Verificador failedCheck(String id, String rule, String evaluated, String expected) {
        Verificador verification = Verificador.create(id, "Verificación " + id);
        verification.setRule(rule);
        verification.setEvaluado(evaluated);
        verification.setResultado(expected);
        return verification;
    }

    private static final class FakeBrowser {
        private int screenshotCalls;
        private int urlCalls;
        private RuntimeException failure;

        WebDriver driver() {
            return (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class, TakesScreenshot.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getCurrentUrl":
                                urlCalls++;
                                if (failure != null) throw failure;
                                return URL;
                            case "getScreenshotAs":
                                screenshotCalls++;
                                if (failure != null) throw failure;
                                assertSame(OutputType.BYTES, args[0]);
                                return SCREENSHOT.clone();
                            default:
                                throw new AssertionError("Unexpected browser call: " + method.getName());
                        }
                    });
        }
    }
}

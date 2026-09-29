package com.rebirth.qarobot.scraping.di.modules;

import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.Verificador;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DashboardTemplateTest {
    @TempDir Path temp;

    @Test
    void rendersTheBundledDashboardWithItsCssAndJavascript() throws Exception {
        String output = render(model());

        assertTrue(output.contains("&lt;qarobot/&gt;"));
        assertTrue(output.contains("<style>"));
        assertTrue(output.contains("<script>"));
        assertTrue(output.contains("No se registraron fallos"));
    }

    @Test
    void rendersAnEmptyFailureList() throws Exception {
        Map<String, Object> model = model();
        model.put("failureEvidence", List.of());

        String output = render(model);

        assertTrue(output.contains("badge bg-secondary\">0</span>"));
        assertFalse(output.contains("Paso con fallo:"));
    }

    @Test
    void rendersFailureEvidenceAndNestedChecksAsEscapedText() throws Exception {
        Verificador child = Verificador.create("check-1", "Compare content");
        child.setRule("<equals>");
        child.setEvaluado("<actual>");
        child.setResultado("<expected>");
        child.setOk(false);
        Verificador parent = Verificador.create("checks", "Compound check");
        parent.addVerificacion(child);

        Map<String, Object> failure = Map.ofEntries(
                Map.entry("id", "failure-001"),
                Map.entry("description", "<step>"),
                Map.entry("actionId", "\"<action>"),
                Map.entry("actionType", "<ClickAction>"),
                Map.entry("capturedAt", "<time>"),
                Map.entry("url", "javascript:<script>url()</script>"),
                Map.entry("selectorUsed", "#login[title=\"<value>\"]"),
                Map.entry("selectors", List.of("<configured>")),
                Map.entry("exceptionType", "<AssertionError>"),
                Map.entry("message", "<script>failure()</script>"),
                Map.entry("screenshotPath", "evidence/failure-001.png"),
                Map.entry("detailsPath", "evidence/failure-001.txt"),
                Map.entry("verifications", List.of(parent)),
                Map.entry("captureProblems", List.of()));
        Map<String, Object> model = model();
        model.put("failureEvidence", List.of(failure));

        String output = render(model);

        assertTrue(output.contains("badge bg-danger\">1</span>"));
        for (String value : List.of("step", "action", "ClickAction", "time", "value", "configured",
                "AssertionError", "equals", "actual", "expected")) {
            assertTrue(output.contains("&lt;" + value + "&gt;"), value);
            assertFalse(output.contains("<" + value + ">"), value);
        }
        assertTrue(output.contains("javascript:&lt;script&gt;url()&lt;/script&gt;"));
        assertFalse(output.contains("href=\"javascript:"));
        assertFalse(output.contains("src=\"javascript:"));
        assertTrue(output.contains("&lt;script&gt;failure()&lt;/script&gt;"));
        assertFalse(output.contains("<script>failure()</script>"));
        assertTrue(output.contains("src=\"evidence/failure-001.png\""));
        assertTrue(output.contains("href=\"evidence/failure-001.png\" download"));
        assertTrue(output.contains("href=\"evidence/failure-001.txt\" download"));
        assertTrue(output.contains("No paso la verificacion"));
    }

    @Test
    void keepsTheOriginalFailureVisibleWhenScreenshotCaptureFails() throws Exception {
        Map<String, Object> model = model();
        model.put("failureEvidence", List.of(Map.of(
                "description", "Submit form", "exceptionType", "TimeoutException",
                "message", "Original action timed out", "captureProblems", List.of("<browser closed>"))));

        String output = render(model);

        assertTrue(output.contains("Error original: TimeoutException"));
        assertTrue(output.contains("Original action timed out"));
        assertTrue(output.contains("No hay captura del navegador disponible"));
        assertTrue(output.contains("&lt;browser closed&gt;"));
        assertFalse(output.contains("<browser closed>"));
        assertFalse(output.contains("Descargar captura"));
        assertFalse(output.contains("href=\"evidence/"));
    }

    @Test
    void missingTemplatesHaveAnActionableError() {
        Configuracion configuration = new Configuracion();
        configuration.setDashboardtemplateHome(temp.resolve("missing").toFile());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> FreemarkerModule.freemarkerProvider(configuration));

        assertTrue(error.getMessage().contains("qarobot.home"));
    }

    private Map<String, Object> model() {
        return new HashMap<>(Map.of(
                "myConfigvalues", List.of(), "actionsdto", List.of(), "verificadors", List.of(),
                "counterAcciones", 0, "counterPruebasOk", 0, "counterPruebasErr", 0, "counterPruebasSkip", 0,
                "execxml", "<qarobot/>", "dashboartitle", "Offline demo"));
    }

    private String render(Map<String, Object> model) throws Exception {
        Configuracion configuration = new Configuracion();
        configuration.setDashboardtemplateHome(Path.of("dashboardtemplate").toAbsolutePath().toFile());
        Template template = FreemarkerModule.templateProvider(FreemarkerModule.freemarkerProvider(configuration));
        StringWriter output = new StringWriter();
        template.process(model, output);
        return output.toString();
    }
}

package com.rebirth.qarobot.app.cli;

import com.rebirth.qarobot.scraping.enums.Browser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CliSuiteRunnerTest {
    @TempDir Path temp;

    @Test
    void continuesAfterFailedChecksAndInitializationErrorsAndAggregatesJunit() throws Exception {
        List<Path> scenarios = List.of(temp.resolve("first.xml"), temp.resolve("second.xml"), temp.resolve("third.xml"));
        List<Path> executed = new ArrayList<>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        var runner = runner((scenario, directory) -> {
            executed.add(scenario);
            if (scenario.equals(scenarios.get(0))) {
                return new ScenarioResult(scenario, ScenarioResult.Status.FAILURE, 0.2, directory.resolve("index.html"),
                        "Esperado: A&B <pedido> \"listo\"; obtenido: revisión 😀\u0001", "AssertionError");
            }
            if (scenario.equals(scenarios.get(1))) throw new IllegalStateException("Browser could not start");
            return new ScenarioResult(scenario, ScenarioResult.Status.PASSED, 0.1, directory.resolve("index.html"), "", "");
        }, output);
        assertEquals(1, runner.run(options(scenarios)));
        assertEquals(scenarios, executed);
        Document report = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(temp.resolve("junit.xml").toFile());
        assertEquals("3", report.getDocumentElement().getAttribute("tests"));
        assertEquals("1", report.getDocumentElement().getAttribute("failures"));
        assertEquals("1", report.getDocumentElement().getAttribute("errors"));
        assertEquals(3, report.getElementsByTagName("testcase").getLength());
        assertEquals("Esperado: A&B <pedido> \"listo\"; obtenido: revisión 😀",
                report.getElementsByTagName("failure").item(0).getTextContent());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Total: 3 | Correctos: 1 | Fallidos: 1 | Errores: 1"));
        try (var files = Files.walk(temp.resolve("reports"))) {
            assertEquals(2, files.filter(path -> path.getFileName().toString().equals("error.txt")).count());
        }
    }

    @Test
    void repeatedRunsWithSameScenarioNameNeverOverwriteReports() throws Exception {
        Path scenario = temp.resolve("same.xml");
        List<Path> directories = new ArrayList<>();
        var runner = runner((file, directory) -> {
            directories.add(directory);
            Files.writeString(directory.resolve("index.html"), "report-" + directories.size());
            return new ScenarioResult(file, ScenarioResult.Status.PASSED, 0, directory.resolve("index.html"), "", "");
        }, new ByteArrayOutputStream());
        assertEquals(0, runner.run(options(List.of(scenario))));
        assertEquals(0, runner.run(options(List.of(scenario))));
        assertNotEquals(directories.get(0), directories.get(1));
        assertEquals("report-1", Files.readString(directories.get(0).resolve("index.html")));
        assertEquals("report-2", Files.readString(directories.get(1).resolve("index.html")));
    }

    private CliOptions options(List<Path> scenarios) {
        return new CliOptions(scenarios, Browser.CHROME, true, temp.resolve("workspace"), temp.resolve("reports"), temp.resolve("junit.xml"));
    }

    private CliSuiteRunner runner(ScenarioExecutor executor, ByteArrayOutputStream output) {
        return new CliSuiteRunner(executor, new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream()));
    }
}

package com.rebirth.qarobot.app.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CliApplicationTest {
    @TempDir Path temp;
    private final Map<String, String> originalProperties = new HashMap<>();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    CliApplicationTest() {
        for (String key : new String[]{"qarobot.cli", "qarobot.headless", "qarobot.openReport", "qarobot.workspace"}) {
            originalProperties.put(key, System.getProperty(key));
        }
    }

    @AfterEach
    void restoreProperties() {
        originalProperties.forEach((key, value) -> {
            if (value == null) System.clearProperty(key);
            else System.setProperty(key, value);
        });
    }

    @Test
    void helpAndInvalidArgumentsDoNotInitializeExecutionGraph() {
        var app = new CliApplication(options -> { throw new AssertionError("Must not initialize the graph"); });
        assertEquals(0, execute(app, "--help"));
        assertEquals(0, execute(app, "run", "--help"));
        assertEquals(2, execute(app, "run"));
        assertEquals(2, execute(app, "run", temp.resolve("missing.xml").toString()));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Códigos de salida"));
    }

    @Test
    void validatesLaterPathsBeforeStartingFirstScenario() throws Exception {
        Path valid = Files.writeString(temp.resolve("valid.xml"), "<qarobot/>");
        var app = new CliApplication(options -> { throw new AssertionError("Must not initialize the graph"); });
        assertEquals(2, execute(app, "run", valid.toString(), temp.resolve("missing.xml").toString()));
    }

    @Test
    void initializesCliPropertiesBeforeGraphAndReturnsSuccess() throws Exception {
        Path scenario = Files.writeString(temp.resolve("valid.xml"), "<qarobot/>");
        AtomicInteger constructions = new AtomicInteger();
        var app = new CliApplication(options -> {
            constructions.incrementAndGet();
            assertEquals("true", System.getProperty("qarobot.cli"));
            assertEquals("true", System.getProperty("qarobot.headless"));
            assertEquals("false", System.getProperty("qarobot.openReport"));
            assertEquals(temp.resolve("workspace").toString(), System.getProperty("qarobot.workspace"));
            return (file, directory) -> new ScenarioResult(file, ScenarioResult.Status.PASSED, 0, null, "", "");
        });
        assertEquals(0, execute(app, "run", scenario.toString(), "--workspace", temp.resolve("workspace").toString()));
        assertEquals(1, constructions.get());
    }

    @Test
    void dependencyInitializationFailureProducesJUnitAndNonzeroExit() throws Exception {
        Path scenario = Files.writeString(temp.resolve("valid.xml"), "<qarobot/>");
        Path junit = temp.resolve("reports/junit.xml");
        var app = new CliApplication(options -> { throw new IllegalStateException("Cannot create workspace"); });
        assertEquals(1, execute(app, "run", scenario.toString(), "--output", temp.resolve("output").toString(), "--junit", junit.toString()));
        assertTrue(Files.readString(junit).contains("<error"));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Cannot create workspace"));
    }

    private int execute(CliApplication application, String... args) {
        return application.execute(args, new PrintStream(output, true, StandardCharsets.UTF_8), new PrintStream(errors, true, StandardCharsets.UTF_8));
    }
}

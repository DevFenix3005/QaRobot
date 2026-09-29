import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.scraping.di.modules.ScriptModule;
import com.rebirth.qarobot.scraping.models.qabot.rhinox.Rhinox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ScriptTest {
    @TempDir
    Path scriptsHome;

    private ScriptEngine graalEngine;

    @AfterEach
    void closeEngine() throws Exception {
        if (graalEngine instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    @Test
    void scriptsCanReadUpdatedContextAndReturnValuesAcrossThreads() throws Exception {
        Rhinox rhinox = createRhinox();
        rhinox.addProperties2Scope(Map.of("x", 12, "y", 12));
        rhinox.addProperties2Scope("x", 15);
        rhinox.addProperties2Scope("y", 15);

        String script = """
                const values = [12, 12, x, y];
                return values.reduce((sum, value) => sum + value, 0);
                """;
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Object>> tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                // Production actions create separate Rhinox wrappers around the same engine.
                tasks.add(() -> new Rhinox(graalEngine).runScript("context-values", script));
            }
            for (var future : executor.invokeAll(tasks, 30, TimeUnit.SECONDS)) {
                assertEquals(54, ((Number) future.get()).intValue());
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void librariesLoadInFilenameOrderAndSkipUnrelatedFilesAndDirectories() throws Exception {
        // Create the dependent library first to avoid relying on filesystem enumeration order.
        Files.writeString(scriptsHome.resolve("20-dependent.js"), "self.answer = self.base + 2;");
        Files.writeString(scriptsHome.resolve("10-base.js"), "self.base = 40;");
        Files.writeString(scriptsHome.resolve("README.txt"), "This is documentation, not JavaScript.");
        Files.createDirectory(scriptsHome.resolve("nested.js"));

        Rhinox rhinox = createRhinox();
        assertEquals(42, ((Number) rhinox.runScript("libraries", "return self.answer;")).intValue());
        // Loaded files must be released, including on Windows.
        Files.delete(scriptsHome.resolve("10-base.js"));
    }

    @Test
    void trailingLineCommentsDoNotConsumeTheFunctionWrapper() throws Exception {
        assertEquals(42, ((Number) createRhinox().runScript("comment", "return 42; // comment")).intValue());
    }

    @Test
    void localDeclarationsAreIsolatedBetweenInvocations() throws Exception {
        Rhinox rhinox = createRhinox();
        assertEquals(42, ((Number) rhinox.runScript("first", "const answer = 42; return answer;")).intValue());
        assertEquals("undefined", rhinox.runScript("second", "return typeof answer;"));
    }

    @Test
    void scriptErrorsReachTheCaller() throws Exception {
        Rhinox rhinox = createRhinox();
        ScriptException failure = assertThrows(ScriptException.class,
                () -> rhinox.runScript("invalid-script", "throw new Error('verification failed');"));
        assertTrue(failure.getMessage().contains("verification failed"));
    }

    @Test
    void invalidLibrariesReportTheFileAndPreserveTheCause() throws Exception {
        Path library = scriptsHome.resolve("invalid.js");
        Files.writeString(library, "function (");
        IllegalStateException failure = assertThrows(IllegalStateException.class, this::createRhinox);
        assertTrue(failure.getMessage().contains("invalid.js"));
        assertInstanceOf(ScriptException.class, failure.getCause());
        Files.delete(library);
    }

    @Test
    void unavailableEngineReportsAnActionableError() {
        ScriptEngineManager noEngines = new ScriptEngineManager() {
            @Override
            public ScriptEngine getEngineByName(String name) {
                return null;
            }
        };
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ScriptModule.scopeProvide(noEngines, configuration()));
        assertTrue(failure.getMessage().contains("GraalJS"));
    }

    @Test
    void aFreshWorkspaceDoesNotRequireExternalLibraries() throws Exception {
        Configuracion configuration = configuration();
        configuration.setScriptsHome(scriptsHome.resolve("not-created-yet").toFile());
        graalEngine = ScriptModule.scopeProvide(ScriptModule.contextProvide(), configuration);
        assertEquals(42, ((Number) new Rhinox(graalEngine).runScript("fresh", "return 42;")).intValue());
    }

    private Rhinox createRhinox() {
        graalEngine = ScriptModule.scopeProvide(ScriptModule.contextProvide(), configuration());
        return new Rhinox(graalEngine);
    }

    private Configuracion configuration() {
        Configuracion configuration = new Configuracion();
        configuration.setScriptsHome(scriptsHome.toFile());
        return configuration;
    }
}
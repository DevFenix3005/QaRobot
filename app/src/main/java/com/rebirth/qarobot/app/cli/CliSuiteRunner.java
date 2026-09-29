package com.rebirth.qarobot.app.cli;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CliSuiteRunner {
    private final ScenarioExecutor executor;
    private final PrintStream out;
    private final PrintStream err;

    CliSuiteRunner(ScenarioExecutor executor, PrintStream out, PrintStream err) {
        this.executor = executor;
        this.out = out;
        this.err = err;
    }

    int run(CliOptions options) throws Exception {
        Files.createDirectories(options.output());
        List<ScenarioResult> results = new ArrayList<>();
        for (Path scenario : options.scenarios()) {
            long start = System.nanoTime();
            ScenarioResult result;
            Path directory = null;
            out.println("[RUN] " + scenario);
            try {
                String prefix = scenario.getFileName().toString().replaceAll("[^\\p{L}\\p{N}._-]", "_");
                directory = Files.createTempDirectory(options.output(), prefix + "-");
                result = executor.execute(scenario, directory);
            } catch (Exception failure) {
                result = ScenarioResult.error(scenario, elapsed(start), null, failure);
            }
            results.add(result);
            String status = switch (result.status()) {
                case PASSED -> "PASS";
                case FAILURE -> "FAIL";
                case ERROR -> "ERROR";
            };
            out.printf(Locale.ROOT, "[%s] %s (%.2f s)%n", status, scenario.getFileName(), result.seconds());
            if (result.status() != ScenarioResult.Status.PASSED) {
                err.println(result.message());
                if (directory != null) {
                    try {
                        Files.writeString(directory.resolve("error.txt"), result.errorType() + System.lineSeparator()
                                + result.message() + System.lineSeparator(), StandardCharsets.UTF_8);
                    } catch (Exception failure) {
                        err.println("No se pudo guardar el diagnóstico: " + failure.getMessage());
                    }
                }
            }
            if (result.report() != null) out.println("Reporte: " + result.report());
            else if (directory != null) out.println("Salida: " + directory);
        }
        out.printf("Total: %d | Correctos: %d | Fallidos: %d | Errores: %d%n", results.size(),
                JunitReport.count(results, ScenarioResult.Status.PASSED),
                JunitReport.count(results, ScenarioResult.Status.FAILURE),
                JunitReport.count(results, ScenarioResult.Status.ERROR));
        if (options.junit() != null) {
            JunitReport.write(options.junit(), results);
            out.println("JUnit: " + options.junit());
        }
        return results.stream().allMatch(result -> result.status() == ScenarioResult.Status.PASSED) ? 0 : 1;
    }

    static double elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000_000d;
    }
}

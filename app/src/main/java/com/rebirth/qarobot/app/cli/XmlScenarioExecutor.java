package com.rebirth.qarobot.app.cli;

import com.rebirth.qarobot.app.utils.QaXmlReadService;
import com.rebirth.qarobot.commons.models.dtos.QarobotWrapper;
import com.rebirth.qarobot.commons.models.dtos.Verificador;
import com.rebirth.qarobot.commons.exceptions.NotFoundWebElement;
import com.rebirth.qarobot.scraping.di.ScrappingComponent;
import com.rebirth.qarobot.scraping.enums.Browser;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
final class XmlScenarioExecutor implements ScenarioExecutor {
    private final QaXmlReadService reader;
    private final ScrappingComponent.Factory factory;
    private final Browser browser;

    XmlScenarioExecutor(QaXmlReadService reader, ScrappingComponent.Factory factory, Browser browser) {
        this.reader = reader;
        this.factory = factory;
        this.browser = browser;
    }

    @Override
    public ScenarioResult execute(Path scenario, Path directory) throws Exception {
        long start = System.nanoTime();
        QarobotWrapper xml = null;
        try {
            reader.setQaXmlFile(scenario.toFile());
            xml = reader.read();
            if (!xml.isValidXml()) {
                throw new IllegalArgumentException("XML inválido: " + String.join(System.lineSeparator(),
                        xml.getErrores() == null ? List.of("La validación del esquema falló.") : xml.getErrores()));
            }
            if (xml.getConfiguration() == null) {
                throw new IllegalArgumentException("El escenario no contiene <configuration/>: " + scenario);
            }
            xml.setDashboardExitFile(directory.toFile());
            var component = factory.create(browser);
            // Acquire the registry before the graph creates the driver: partial initialization
            // failures must also release the resources that were successfully constructed.
            try (var resources = component.getExecutionResources(); var robot = component.getQaRobotXml()) {
                robot.setQaRobot(xml);
                boolean passed = robot.flux();
                Path report = Files.isRegularFile(directory.resolve("index.html")) ? directory.resolve("index.html") : null;
                Throwable failure = robot.getExecutionFailure();
                if (failure != null) {
                    if (failure instanceof NotFoundWebElement) {
                        return new ScenarioResult(scenario, ScenarioResult.Status.FAILURE, CliSuiteRunner.elapsed(start), report,
                                failure.getMessage(), failure.getClass().getName());
                    }
                    return ScenarioResult.error(scenario, CliSuiteRunner.elapsed(start), report, failure);
                }
                if (!passed) {
                    String message = robot.getSeleniumHelper().getVerificadores().stream()
                            .filter(check -> !check.isSkip() && !check.isOk())
                            .map(XmlScenarioExecutor::describeVerification).collect(Collectors.joining(System.lineSeparator()));
                    if (message.isBlank()) message = "La prueba terminó con verificaciones fallidas.";
                    return new ScenarioResult(scenario, ScenarioResult.Status.FAILURE, CliSuiteRunner.elapsed(start), report,
                            message, "AssertionError");
                }
                return new ScenarioResult(scenario, ScenarioResult.Status.PASSED, CliSuiteRunner.elapsed(start), report, "", "");
            }
        } catch (Exception failure) {
            Path report = directory.resolve("index.html");
            return ScenarioResult.error(scenario, CliSuiteRunner.elapsed(start), Files.isRegularFile(report) ? report : null, failure);
        } finally {
            if (xml != null && xml.getXmlTempFile() != null) {
                try {
                    Files.deleteIfExists(xml.getXmlTempFile().toPath());
                } catch (IOException | SecurityException cleanupFailure) {
                    // Preserve the scenario's result and report if temporary-file cleanup fails.
                    log.warn("No se pudo eliminar el XML temporal {}", xml.getXmlTempFile(), cleanupFailure);
                }
            }
        }
    }

    private static String describeVerification(Verificador check) {
        return check.getDescVerifyAccion() + ": esperado=" + check.getResultado() + ", obtenido=" + check.getEvaluado()
                + ", regla=" + check.getRule();
    }
}

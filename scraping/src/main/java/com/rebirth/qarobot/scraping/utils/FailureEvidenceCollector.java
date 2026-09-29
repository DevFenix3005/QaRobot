package com.rebirth.qarobot.scraping.utils;

import com.rebirth.qarobot.commons.models.dtos.FailureEvidence;
import com.rebirth.qarobot.commons.models.dtos.Verificador;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionTypeWithSelectorAndTimeOut;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionTypeWithSelectorLambdaAndTimeOut;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SelectorType;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Capturing evidence is best effort: it must never replace the original failure. */
public final class FailureEvidenceCollector {
    private final WebDriver driver;
    private final Path reportDirectory;
    private final List<FailureEvidence> evidence = new ArrayList<>();
    private final Set<Throwable> capturedExceptions = Collections.newSetFromMap(new IdentityHashMap<>());

    public FailureEvidenceCollector(WebDriver driver, Path reportDirectory) {
        this.driver = driver;
        this.reportDirectory = reportDirectory;
    }

    public List<FailureEvidence> getEvidence() {
        return List.copyOf(evidence);
    }

    public void capture(BaseActionType action, String selectorUsed, Throwable error, Verificador verification) {
        // A nested action and its enclosing iteration propagate the same exception.
        if (error != null && !capturedExceptions.add(error)) return;
        if (verification != null && (verification.isOk() || verification.isSkip())) return;

        FailureEvidence item = new FailureEvidence();
        item.setId("failure-%04d".formatted(evidence.size() + 1));
        item.setCapturedAt(OffsetDateTime.now().toString());
        item.setActionId(action == null ? "" : text(action.getId()));
        item.setDescription(action == null ? "Preparacion de la prueba" : text(action.getDesc()));
        item.setActionType(action == null ? "Configuracion" : action.getClass().getSimpleName());
        item.setSelectorUsed(selectorUsed);
        item.setSelectors(selectors(action).stream().map(s -> s.getBy() + ": " + s.getValue()).toList());
        item.setExceptionType(error == null ? "Verificacion fallida" : error.getClass().getName());
        item.setMessage(error == null ? text(verification == null ? null : verification.getRule()) : text(error.getMessage()));
        if (verification != null) collectFailedChecks(verification, item.getVerifications());
        evidence.add(item);

        try {
            item.setUrl(driver.getCurrentUrl());
        } catch (RuntimeException ex) {
            item.getCaptureProblems().add("No se pudo leer la URL: " + message(ex));
        }

        try {
            Path directory = reportDirectory.resolve("evidence");
            Files.createDirectories(directory);
            if (driver instanceof TakesScreenshot screenshot) {
                Path image = directory.resolve(item.getId() + ".png");
                Files.write(image, screenshot.getScreenshotAs(OutputType.BYTES));
                item.setScreenshotPath("evidence/" + image.getFileName());
            } else {
                item.getCaptureProblems().add("El navegador no permite capturas de pantalla.");
            }
        } catch (IOException | RuntimeException ex) {
            item.getCaptureProblems().add("No se pudo guardar la captura: " + message(ex));
        }

        try {
            Path details = reportDirectory.resolve("evidence").resolve(item.getId() + ".txt");
            Files.createDirectories(details.getParent());
            Files.writeString(details, describe(item, error), StandardCharsets.UTF_8);
            item.setDetailsPath("evidence/" + details.getFileName());
        } catch (IOException | RuntimeException ex) {
            item.getCaptureProblems().add("No se pudieron guardar los detalles: " + message(ex));
        }
    }

    private static List<SelectorType> selectors(BaseActionType action) {
        if (action instanceof BaseActionTypeWithSelectorAndTimeOut selected) return selected.getSelector();
        if (action instanceof BaseActionTypeWithSelectorLambdaAndTimeOut selected) return selected.getSelector();
        return List.of();
    }

    private static void collectFailedChecks(Verificador verification, List<Verificador> result) {
        if (verification.isOk() || verification.isSkip()) return;
        if (verification.getVerificadorList() != null && !verification.getVerificadorList().isEmpty()) {
            verification.getVerificadorList().forEach(child -> collectFailedChecks(child, result));
            return;
        }
        Verificador snapshot = Verificador.create(verification.getId(), verification.getDescVerifyAccion());
        snapshot.setRule(text(verification.getRule()));
        snapshot.setEvaluado(text(verification.getEvaluado()));
        snapshot.setResultado(text(verification.getResultado()));
        snapshot.setScript(verification.getScript());
        result.add(snapshot);
    }

    private static String describe(FailureEvidence item, Throwable error) {
        StringBuilder result = new StringBuilder()
                .append("Fecha: ").append(item.getCapturedAt())
                .append("\nAccion: ").append(item.getActionId())
                .append("\nDescripcion: ").append(item.getDescription())
                .append("\nTipo: ").append(item.getActionType())
                .append("\nURL: ").append(text(item.getUrl()))
                .append("\nSelector utilizado: ").append(text(item.getSelectorUsed()))
                .append("\nSelectores configurados: ").append(String.join("; ", item.getSelectors()))
                .append("\nError: ").append(item.getExceptionType()).append(": ").append(item.getMessage()).append('\n');
        for (Verificador verification : item.getVerifications()) {
            result.append("\nRegla: ").append(verification.getRule())
                    .append("\nValor evaluado: ").append(verification.getEvaluado())
                    .append("\nComparacion / resultado: ").append(verification.getResultado()).append('\n');
        }
        for (String problem : item.getCaptureProblems()) result.append("\nEvidencia: ").append(problem);
        if (error != null) {
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            result.append("\n\n").append(trace);
        }
        return result.toString();
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String message(Exception exception) {
        return exception.getClass().getSimpleName() + ": " + text(exception.getMessage());
    }
}

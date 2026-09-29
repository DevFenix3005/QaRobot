package com.rebirth.qarobot.app.cli;

import java.nio.file.Path;

public record ScenarioResult(Path scenario, Status status, double seconds,
                             Path report, String message, String errorType) {
    public enum Status { PASSED, FAILURE, ERROR }

    public static ScenarioResult error(Path scenario, double seconds, Path report, Throwable failure) {
        return new ScenarioResult(scenario, Status.ERROR, seconds, report,
                failure.getMessage() == null ? failure.toString() : failure.getMessage(), failure.getClass().getName());
    }
}

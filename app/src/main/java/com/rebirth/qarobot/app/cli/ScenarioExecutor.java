package com.rebirth.qarobot.app.cli;

import java.nio.file.Path;

@FunctionalInterface
public interface ScenarioExecutor {
    ScenarioResult execute(Path scenario, Path reportDirectory) throws Exception;
}

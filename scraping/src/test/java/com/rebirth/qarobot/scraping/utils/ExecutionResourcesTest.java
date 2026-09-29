package com.rebirth.qarobot.scraping.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionResourcesTest {
    @Test
    void closesResourcesInReverseOrderAndOnlyOnce() {
        ExecutionResources resources = new ExecutionResources();
        List<String> closed = new ArrayList<>();
        resources.add(() -> closed.add("browser"));
        resources.add(() -> closed.add("http"));
        resources.add(() -> closed.add("scripts"));

        resources.close();
        resources.close();

        assertEquals(List.of("scripts", "http", "browser"), closed);
    }

    @Test
    void attemptsEveryCloseAndPreservesAllFailures() {
        ExecutionResources resources = new ExecutionResources();
        List<String> closed = new ArrayList<>();
        IOException browserFailure = new IOException("Browser shutdown failed");
        IOException scriptFailure = new IOException("Script shutdown failed");
        resources.add(() -> {
            closed.add("browser");
            throw browserFailure;
        });
        resources.add(() -> closed.add("http"));
        resources.add(() -> {
            closed.add("scripts");
            throw scriptFailure;
        });

        IllegalStateException failure = assertThrows(IllegalStateException.class, resources::close);

        assertEquals(List.of("scripts", "http", "browser"), closed);
        assertSame(scriptFailure, failure.getCause());
        assertArrayEquals(new Throwable[]{browserFailure}, failure.getSuppressed());
        assertDoesNotThrow(resources::close);
        assertEquals(3, closed.size(), "Failed cleanup must not close the same resources twice");
    }

    @Test
    void rejectsNewResourcesAfterClosing() {
        ExecutionResources resources = new ExecutionResources();
        resources.close();

        assertThrows(IllegalStateException.class, () -> resources.add(() -> {}));
    }
}

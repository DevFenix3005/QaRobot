package com.rebirth.qarobot.scraping.utils;

import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;

import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.Deque;

/** Owns the resources allocated while constructing and running one scenario. */
@ChildComponent
public final class ExecutionResources implements AutoCloseable {
    private final Deque<AutoCloseable> resources = new ArrayDeque<>();
    private boolean closed;

    @Inject
    public ExecutionResources() {}

    public synchronized void add(AutoCloseable resource) {
        if (closed) throw new IllegalStateException("La ejecucion ya fue cerrada");
        resources.push(resource);
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        IllegalStateException failure = null;
        while (!resources.isEmpty()) {
            try {
                resources.pop().close();
            } catch (Exception error) {
                if (failure == null) failure = new IllegalStateException("No se pudieron cerrar todos los recursos de la prueba", error);
                else failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }
}

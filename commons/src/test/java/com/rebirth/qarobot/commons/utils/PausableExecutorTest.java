package com.rebirth.qarobot.commons.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PausableExecutorTest {
    @Test
    void repeatedPauseAndResumeKeepTheWorkerUsable() throws Exception {
        ObservedExecutor executor = new ObservedExecutor();
        AtomicInteger executions = new AtomicInteger();
        try {
            executor.resume();
            executor.resume();
            executor.pause();
            executor.pause();
            Future<Integer> task = executor.submit(executions::incrementAndGet);
            executor.awaitWorkerPaused();
            assertFalse(task.isDone());
            assertEquals(0, executions.get());

            executor.resume();
            executor.resume();

            assertEquals(1, task.get(5, TimeUnit.SECONDS));
            assertEquals(2, executor.submit(executions::incrementAndGet).get(5, TimeUnit.SECONDS));
        } finally {
            cleanup(executor);
        }
    }

    @Test
    void shutdownNowReleasesAWorkerAlreadyBlockedByPause() throws Exception {
        ObservedExecutor executor = new ObservedExecutor();
        try {
            executor.pause();
            Future<?> task = executor.submit(() -> {});
            executor.awaitWorkerPaused();
            assertFalse(task.isDone());

            executor.shutdownNow();

            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
                    "An interrupted worker must not remain stuck in the pause guard");
        } finally {
            cleanup(executor);
        }
    }

    @Test
    void gracefulShutdownReleasesPauseAndFinishesTheAcceptedTask() throws Exception {
        ObservedExecutor executor = new ObservedExecutor();
        try {
            executor.pause();
            Future<Integer> task = executor.submit(() -> 42);
            executor.awaitWorkerPaused();

            executor.shutdown();

            assertEquals(42, task.get(5, TimeUnit.SECONDS));
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            cleanup(executor);
        }
    }

    private static void cleanup(PausableExecutor executor) throws InterruptedException {
        // Explicitly release the guard even if the shutdown behavior under test regresses.
        executor.resume();
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Test worker must be released");
    }

    private static final class ObservedExecutor extends PausableExecutor {
        private final CountDownLatch enteredBeforeExecute = new CountDownLatch(1);
        private final AtomicReference<Thread> worker;

        private ObservedExecutor() {
            this(new AtomicReference<>());
        }

        private ObservedExecutor(AtomicReference<Thread> worker) {
            super(1, runnable -> {
                Thread thread = new Thread(runnable, "qarobot-pause-test");
                thread.setDaemon(true);
                worker.set(thread);
                return thread;
            });
            this.worker = worker;
        }

        @Override
        protected void beforeExecute(Thread thread, Runnable task) {
            enteredBeforeExecute.countDown();
            super.beforeExecute(thread, task);
        }

        private void awaitWorkerPaused() throws InterruptedException {
            assertTrue(enteredBeforeExecute.await(5, TimeUnit.SECONDS), "Worker must take the submitted task");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            // The task has been dequeued, so WAITING here is the pause guard, not the work queue.
            while (worker.get().getState() != Thread.State.WAITING && System.nanoTime() < deadline) {
                TimeUnit.MILLISECONDS.sleep(5);
            }
            assertEquals(Thread.State.WAITING, worker.get().getState(), "Worker must already be blocked by pause");
        }
    }
}

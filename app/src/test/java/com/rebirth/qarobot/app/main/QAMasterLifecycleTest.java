package com.rebirth.qarobot.app.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rebirth.qarobot.app.viewmodel.MainViewModel;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.QarobotWrapper;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.tables.ActionColorAndExIfExits;
import com.rebirth.qarobot.commons.models.dtos.toggle.PauseOrResumeState;
import com.rebirth.qarobot.commons.utils.PuaseExecutionFromStopAction;
import com.rebirth.qarobot.commons.utils.SendInfo2View;
import com.rebirth.qarobot.commons.utils.SendQaContext2View;
import com.rebirth.qarobot.commons.utils.ShowInDialog;
import com.rebirth.qarobot.scraping.QaRobotXml;
import com.rebirth.qarobot.scraping.di.ScrappingComponent;
import com.rebirth.qarobot.scraping.impl.QaRobotXmlImpl;
import com.rebirth.qarobot.scraping.utils.ExecutionResources;

class QAMasterLifecycleTest {
    @TempDir
    Path reports;

    @Test
    void newIterationsAndRunsDoNotReplayPauseOrKeepClosedRobotsSubscribed() {
        StubRobot first = new StubRobot();
        StubRobot second = new StubRobot();
        StubRobot third = new StubRobot();
        Harness harness = harness(first, second, third);
        harness.viewModel.getInteracion().onNext(2);
        harness.viewModel.getPauseOrResumenActionExecution().onNext(PauseOrResumeState.RESUME);
        first.onFlux = () -> {
            assertEquals(0, first.pauses, "A stale pause must not reach a new robot");
            harness.viewModel.getPauseOrResumenActionExecution().onNext(PauseOrResumeState.RESUME);
        };
        second.onFlux = () -> assertEquals(0, second.pauses);

        harness.master.run();

        assertTrue(first.closed);
        assertTrue(second.closed);
        assertEquals(1, first.pauses);
        assertFalse(harness.viewModel.getPauseOrResumenActionExecution().hasObservers());
        harness.viewModel.getPauseOrResumenActionExecution().onNext(PauseOrResumeState.RESUME);
        harness.viewModel.getInteracion().onNext(1);
        harness.master.run();
        assertEquals(0, third.pauses);
        assertEquals(1, first.pauses);
        assertEquals(0, second.pauses);
        assertTrue(third.closed);
        assertEquals(PauseOrResumeState.NONE,
                harness.viewModel.getPauseOrResumenActionExecution().getValue());
        assertEquals(2, harness.viewModel.successes);
    }

    @Test
    void actionErrorsFinishOnlyAfterCleanupAndStopFurtherIterations() {
        StubRobot robot = new StubRobot();
        Harness harness = harness(robot);
        harness.viewModel.getInteracion().onNext(2);
        IllegalStateException error = new IllegalStateException("Action failed");
        robot.success = false;
        robot.onFlux = () -> {
            ActionColorAndExIfExits update = ActionColorAndExIfExits.create(new BaseActionType(), Color.RED);
            update.setThrowable(error);
            robot.sendInfo.send(update);
            assertNull(harness.viewModel.failure, "Reporting an action error must not finish a running test");
            assertFalse(harness.viewModel.getStatusInitButton().getValue());
        };
        robot.onClose = () -> {
            assertNull(harness.viewModel.failure);
            assertFalse(harness.viewModel.getStatusInitButton().getValue());
            assertFalse(harness.viewModel.getPauseOrResumenActionExecution().hasObservers());
        };

        harness.master.run();

        assertTrue(robot.closed);
        assertSame(error, harness.viewModel.failure);
        assertTrue(harness.viewModel.getStatusInitButton().getValue());
        assertEquals(0, harness.viewModel.successes);
    }

    @Test
    void unexpectedFailureAlsoDisposesPauseBeforeClosingAndFinishing() {
        StubRobot robot = new StubRobot();
        Harness harness = harness(robot);
        IllegalStateException error = new IllegalStateException("Driver failed");
        robot.onFlux = () -> {
            throw error;
        };
        robot.onClose = () -> {
            assertNull(harness.viewModel.failure);
            assertFalse(harness.viewModel.getPauseOrResumenActionExecution().hasObservers());
        };

        harness.master.run();

        assertTrue(robot.closed);
        assertSame(error, harness.viewModel.failure);
        assertFalse(harness.viewModel.getPauseOrResumenActionExecution().hasObservers());
        assertEquals(PauseOrResumeState.NONE,
                harness.viewModel.getPauseOrResumenActionExecution().getValue());
    }

    @Test
    void partialInitializationClosesResourcesBeforeReportingFailureToTheGui() {
        ExecutionResources resources = new ExecutionResources();
        IllegalStateException failure = new IllegalStateException("Report template is missing");
        Harness harness = failingInitialization(resources, failure);
        AtomicInteger closes = new AtomicInteger();
        resources.add(() -> {
            assertNull(harness.viewModel.failure, "Cleanup must finish before the GUI is re-enabled");
            assertFalse(harness.viewModel.getStatusInitButton().getValue());
            closes.incrementAndGet();
        });

        harness.master.run();

        assertEquals(1, closes.get());
        assertSame(failure, harness.viewModel.failure);
        assertTrue(harness.viewModel.getStatusInitButton().getValue());
        assertEquals(0, harness.viewModel.successes);
        assertFalse(harness.viewModel.getPauseOrResumenActionExecution().hasObservers());
        resources.close();
        assertEquals(1, closes.get());
    }

    @Test
    void initializationFailureRemainsThePrimaryErrorIfResourceCleanupAlsoFails() {
        ExecutionResources resources = new ExecutionResources();
        IOException cleanupFailure = new IOException("Browser shutdown failed");
        resources.add(() -> { throw cleanupFailure; });
        IllegalStateException failure = new IllegalStateException("JavaScript library is invalid");
        Harness harness = failingInitialization(resources, failure);

        harness.master.run();

        assertSame(failure, harness.viewModel.failure);
        assertEquals(1, failure.getSuppressed().length);
        assertSame(cleanupFailure, failure.getSuppressed()[0].getCause());
        assertTrue(harness.viewModel.getStatusInitButton().getValue());
        assertEquals(0, harness.viewModel.successes);
    }

    private Harness failingInitialization(ExecutionResources resources, RuntimeException failure) {
        ScrappingComponent component = new ScrappingComponent() {
            @Override
            public QaRobotXmlImpl getQaRobotXml() {
                throw failure;
            }

            @Override
            public ExecutionResources getExecutionResources() {
                return resources;
            }
        };
        AtomicReference<MainViewModel> viewModelReference = new AtomicReference<>();
        QAMaster master = new QAMaster(null, () -> browser -> component, null, viewModelReference::get);
        QarobotWrapper wrapper = new QarobotWrapper();
        wrapper.setValidXml(true);
        wrapper.setDashboardExitFile(reports.toFile());
        master.setQarobot(wrapper);
        TestViewModel viewModel = new TestViewModel(master);
        viewModelReference.set(viewModel);
        viewModel.getSearchButton().onNext(false);
        viewModel.getStatusInitButton().onNext(false);
        viewModel.getPauseOrResumenStatus().onNext(true);
        return new Harness(master, viewModel);
    }

    private Harness harness(StubRobot... robots) {
        Queue<StubRobot> queue = new ArrayDeque<>(List.of(robots));
        AtomicReference<MainViewModel> viewModelReference = new AtomicReference<>();
        QAMaster master = new QAMaster(null, null, null, viewModelReference::get) {
            @Override
            public QaRobotXml getScrappingComponentProviderQaRobot() {
                return queue.remove();
            }
        };
        QarobotWrapper wrapper = new QarobotWrapper();
        wrapper.setValidXml(true);
        wrapper.setDashboardExitFile(reports.toFile());
        master.setQarobot(wrapper);
        TestViewModel viewModel = new TestViewModel(master);
        viewModelReference.set(viewModel);
        viewModel.getSearchButton().onNext(false);
        viewModel.getStatusInitButton().onNext(false);
        viewModel.getPauseOrResumenStatus().onNext(true);
        return new Harness(master, viewModel);
    }

    private record Harness(QAMaster master, TestViewModel viewModel) {
    }

    private static final class TestViewModel extends MainViewModel {
        private Throwable failure;

        private int successes;

        TestViewModel(QAMaster master) {
            super(master, new Configuracion());
        }

        @Override
        public void finishQa() {
            successes++;
            restartStatusButtons();
        }

        @Override
        public void finishQaWithError(Throwable error) {
            failure = error;
            restartStatusButtons();
        }
    }

    private static final class StubRobot implements QaRobotXml {
        private SendInfo2View sendInfo;

        private Runnable onFlux = () -> {
        };

        private Runnable onClose = () -> {
        };

        private boolean success = true;

        private boolean closed;

        private int pauses;

        @Override
        public void setQaRobot(QarobotWrapper wrapper) {
        }

        @Override
        public boolean flux() {
            onFlux.run();
            return success;
        }

        @Override
        public void pauseExecution() {
            assertFalse(closed);
            pauses++;
        }

        @Override
        public void resumenExecution() {
            assertFalse(closed);
        }

        @Override
        public void close() {
            onClose.run();
            closed = true;
        }

        @Override
        public void delegateSenders2SeleniumHelper(SendInfo2View info, ShowInDialog dialog,
                PuaseExecutionFromStopAction pause, SendQaContext2View context) {
            sendInfo = info;
        }
    }
}

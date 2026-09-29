package com.rebirth.qarobot.app.ui;

import com.formdev.flatlaf.FlatLaf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class UiThemeTest {
    @AfterEach
    void releaseListener() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UiTheme.shutdown();
            UiTheme.apply(UiTheme.Theme.LIGHT);
        });
    }

    @Test
    void detectsInitialThemeAndAppliesBackgroundNotificationsOnSwingThread() throws Exception {
        TestAppearance system = new TestAppearance(true);
        SwingUtilities.invokeAndWait(() -> {
            UiTheme.initialize(system);
            assertEquals(UiTheme.Theme.DARK, UiTheme.current());
            assertTrue(FlatLaf.isLafDark());
        });

        assertFalse(SwingUtilities.isEventDispatchThread());
        system.listener.accept(false);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(UiTheme.Theme.LIGHT, UiTheme.current());
            assertFalse(FlatLaf.isLafDark());
        });
        system.listener.accept(true);
        SwingUtilities.invokeAndWait(() -> assertTrue(FlatLaf.isLafDark()));
    }

    @Test
    void ignoresQueuedNotificationsFromAReplacedOrStoppedDetector() throws Exception {
        TestAppearance previous = new TestAppearance(true);
        TestAppearance replacement = new TestAppearance(false);
        SwingUtilities.invokeAndWait(() -> {
            UiTheme.initialize(previous);
            previous.listener.accept(true);
            UiTheme.initialize(replacement);
            assertNull(previous.listener);
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(UiTheme.Theme.LIGHT, UiTheme.current());
            replacement.listener.accept(true);
            UiTheme.shutdown();
            assertNull(replacement.listener);
        });
        SwingUtilities.invokeAndWait(() -> assertEquals(UiTheme.Theme.LIGHT, UiTheme.current()));
    }

    @Test
    void unchangedThemeDoesNotReinstallTheLookAndFeel() throws Exception {
        TestAppearance system = new TestAppearance(false);
        SwingUtilities.invokeAndWait(() -> UiTheme.initialize(system));
        var installed = UIManager.getLookAndFeel();
        system.listener.accept(false);
        SwingUtilities.invokeAndWait(() -> assertSame(installed, UIManager.getLookAndFeel()));
    }

    private static final class TestAppearance implements UiTheme.AppearanceSource {
        private final boolean dark;
        private Consumer<Boolean> listener;

        TestAppearance(boolean dark) { this.dark = dark; }
        @Override public boolean isDark() { return dark; }
        @Override public void addListener(Consumer<Boolean> callback) {
            assertNull(listener);
            listener = callback;
        }
        @Override public void removeListener(Consumer<Boolean> callback) {
            assertSame(listener, callback);
            listener = null;
        }
    }
}

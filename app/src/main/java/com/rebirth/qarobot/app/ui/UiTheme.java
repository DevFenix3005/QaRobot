package com.rebirth.qarobot.app.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

/** Installs the application theme before Swing components are constructed. */
@Slf4j
public final class UiTheme {
    public enum Theme {
        LIGHT, DARK
    }

    private static Theme current = Theme.LIGHT;
    private static AppearanceSource source;
    private static Consumer<Boolean> listener;
    private static boolean defaultsRegistered;
    private UiTheme() { }

    public static void initialize() {
        requireEdt();
        try {
            OsThemeDetector detector = OsThemeDetector.getDetector();
            initialize(new AppearanceSource() {
                @Override public boolean isDark() { return detector.isDark(); }
                @Override public void addListener(Consumer<Boolean> callback) { detector.registerListener(callback); }
                @Override public void removeListener(Consumer<Boolean> callback) { detector.removeListener(callback); }
            });
        } catch (RuntimeException | LinkageError e) {
            shutdown();
            log.warn("No se pudo detectar el tema del sistema; se usará el tema claro", e);
            apply(Theme.LIGHT);
        }
    }

    static void initialize(AppearanceSource appearance) {
        requireEdt();
        shutdown();
        source = appearance;
        listener = dark -> SwingUtilities.invokeLater(() -> {
            // Ignore notifications queued before shutdown or reinitialization.
            if (source == appearance) {
                Theme next = dark ? Theme.DARK : Theme.LIGHT;
                if (current != next) apply(next);
            }
        });
        appearance.addListener(listener);
        apply(appearance.isDark() ? Theme.DARK : Theme.LIGHT);
    }

    public static void shutdown() {
        requireEdt();
        AppearanceSource previous = source;
        Consumer<Boolean> previousListener = listener;
        source = null;
        listener = null;
        if (previous != null && previousListener != null) {
            try {
                previous.removeListener(previousListener);
            } catch (RuntimeException | LinkageError e) {
                log.warn("No se pudo liberar el detector del tema del sistema", e);
            }
        }
    }

    public static Theme current() { return current; }

    public static void apply(Theme theme) {
        requireEdt();
        if (!defaultsRegistered) {
            FlatLaf.registerCustomDefaultsSource("com.rebirth.qarobot.app.ui.themes");
            defaultsRegistered = true;
        }
        boolean installed = theme == Theme.DARK ? FlatDarkLaf.setup() : FlatLightLaf.setup();
        if (!installed) throw new IllegalStateException("No se pudo aplicar el tema de QaRobot");
        current = theme;
        FlatLaf.updateUI();
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("El tema debe actualizarse en el hilo de Swing");
        }
    }

    interface AppearanceSource {
        boolean isDark();
        void addListener(Consumer<Boolean> callback);
        void removeListener(Consumer<Boolean> callback);
    }
}

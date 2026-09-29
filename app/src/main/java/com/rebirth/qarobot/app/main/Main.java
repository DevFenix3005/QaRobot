package com.rebirth.qarobot.app.main;

import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.app.di.AppComponent;
import com.rebirth.qarobot.app.di.DaggerAppComponent;
import com.rebirth.qarobot.app.ui.UiTheme;
import com.rebirth.qarobot.app.cli.CliApplication;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;


@Log4j2
public class Main {

    private static AppComponent appComponent;

    public static void main(String[] args) {
        if (isCliInvocation(args)) {
            System.exit(new CliApplication().execute(args, System.out, System.err));
            return;
        }
        int timeout = 999;
        if (args.length > 0) {
            timeout = Integer.parseInt(args[0]);
        }
        appComponent = DaggerAppComponent.factory().create(timeout);
        SwingUtilities.invokeLater(Main::launchGui);
    }

    static boolean isCliInvocation(String[] args) {
        if (args.length == 0) return false;
        if (args.length == 1) {
            try {
                Integer.parseInt(args[0]);
                return false; // Preserve the legacy GUI timeout argument.
            } catch (NumberFormatException ignored) {
                return true;
            }
        }
        return true;
    }

    private static void launchGui() {
        if (appComponent != null) {
            try {
                UiTheme.initialize();
                JFrame mainFrame = appComponent.getMainFrame();
                mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                mainFrame.pack();
                Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                mainFrame.setSize(Math.min(mainFrame.getWidth(), available.width),
                        Math.min(mainFrame.getHeight(), available.height));
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
            } catch (RuntimeException e) {
                log.error("No se pudo iniciar QaRobot", e);
                JOptionPane.showMessageDialog(null, e.getMessage(), "No se pudo iniciar QaRobot", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}


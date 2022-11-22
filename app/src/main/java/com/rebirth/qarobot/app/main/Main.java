package com.rebirth.qarobot.app.main;

import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.app.di.AppComponent;
import com.rebirth.qarobot.app.di.DaggerAppComponent;

import javax.swing.*;


@Log4j2
public class Main {

    private static AppComponent appComponent;

    public static void main(String[] args) {
        int timeout = 999;
        if (args.length > 0) {
            timeout = Integer.parseInt(args[0]);
        }
        appComponent = DaggerAppComponent.factory().create(timeout);
        SwingUtilities.invokeLater(Main::launchGui);
    }

    private static void launchGui() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        if (appComponent != null) {
            JFrame mainFrame = appComponent.getMainFrame();
            mainFrame.setVisible(true);
            mainFrame.setLocationRelativeTo(null);
            mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            mainFrame.pack();
        }
    }

}


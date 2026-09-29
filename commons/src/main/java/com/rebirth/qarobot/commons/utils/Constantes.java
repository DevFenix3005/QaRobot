package com.rebirth.qarobot.commons.utils;

import com.google.common.base.StandardSystemProperty;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Constantes {

    public static final String RANDOM_LIST = "RANDOM_LIST";
    public static final String VALUE = "value";
    public static final byte[] DIGEST = new byte[0];
    private static final String QA_WORKPLACE = getWorkspaceHome().getPath();
    private static final String APP_WORKPLACE = getApplicationHome().getPath();
    public static final String XML_HOME = "xmlenium";
    public static final String SCRIPTS_HOME = "scripts";
    public static final String DASHBOARDS_OUTPUT = "dashboards";
    public static final String WEBDRIVERS_HOME = "webdrivers";
    public static final String TEMPLATES_HOME = "dashboardtemplate";
    public static final String AUTH_HOME = "auth";
    public static final File QA_WORKPLACE_DIR = new File(QA_WORKPLACE);
    public static final File XML_HOME_DIR = new File(QA_WORKPLACE, XML_HOME);
    public static final File SCRIPTS_HOME_DIR = new File(QA_WORKPLACE, SCRIPTS_HOME);
    public static final File DASHBOARDS_OUTPUT_DIR = new File(QA_WORKPLACE, DASHBOARDS_OUTPUT);
    public static final File WEBDRIVERS_HOME_DIR = new File(APP_WORKPLACE, WEBDRIVERS_HOME);
    public static final File TEMPLATES_HOME_DIR = new File(APP_WORKPLACE, TEMPLATES_HOME);
    public static final File AUTH_HOME_DIR = new File(APP_WORKPLACE, AUTH_HOME);

    public static File getWorkspaceHome() {
        String configured = System.getProperty("qarobot.workspace");
        return configured == null || configured.isBlank()
                ? new File(StandardSystemProperty.USER_HOME.value(), "QaRobotWorkplace")
                : new File(configured).getAbsoluteFile();
    }

    public static File getApplicationHome() {
        String configured = System.getProperty("qarobot.home");
        if (configured != null && !configured.isBlank()) {
            return new File(configured).getAbsoluteFile();
        }

        List<File> candidates = new ArrayList<>();
        try {
            File codeLocation = new File(Constantes.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (codeLocation.isFile()) {
                File libraryDirectory = codeLocation.getParentFile();
                candidates.add(libraryDirectory.getParentFile());
            }
        } catch (URISyntaxException e) {
            throw new IllegalStateException("No se pudo localizar la instalacion de QaRobot", e);
        }
        File workingDirectory = new File(StandardSystemProperty.USER_DIR.value());
        candidates.add(workingDirectory);
        candidates.add(new File(workingDirectory, "app"));
        for (File candidate : candidates) {
            if (new File(candidate, "dashboardtemplate/index.ftl").isFile()) {
                return candidate.getAbsoluteFile();
            }
        }
        return workingDirectory.getAbsoluteFile();
    }

    private Constantes() {
    }
}

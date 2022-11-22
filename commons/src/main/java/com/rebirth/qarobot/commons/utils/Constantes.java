package com.rebirth.qarobot.commons.utils;

import com.google.common.base.StandardSystemProperty;

import java.io.File;

public class Constantes {

    public static final String RANDOM_LIST = "RANDOM_LIST";
    public static final String VALUE = "value";
    public static final byte[] DIGEST = new byte[0];
    private static final String QA_WORKPLACE = StandardSystemProperty.USER_HOME.value() + File.separator + "QaRobotWorkplace";
    private static final String APP_WORKPLACE = StandardSystemProperty.USER_DIR.value();
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

    private Constantes() {
    }
}

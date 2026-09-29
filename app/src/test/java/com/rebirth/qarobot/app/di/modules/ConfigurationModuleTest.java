package com.rebirth.qarobot.app.di.modules;

import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.utils.Constantes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationModuleTest {
    @TempDir Path temp;
    private final String previousWorkspace = System.getProperty("qarobot.workspace");
    private final String previousHome = System.getProperty("qarobot.home");
    private final String previousWorkingDirectory = System.getProperty("user.dir");

    @AfterEach
    void restoreProperties() {
        restore("qarobot.workspace", previousWorkspace);
        restore("qarobot.home", previousHome);
        restore("user.dir", previousWorkingDirectory);
    }

    @Test
    void createsIsolatedWorkspaceAndUsesExplicitApplicationHome() {
        Path workspace = temp.resolve("nested/workspace");
        Path application = temp.resolve("installation");
        System.setProperty("qarobot.workspace", workspace.toString());
        System.setProperty("qarobot.home", application.toString());

        Configuracion configuration = ConfigurationModule.configuracionProvider(500);

        assertTrue(configuration.getXmlHome().isDirectory());
        assertTrue(configuration.getScriptsHome().isDirectory());
        assertTrue(configuration.getDashboardsOutputs().isDirectory());
        assertEquals(workspace.resolve("xmlenium").toFile(), configuration.getXmlHome());
        assertEquals(application.resolve("dashboardtemplate").toFile(), configuration.getDashboardtemplateHome());
        assertEquals(application.resolve("webdrivers").toFile(), configuration.getWebdriverHome());
        assertEquals(500, configuration.getBaseTimeout());
    }

    @Test
    void reportsAnUnusableWorkspaceWithoutExitingTheApplication() throws Exception {
        Path file = Files.writeString(temp.resolve("file"), "not a directory");
        System.setProperty("qarobot.workspace", file.toString());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ConfigurationModule.configuracionProvider(500));

        assertTrue(error.getMessage().contains(file.toString()));
    }

    @Test
    void findsPackagedTemplatesWhenLaunchedFromRepositoryRoot() throws Exception {
        Path app = temp.resolve("app");
        Files.createDirectories(app.resolve("dashboardtemplate"));
        Files.writeString(app.resolve("dashboardtemplate/index.ftl"), "report");
        // A source dashboard directory must not hide the compiled template under app/.
        Files.createDirectories(temp.resolve("dashboardtemplate"));
        System.clearProperty("qarobot.home");
        System.setProperty("user.dir", temp.toString());

        assertEquals(app.toFile(), Constantes.getApplicationHome());
    }

    private static void restore(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }
}

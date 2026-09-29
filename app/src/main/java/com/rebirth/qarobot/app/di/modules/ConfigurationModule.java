package com.rebirth.qarobot.app.di.modules;

import dagger.Module;
import dagger.Provides;
import org.apache.logging.log4j.Logger;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.utils.Constantes;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Module
public abstract class ConfigurationModule {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ConfigurationModule.class);

    private ConfigurationModule() {
    }

    @Provides
    @Singleton
    static Configuracion configuracionProvider(@Named("timeout") int timeout) {
        try {
            File workspace = Constantes.getWorkspaceHome();
            File applicationHome = Constantes.getApplicationHome();
            File[] appDirs = new File[]{
                    workspace,
                    new File(workspace, Constantes.XML_HOME),
                    new File(workspace, Constantes.DASHBOARDS_OUTPUT),
                    new File(workspace, Constantes.SCRIPTS_HOME),
            };

            for (File dir : appDirs) {
                Files.createDirectories(dir.toPath());
            }
            Configuracion configuracion = new Configuracion();
            configuracion.setXmlHome(appDirs[1]);
            configuracion.setDashboardsOutputs(appDirs[2]);
            configuracion.setScriptsHome(appDirs[3]);

            configuracion.setWebdriverHome(new File(applicationHome, Constantes.WEBDRIVERS_HOME));
            configuracion.setDashboardtemplateHome(new File(applicationHome, Constantes.TEMPLATES_HOME));

            configuracion.setBaseTimeout(timeout);

            return configuracion;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el espacio de trabajo de QaRobot: " + Constantes.getWorkspaceHome(), e);
        }
    }

}

package com.rebirth.qarobot.app.di.modules;

import com.google.common.io.Files;
import dagger.Module;
import dagger.Provides;
import org.apache.logging.log4j.Logger;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.utils.Constantes;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;

@Module
public abstract class ConfigurationModule {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ConfigurationModule.class);

    private ConfigurationModule() {
    }

    @Provides
    @Singleton
    static Configuracion configuracionProvider(@Named("timeout") int timeout) {
        try {
            File[] appDirs = new File[]{
                    Constantes.QA_WORKPLACE_DIR,
                    Constantes.XML_HOME_DIR,
                    Constantes.DASHBOARDS_OUTPUT_DIR,
                    Constantes.SCRIPTS_HOME_DIR,
            };
            Files.createParentDirs(appDirs[0]);

            for (File dir : appDirs) {
                if (!dir.exists() && dir.mkdir()) {
                    log.info("The dir {} was created!",dir.getName());
                }
            }
            Configuracion configuracion = new Configuracion();
            configuracion.setXmlHome(appDirs[1]);
            configuracion.setDashboardsOutputs(appDirs[2]);
            configuracion.setScriptsHome(appDirs[3]);

            configuracion.setWebdriverHome(Constantes.WEBDRIVERS_HOME_DIR);
            configuracion.setDashboardtemplateHome(Constantes.TEMPLATES_HOME_DIR);

            configuracion.setBaseTimeout(timeout);

            return configuracion;
        } catch (IOException e) {
            System.exit(-1);
            return null;
        }
    }

}

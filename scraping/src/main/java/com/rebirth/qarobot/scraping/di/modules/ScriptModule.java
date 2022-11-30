package com.rebirth.qarobot.scraping.di.modules;

import com.google.common.io.Files;
import dagger.Module;
import dagger.Provides;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Module()
public abstract class ScriptModule {

    private static final Logger log = LogManager.getLogger(ScriptModule.class);

    private ScriptModule() {
    }

    @Provides
    @ChildComponent
    public static ScriptEngineManager contextProvide() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new ScriptEngineManager(classLoader);
    }


    @Provides
    @ChildComponent
    public static ScriptEngine scopeProvide(ScriptEngineManager scriptEngineManager, Configuracion configuracion) {
        ScriptEngine graalEngine = scriptEngineManager.getEngineByName("graal.js");
        try {
            graalEngine.eval("var self = {};");
            File scriptFolder = configuracion.getScriptsHome();
            File[] scriptsArray = scriptFolder.listFiles();
            if (scriptsArray != null) {
                for (File file : scriptsArray) {
                    Reader reader = Files.newReader(file, StandardCharsets.UTF_8);
                    graalEngine.eval(reader);
                }
            }
        } catch (ScriptException e) {
            log.error("Error when we try to load the scripts", e);
            System.exit(-1);
        } catch (FileNotFoundException e) {
            log.error("Script don't exists", e);
            System.exit(-1);
        }
        return graalEngine;
    }

}

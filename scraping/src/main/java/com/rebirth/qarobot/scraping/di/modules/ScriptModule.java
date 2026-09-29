package com.rebirth.qarobot.scraping.di.modules;

import dagger.Module;
import dagger.Provides;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

@Module()
public abstract class ScriptModule {

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
        if (graalEngine == null) {
            throw new IllegalStateException("GraalJS is unavailable. Include the JavaScript engine in the runtime classpath.");
        }
        String source = "JavaScript initialization";
        try {
            graalEngine.eval("var self = {};");
            File scriptFolder = configuracion.getScriptsHome();
            // Libraries are optional; a fresh workspace may not have a scripts directory yet.
            if (scriptFolder == null || !scriptFolder.exists()) {
                return graalEngine;
            }
            source = scriptFolder.getAbsolutePath();
            File[] scriptsArray = scriptFolder.listFiles(file -> file.isFile()
                    && file.getName().toLowerCase(Locale.ROOT).endsWith(".js"));
            if (scriptsArray == null) {
                throw new IOException("Cannot read the JavaScript library directory: " + source);
            }
            Arrays.sort(scriptsArray, Comparator.comparing(File::getName));
            for (File file : scriptsArray) {
                source = file.getAbsolutePath();
                try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    graalEngine.eval(reader);
                }
            }
        } catch (ScriptException | IOException | RuntimeException e) {
            IllegalStateException failure = new IllegalStateException("Cannot load JavaScript from " + source, e);
            if (graalEngine instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            throw failure;
        }
        return graalEngine;
    }

}

package com.rebirth.qarobot.scraping.di.modules;

import dagger.Module;
import dagger.Provides;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.apache.logging.log4j.Logger;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;

import java.io.IOException;

@Module
public abstract class FreemarkerModule {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(FreemarkerModule.class);

    private FreemarkerModule() {
    }

    @Provides
    @ChildComponent
    static Configuration freemarkerProvider(Configuracion configuracion) {
        try {
            // Create your Configuration instance, and specify if up to what FreeMarker
            // version (here 2.3.29) do you want to apply the fixes that are not 100%
            // backward-compatible. See the Configuration JavaDoc for details.
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
            // Specify the source where the template files come from. Here I set a
            // plain directory for it, but non-file-system sources are possible too:
            cfg.setDirectoryForTemplateLoading(configuracion.getDashboardtemplateHome());
            // From here we will set the settings recommended for new projects. These
            // aren't the defaults for backward compatibilty.
            // Set the preferred charset template files are stored in. UTF-8 is
            // a good choice in most applications:
            cfg.setDefaultEncoding("ISO-8859-1");
            cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
            // Sets how errors will appear.
            // During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

            // Don't log exceptions inside FreeMarker that it will thrown at you anyway:
            cfg.setLogTemplateExceptions(false);

            // Wrap unchecked exceptions thrown during template processing into TemplateException-s:
            cfg.setWrapUncheckedExceptions(true);

            // Do not fall back to higher scopes when reading a null loop variable:
            cfg.setFallbackOnNullLoopVariable(false);

            return cfg;
        } catch (IOException ioException) {
            log.error("Read/Write exception...", ioException);
            return null;
        }
    }

    @Provides
    @ChildComponent
    static Template templateProvider(Configuration configuration) {
        Template template = null;
        try {
            template = configuration.getTemplate("index.ftl");
        } catch (IOException ioException) {
            log.error("Read/Write exception...", ioException);
        }
        return template;
    }


}

package com.rebirth.qarobot.app.di;

import com.rebirth.qarobot.app.di.modules.AppModule;
import com.rebirth.qarobot.app.di.modules.ConfigurationModule;
import com.rebirth.qarobot.app.di.modules.PatternsModule;
import com.rebirth.qarobot.app.di.modules.XmlReaderModule;
import com.rebirth.qarobot.app.utils.QaXmlReadService;
import com.rebirth.qarobot.scraping.di.ScrappingComponent;
import dagger.BindsInstance;
import dagger.Component;

import javax.inject.Named;
import javax.inject.Singleton;

/** Only requests the execution graph: no view models, windows or screen providers. */
@Singleton
@Component(modules = {AppModule.class, ConfigurationModule.class, PatternsModule.class, XmlReaderModule.class})
public interface CliComponent {
    QaXmlReadService xmlReader();
    ScrappingComponent.Factory scrapingFactory();

    @Component.Factory
    interface Factory {
        CliComponent create(@BindsInstance @Named("timeout") int timeout);
    }
}

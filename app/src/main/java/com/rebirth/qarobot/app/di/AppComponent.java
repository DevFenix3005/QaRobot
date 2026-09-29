package com.rebirth.qarobot.app.di;

import com.rebirth.qarobot.app.di.modules.AppModule;
import com.rebirth.qarobot.app.di.modules.ConfigurationModule;
import com.rebirth.qarobot.app.di.modules.PatternsModule;
import com.rebirth.qarobot.app.di.modules.UiModule;
import com.rebirth.qarobot.app.di.modules.ViewModelModule;
import com.rebirth.qarobot.app.di.modules.XmlReaderModule;
import dagger.BindsInstance;
import dagger.Component;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;

@Singleton
@Component(modules = {
        AppModule.class,
        ConfigurationModule.class,
        XmlReaderModule.class,
        PatternsModule.class,
        ViewModelModule.class,
        UiModule.class
})
public interface AppComponent {

    JFrame getMainFrame();

    @Component.Factory
    interface Factory {
        AppComponent create(@BindsInstance @Named("timeout") int timeout);
    }
}

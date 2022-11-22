package com.rebirth.qarobot.scraping.di.modules;

import dagger.Module;
import dagger.Provides;
import kong.unirest.Config;
import kong.unirest.UnirestInstance;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;

@Module
public interface RemoteModule {

    @Provides
    @ChildComponent
    static UnirestInstance unirestProvide() {
        Config config = new Config();
        config.connectTimeout(5000);
        return new UnirestInstance(config);
    }

}

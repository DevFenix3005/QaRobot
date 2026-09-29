package com.rebirth.qarobot.scraping.di.modules;

import dagger.Module;
import dagger.Provides;
import kong.unirest.Config;
import kong.unirest.UnirestInstance;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.scraping.utils.UnirestJacksonMapper;

@Module
public interface RemoteModule {

    @Provides
    @ChildComponent
    static UnirestInstance unirestProvide() {
        Config config = new Config();
        config.connectTimeout(5000);
        config.setObjectMapper(new UnirestJacksonMapper());
        return new UnirestInstance(config);
    }

}

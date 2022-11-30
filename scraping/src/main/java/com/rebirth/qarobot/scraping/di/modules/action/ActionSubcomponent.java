package com.rebirth.qarobot.scraping.di.modules.action;

import dagger.Subcomponent;

@Subcomponent(
        modules = {
                ActionModule.class,
        }
)
public interface ActionSubcomponent {

    @Subcomponent.Factory
    interface Factory {
        ActionSubcomponent create();
    }


}

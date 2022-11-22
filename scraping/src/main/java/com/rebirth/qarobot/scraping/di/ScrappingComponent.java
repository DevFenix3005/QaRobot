package com.rebirth.qarobot.scraping.di;

import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.scraping.di.modules.ActionModule;
import com.rebirth.qarobot.scraping.di.modules.DriverModule;
import com.rebirth.qarobot.scraping.di.modules.FreemarkerModule;
import com.rebirth.qarobot.scraping.di.modules.RemoteModule;
import com.rebirth.qarobot.scraping.di.modules.ScriptModule;
import com.rebirth.qarobot.scraping.enums.Browser;
import com.rebirth.qarobot.scraping.impl.QaRobotXmlImpl;
import dagger.BindsInstance;
import dagger.Subcomponent;

@ChildComponent()
@Subcomponent(modules = {
        RemoteModule.class,
        DriverModule.class,
        ActionModule.class,
        FreemarkerModule.class,
        //QaContextModule.class,
        ScriptModule.class
})
public interface ScrappingComponent {

    QaRobotXmlImpl getQaRobotXml();

    @Subcomponent.Factory
    interface Factory {
        ScrappingComponent create(@BindsInstance Browser browser);
    }


}

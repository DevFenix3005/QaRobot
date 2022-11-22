package com.rebirth.qarobot.record.di;

import com.rebirth.qarobot.record.di.modules.RecordModule;
import com.rebirth.qarobot.record.video.MyQAVideoRecorder;
import dagger.BindsInstance;
import dagger.Subcomponent;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;

import javax.inject.Named;
import java.awt.*;
import java.io.File;

@ChildComponent()
@Subcomponent(modules = {
        RecordModule.class,
})
public interface RecordComponent {

    MyQAVideoRecorder getMyQAVideoRecorder();

    @Subcomponent.Factory
    interface Factory {
        RecordComponent create(@BindsInstance @Named("movie folder") File movieFolder,
                               @BindsInstance @Named("selected screen") GraphicsDevice selectedScreen);
    }

}

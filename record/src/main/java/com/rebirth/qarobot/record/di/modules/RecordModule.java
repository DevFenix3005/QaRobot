package com.rebirth.qarobot.record.di.modules;

import dagger.Module;
import dagger.Provides;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.record.video.MyQAVideoRecorder;

import javax.inject.Named;
import java.awt.*;
import java.io.File;
import java.io.IOException;

@Module
public interface RecordModule {

    @ChildComponent()
    @Provides
    static MyQAVideoRecorder myQAVideoRecorderProvider(@Named("selected screen") GraphicsDevice selectedScreen,
                                                       @Named("movie folder") File movieFolder) {
        try {
            return new MyQAVideoRecorder(selectedScreen.getDefaultConfiguration(), movieFolder);
        } catch (IOException | AWTException e) {
            e.printStackTrace();
            return null;
        }
    }


}

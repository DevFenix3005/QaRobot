package com.rebirth.qarobot.app.di.modules;

import dagger.Binds;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import com.rebirth.qarobot.app.ui.MainFrame;
import com.rebirth.qarobot.app.ui.dialogs.EvaluatioContextDialog;
import com.rebirth.qarobot.app.ui.dialogs.QaDialog;
import com.rebirth.qarobot.app.ui.mainview.MyMainView;

import javax.inject.Singleton;
import javax.swing.*;

@Module
public abstract class UiModule {

    private UiModule() {
    }

    @Binds
    @Singleton
    public abstract JPanel bindsMyMainView(MyMainView myMainView);

    @Binds
    @Singleton
    public abstract JFrame bindsMainFrame(MainFrame mainFrame);

    @Provides
    @Singleton
    public static QaDialog provideQaDialog(Lazy<JFrame> mainFrame) {
        return new QaDialog(mainFrame.get());
    }

    @Provides
    @Singleton
    public static EvaluatioContextDialog provideEvaluatioContextDialog(Lazy<JFrame> mainFrame) {
        return new EvaluatioContextDialog(mainFrame.get());
    }

}

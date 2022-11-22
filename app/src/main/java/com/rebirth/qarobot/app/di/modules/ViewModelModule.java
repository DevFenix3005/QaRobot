package com.rebirth.qarobot.app.di.modules;

import dagger.Module;
import dagger.Provides;
import com.rebirth.qarobot.app.main.QAMaster;
import com.rebirth.qarobot.app.viewmodel.MainViewModel;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;

import javax.inject.Singleton;
import java.security.MessageDigest;

@Module
public interface ViewModelModule {

    @Provides()
    @Singleton
    static MainViewModel providesMainViewModel(QAMaster qaMaster, MessageDigest md5, Configuracion configuracion) {
        return new MainViewModel(qaMaster, md5, configuracion);
    }

}

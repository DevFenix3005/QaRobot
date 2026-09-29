package com.rebirth.qarobot.app.di.modules;

import javax.inject.Singleton;

import com.rebirth.qarobot.app.main.QAMaster;
import com.rebirth.qarobot.app.viewmodel.MainViewModel;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;

import dagger.Module;
import dagger.Provides;

@Module
public interface ViewModelModule {

    @Provides()
    @Singleton
    static MainViewModel providesMainViewModel(QAMaster qaMaster, Configuracion configuracion) {
        return new MainViewModel(qaMaster, configuracion);
    }

}

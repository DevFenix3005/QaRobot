package com.rebirth.qarobot.app.di.modules;

import java.awt.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Random;

import javax.inject.Singleton;

import com.rebirth.qarobot.app.utils.ComboBoxScreenModel;
import com.rebirth.qarobot.record.di.RecordComponent;
import com.rebirth.qarobot.scraping.di.ScrappingComponent;
import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

import dagger.Module;
import dagger.Provides;

@Module(subcomponents = {
        RecordComponent.class,
        ScrappingComponent.class
})
public abstract class AppModule {

    private AppModule() {
    }

    @Singleton
    @Provides
    public static Random randomProvider() {
        return new SecureRandom();
    }

    @Singleton
    @Provides
    public static Lorem loremProvider() {
        return LoremIpsum.getInstance();
    }

    @Singleton
    @Provides
    public static DecimalFormat decimalFormatProvider() {
        DecimalFormat df = new DecimalFormat();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
        df.setDecimalFormatSymbols(symbols);
        return df;
    }

    @Singleton
    @Provides
    public static GraphicsDevice[] graphicsDeviceProvider() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return ge.getScreenDevices();
    }

    @Singleton
    @Provides
    public static ComboBoxScreenModel comboBoxScreenModelProvider(GraphicsDevice[] graphicsDevices) {
        return new ComboBoxScreenModel(graphicsDevices);
    }

}

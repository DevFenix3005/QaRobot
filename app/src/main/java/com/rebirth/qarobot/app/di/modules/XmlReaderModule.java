package com.rebirth.qarobot.app.di.modules;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@Module
public interface XmlReaderModule {

    @Provides
    @Singleton()
    static DocumentBuilder documentBuilderProvider() {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try {
            builderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return null;
        }
    }

}

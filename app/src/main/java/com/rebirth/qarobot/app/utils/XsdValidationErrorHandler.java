package com.rebirth.qarobot.app.utils;

import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.models.dtos.QarobotWrapper;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

@Log4j2
public class XsdValidationErrorHandler extends DefaultHandler {

    private final QarobotWrapper qarobotWrapper;

    public XsdValidationErrorHandler(QarobotWrapper qarobotWrapper) {
        this.qarobotWrapper = qarobotWrapper;
    }

    @Override
    public void warning(SAXParseException e) {
        printInfo(e);
    }

    @Override
    public void error(SAXParseException e) {
        printInfo(e);
        qarobotWrapper.setValidXml(false);
    }

    @Override
    public void fatalError(SAXParseException e) {
        printInfo(e);
        qarobotWrapper.setValidXml(false);
    }

    private void printInfo(SAXParseException e) {
        log.info("Public ID: {}", e.getPublicId());
        log.info("System ID: {}", e.getSystemId());
        log.info("Line number: {}", e.getLineNumber());
        log.info("Column number: {}", e.getColumnNumber());
        log.info("Message: {}", e.getMessage());
        qarobotWrapper.addError(e.getLineNumber() + ":" + e.getColumnNumber() + ".-" + e.getMessage());
    }

}

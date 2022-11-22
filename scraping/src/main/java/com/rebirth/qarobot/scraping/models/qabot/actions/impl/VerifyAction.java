package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.math.DoubleMath;
import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.enums.ElementStatus;
import com.rebirth.qarobot.scraping.models.qabot.Value;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import com.rebirth.qarobot.scraping.models.qabot.rhinox.Rhinox;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.exceptions.NotFoundWebElement;
import com.rebirth.qarobot.commons.models.dtos.Verificador;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ChooseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SelectorType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.VerifyActionType;

import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public final class VerifyAction extends Action<VerifyActionType> {

    private final ScriptEngine scriptEngine;
    private final DecimalFormat decimalFormat;
    private boolean noExisteWebElement;

    @Inject
    public VerifyAction(SeleniumHelper seleniumHelper, ScriptEngine scriptEngine, DecimalFormat decimalFormat) {
        super(seleniumHelper);
        this.scriptEngine = scriptEngine;
        this.decimalFormat = decimalFormat;
    }

    @Override
    public void run() {
        this.seleniumHelper.actionLog(this.actionDto);
        if (this.actionDto.isSkip()) {
            Verificador verificador = Verificador.create(this.actionDto);
            verificador.setRule("Omitido [" + this.actionDto.getDesc() + "]");
            verificador.setEvaluado("IGNORADO");
            verificador.setResultado("IGNORADO");
            verificador.setOk(false);
            verificador.setSkip(true);
            logVerificador(verificador);
            seleniumHelper.sendAction2View(this.actionDto, Color.YELLOW, null);
        } else {
            beforeExecute();
            delayPreExecutor();
            execute();
            afterExecute();
        }
    }

    @Override
    public void beforeExecute() {
        try {
            super.beforeExecute();

            List<SelectorType> selectorTypeList = this.actionDto.getSelector();
            if (selectorTypeList.isEmpty()) {
                noExisteWebElement = true;
            } else {
                this.element = this.seleniumHelper.getWebElement(this.actionDto);
                noExisteWebElement = false;
            }
        } catch (NotFoundWebElement notFoundWebElement) {
            String descripcion = this.actionDto.getDesc();
            List<String> selectorsInStringList = notFoundWebElement.getSelectors()
                    .stream()
                    .map(s -> s.getBy() + " : " + s.getValue()).toList();

            String selectorsInString = Joiner.on('\n').skipNulls().join(selectorsInStringList);
            boolean negativa = actionDto.isNegative();
            Verificador verificador = Verificador.create(this.actionDto);
            verificador.setRule("Elemento no encontrado[" + descripcion + "]" + (negativa ? "Prueba Negativa" : ""));
            verificador.setEvaluado("Selectores usados: " + selectorsInString);
            verificador.setResultado("----");
            verificador.setOk(negativa);
            verificador.setSkip(false);
            logVerificador(verificador);
            noExisteWebElement = true;
        }
    }

    @Override
    public void execute() {
        switch (this.actionDto.getKind()) {
            case HASCHILDS -> {
                if (noExisteWebElement) return;
                hasChildsVerification();
            }
            case CHILDS -> {
                if (noExisteWebElement) return;
                tableVerification();
            }
            case SELECT -> {
                if (noExisteWebElement) return;
                selectVerification();
            }
            case ELEMENT -> {
                if (noExisteWebElement) return;
                fieldVerification();
            }
            case SCRIPT -> scriptVerification();
        }
    }

    @Override
    public void afterExecute() {
        log.info("Se termino el flujo de la accion " + this.getClass().getSimpleName() + " con la informacion con el id: " + this.actionDto.getId());
        this.element = null;
        this.actionDto = null;
        this.noExisteWebElement = false;
    }

    private void hasChildsVerification() {
        String value = this.actionDto.getValue();
        value = Objects.isNull(value) ? "0" : value;
        boolean isNegativeTest = this.actionDto.isNegative();
        String validation;
        boolean valueIsContained = false;

        WebElement webElementParent = this.seleniumHelper.getWebElement(this.actionDto);
        List<WebElement> webElementsChilds = webElementParent.findElements(By.xpath("./child::*"));
        int webElementsSize = webElementsChilds.size();
        if (webElementsSize > 0) {
            try {
                int quantity = Integer.parseInt(value);
                validation = "Cantidad de elementos es igual o mayor que " + quantity;
                valueIsContained = webElementsSize >= quantity;
            } catch (NumberFormatException numberFormatException) {
                List<String> values = Arrays.asList(value.split(","));
                validation = "Entre los elementos existen los valores " + Joiner.on(", ").join(values);
                for (WebElement webElement : webElementsChilds) {
                    String innerText = webElement.getText();
                    valueIsContained |= values.contains(innerText);
                }
            }
        } else {
            validation = "EL elemento no contiene hijos";
        }
        addNegativePrefix2Validation(value, valueIsContained, isNegativeTest, validation);
    }


    private void tableVerification() {
        String value = this.actionDto.getValue();
        boolean isNegativeTest = this.actionDto.isNegative();
        String validation;
        boolean valueIsContained = false;

        List<WebElement> webElements = this.seleniumHelper.getWebElements(this.actionDto.getId(), this.actionDto.getSelector());
        int webElementsSize = webElements.size();

        if (webElementsSize > 0) {
            try {
                int quantity = Integer.parseInt(value);
                validation = "Cantidad de elementos es igual o mayor que " + quantity;
                valueIsContained = webElementsSize >= quantity;
            } catch (NumberFormatException numberFormatException) {
                List<String> values = Arrays.asList(value.split(","));
                validation = "Entre los elementos existen los valores " + Joiner.on(", ").join(values);
                for (WebElement webElement : webElements) {
                    String innerText = webElement.getText();
                    valueIsContained |= values.contains(innerText);
                }
            }
        } else {
            validation = "EL elemento no contiene hijos";
        }
        addNegativePrefix2Validation(value, valueIsContained, isNegativeTest, validation);
    }

    private void selectVerification() {
        String value = this.actionDto.getValue();
        boolean isNegativeTest = this.actionDto.isNegative();
        String validation;
        boolean valueIsContained = false;
        try {
            ChooseActionType chooseActionType = new ChooseActionType();
            chooseActionType.setId(this.actionDto.getId());
            chooseActionType.setDesc(this.actionDto.getDesc());
            chooseActionType.setTimeout(this.actionDto.getTimeout());
            chooseActionType.setOrder(this.actionDto.getOrder());
            chooseActionType.setSkip(this.actionDto.isSkip());
            chooseActionType.setValue(this.actionDto.getValue());
            chooseActionType.getSelector().addAll(this.actionDto.getSelector());
            this.seleniumHelper.setValueToVadiinsUglyDropdown(chooseActionType);
            validation = "Esta opcion si existe en el select";
            valueIsContained = true;
        } catch (NotFoundWebElement e) {
            validation = "Esta opcion no existe en el select";
        }
        addNegativePrefix2Validation(value, valueIsContained, isNegativeTest, validation);
    }

    private void addNegativePrefix2Validation(String value, boolean valueIsContained, boolean isNegativeTest, String validation) {
        if (isNegativeTest)
            validation = "Prueba Negativa[" + validation + "]";
        Verificador verificador = Verificador.create(this.actionDto);
        verificador.setRule(validation);
        verificador.setEvaluado(value);
        verificador.setResultado("-----");
        verificador.setOk(Boolean.logicalXor(valueIsContained, isNegativeTest));
        logVerificador(verificador);
    }

    private void fieldVerification() {
        String estatusVerification = this.actionDto.getElementStatus();
        List<ElementStatus> enumsElementStatus = Arrays.stream(estatusVerification.split("\\|"))
                .map(ElementStatus::valueOf)
                .toList();

        Map<ElementStatus, Verificador> verificadorMap = Maps.newEnumMap(ElementStatus.class);
        Verificador verificador = null;
        for (ElementStatus status : enumsElementStatus) {
            switch (status) {
                case ENABLE -> {
                    verificador = this.fieldEnableVerification();
                    verificadorMap.put(ElementStatus.ENABLE, verificador);
                }
                case DISPLAYED -> {
                    verificador = fieldDisplayedVerification();
                    verificadorMap.put(ElementStatus.DISPLAYED, verificador);
                }
                case CONTENT -> {
                    verificador = fieldContentVerification();
                    verificadorMap.put(ElementStatus.CONTENT, verificador);
                }
            }
        }

        if (verificadorMap.size() > 1) {
            createComplexVerification(verificadorMap);
        } else if (verificador != null && verificadorMap.size() == 1) {
            logVerificador(verificador);
        }
    }

    private void createComplexVerification(Map<ElementStatus, Verificador> verificadorMap) {
        String concatElementStatus = Joiner.on(",").join(verificadorMap.keySet());
        Verificador verificacionCompuesta = Verificador.create(this.actionDto);
        verificadorMap.forEach((k, v) -> verificacionCompuesta.addVerificacion(v));
        verificacionCompuesta.setEvaluado(concatElementStatus);
        verificacionCompuesta.setResultado((verificacionCompuesta.isOk() ? "" : "'NO' ") + "Pasaron las reglas compuestas");
        verificacionCompuesta.setDescVerifyAccion("Verificacion compuesta de " + concatElementStatus + ", descripcion:" + this.actionDto.getDesc());
        logVerificador(verificacionCompuesta);
    }

    private Verificador fieldEnableVerification() {
        Verificador verificador = Verificador.create(this.actionDto);
        boolean negative = this.actionDto.isNegative();
        boolean enable = this.element.isEnabled();
        verificador.setEvaluado("Habilitado en el Html");
        verificador.setResultado(enable ? "Si" : "No");
        verificador.setOk(Boolean.logicalXor(enable, negative));
        verificador.setRule("Elemento habilitado");
        return verificador;
    }

    private Verificador fieldDisplayedVerification() {
        Verificador verificador = Verificador.create(this.actionDto);
        boolean negative = this.actionDto.isNegative();
        boolean display = this.element.isDisplayed();
        verificador.setEvaluado("Se muestra en el html");
        verificador.setResultado(display ? "Si" : "No");
        verificador.setOk(Boolean.logicalXor(display, negative));
        verificador.setRule("Elemento visible");
        return verificador;
    }

    private Verificador fieldContentVerification() {
        Verificador verificador = Verificador.create(this.actionDto);
        boolean negative = this.actionDto.isNegative();
        String value = this.actionDto.getValue();
        Value valueObj = this.seleniumHelper.processValue(value, this.element);
        boolean resultado = valueObj.initEvaluation();
        verificador.setEvaluado(valueObj.getActualValue());
        verificador.setResultado(valueObj.getContent());
        verificador.setOk(Boolean.logicalXor(resultado, negative));
        verificador.setRule(valueObj.descripcion(negative));
        return verificador;
    }


    private void scriptVerification() {

        Verificador scriptVerificador = Verificador.create(this.actionDto);
        scriptVerificador.setRule("Validacion por Script!");
        scriptVerificador.setScript(this.actionDto.getScript());

        try {
            Rhinox rhinox = new Rhinox(this.scriptEngine);
            rhinox.addProperties2Scope(this.seleniumHelper.getContextMap());
            Object res = rhinox.runScript(this.actionDto);
            boolean verificationFlag;
            String evaluado;
            String resultadoScript;

            if (noExisteWebElement) {
                evaluado = "Validacion en el script";
                resultadoScript = "Validacion en el script";
                verificationFlag = Boolean.parseBoolean(res.toString());
            } else {
                String evalValue = this.seleniumHelper.getValueFromWebElement(this.actionDto);
                double evaluationFieldValue = this.numberFormat(evalValue).doubleValue();
                double resultScriptValue = Double.parseDouble(res.toString());

                evaluado = Double.toString(evaluationFieldValue);
                resultadoScript = Double.toString(resultScriptValue);
                verificationFlag = DoubleMath.fuzzyEquals(evaluationFieldValue, resultScriptValue, this.actionDto.getTolerance());
            }

            scriptVerificador.setEvaluado(evaluado);
            scriptVerificador.setResultado(resultadoScript);
            scriptVerificador.setOk(verificationFlag);

        } catch (ParseException pe) {
            log.error("QaRobot#verifyAction::parseEx", pe);
            scriptVerificador.setEvaluado("ParseErr!");
            scriptVerificador.setResultado(pe.getMessage());
            scriptVerificador.setOk(false);
        } catch (ScriptException scriptException) {
            log.info(scriptException.getMessage());
            log.info("Column error {}", scriptException.getColumnNumber());
            log.info("File error {}", scriptException.getFileName());
            log.error("QaRobot#verifyAction::parseEx", scriptException);
            scriptVerificador.setEvaluado("ScriptException!");
            scriptVerificador.setResultado(scriptException.getMessage());
            scriptVerificador.setOk(false);
        }
        logVerificador(scriptVerificador);
    }

    private Number numberFormat(String number) throws ParseException {
        return this.decimalFormat.parse(number);
    }

    private void logVerificador(Verificador verificador) {
        Color color;
        if (verificador.isOk()) {
            log.info("VERIFICACION DE LA ACCION {} PASO", this.actionDto.getDesc());
            color = Color.GREEN;
        } else {
            log.warn("VERIFICACION DE LA ACCION {} NO PASO", this.actionDto.getDesc());
            color = Color.RED;
        }

        seleniumHelper.sendAction2View(this.actionDto, color, null);
        this.seleniumHelper.addVerificacion2Context(verificador);
    }


}

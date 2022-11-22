package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import com.rebirth.qarobot.scraping.models.qabot.rhinox.Rhinox;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ScriptActionType;

import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Objects;

@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public class ScriptAction extends Action<ScriptActionType> {

    private final ScriptEngine scriptEngine;

    @Inject
    public ScriptAction(SeleniumHelper seleniumHelper,
                        ScriptEngine scriptEngine) {
        super(seleniumHelper);
        this.scriptEngine = scriptEngine;
    }

    @Override
    public void beforeExecute() {
        super.beforeExecute();
    }

    @Override
    public void execute() {
        try {
            Rhinox rhinox = new Rhinox(this.scriptEngine);
            rhinox.addProperties2Scope(this.seleniumHelper.getContextMap());
            String id = actionDto.getId();
            String verifyScript = actionDto.getBody();
            String setterKeyName = actionDto.getSet();
            String resultado = rhinox.runScript(id, verifyScript).toString();

            if (Objects.nonNull(setterKeyName)) {
                this.seleniumHelper.addValue2Contexto(setterKeyName, resultado);
            }
        } catch (ScriptException scriptException) {
            log.info(scriptException.getMessage());
            log.info("Column error {}", scriptException.getColumnNumber());
            log.info("File error {}", scriptException.getFileName());
            log.error("QaRobot#verifyAction::parseEx", scriptException);
        }


    }

    @Override
    public void afterExecute() {
        super.afterExecute();
    }
}

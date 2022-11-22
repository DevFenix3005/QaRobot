package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ReadActionType;

import javax.inject.Inject;

@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public final class ReadAction extends Action<ReadActionType> {

    @Inject
    public ReadAction(SeleniumHelper seleniumHelper) {
        super(seleniumHelper);
    }

    @Override
    public void execute() {
        String value = this.seleniumHelper.getValueFromWebElement(this.actionDto);
        this.seleniumHelper.addValue2Contexto(this.actionDto.getSet(), value);
    }

    @Override
    public void afterExecute() {
        super.afterExecute();
        log.info("Se termino el " + this.getClass().getName());
    }
}

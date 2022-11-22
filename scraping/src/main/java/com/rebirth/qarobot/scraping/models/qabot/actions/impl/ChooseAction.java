package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ChooseActionType;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;

import javax.inject.Inject;

@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public final class ChooseAction extends Action<ChooseActionType> {

    @Inject
    public ChooseAction(SeleniumHelper seleniumHelper) {
        super(seleniumHelper);
    }

    @Override
    public void beforeExecute() {
        super.beforeExecute();
        this.element = this.seleniumHelper.getWebElement(this.actionDto);
    }

    @Override
    public void execute() {
        this.seleniumHelper.setValueToVadiinsUglyDropdown(this.actionDto);
    }

}

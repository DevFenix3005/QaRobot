package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.qarobot.DelayActionType;

import javax.inject.Inject;


@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public final class DelayAction extends Action<DelayActionType> {

    @Inject
    public DelayAction(SeleniumHelper seleniumHelper) {
        super(seleniumHelper);
    }


    @Override
    public void execute() {
        this.seleniumHelper.delay(actionDto.getTimeout().longValue());
    }

}

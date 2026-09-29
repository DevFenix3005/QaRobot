package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import com.rebirth.qarobot.scraping.utils.InterpolationResult;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.qarobot.OpenActionType;

import javax.inject.Inject;

@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public final class OpenAction extends Action<OpenActionType> {

    @Inject
    protected OpenAction(SeleniumHelper seleniumHelper) {
        super(seleniumHelper);
    }

    @Override
    public void execute() {
        String url = this.actionDto.getUrl();
        InterpolationResult interpolation = this.seleniumHelper.getInterpolationOfValueIfExistsOrGetRawValue(url);
        this.seleniumHelper.goToUrl(interpolation.getValue());
    }
}

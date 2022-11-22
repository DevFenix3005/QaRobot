package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import com.rebirth.qarobot.scraping.utils.InterpolationResult;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.Keys;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.qarobot.WriteActionType;

import javax.inject.Inject;
import java.util.Objects;


@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public final class WriteAction extends Action<WriteActionType> {

    @Inject
    public WriteAction(SeleniumHelper seleniumHelper) {
        super(seleniumHelper);
    }

    @Override
    public void execute() {
        this.element = this.seleniumHelper.getWebElement(this.actionDto);
        String value = this.actionDto.getValue();
        InterpolationResult interpolationResult = this.seleniumHelper.getInterpolationOfValueIfExistsOrGetRawValue(value);

        if (this.actionDto.isLazy()) {
            element.sendKeys(interpolationResult.getValue());
        } else {
            this.element.sendKeys(Keys.CONTROL, "A", Keys.DELETE);
            this.element.sendKeys(interpolationResult.getValue());
            if (this.actionDto.isEnter()) {
                this.element.sendKeys(Keys.ENTER);
            }
        }

        String setValue2ContextWithThisKey = this.actionDto.getSet();
        if (Objects.nonNull(setValue2ContextWithThisKey)) {
            this.seleniumHelper.addValue2Contexto(setValue2ContextWithThisKey, interpolationResult.getValue());
        }


    }

}

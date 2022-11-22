package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.impl.SeleniumHelperImpl;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.interactions.Actions;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ClickActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.KindOfClick;

import javax.inject.Inject;


@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public final class ClickAction extends Action<ClickActionType> {

    @Inject
    public ClickAction(SeleniumHelper seleniumHelper) {
        super(seleniumHelper);
    }

    @Override
    public void beforeExecute() {
        super.beforeExecute();
        this.element = seleniumHelper.getWebElement(this.actionDto);
    }

    @Override
    public void execute() {

        Actions actions = new Actions(((SeleniumHelperImpl) this.seleniumHelper).getDriver());

        if (this.actionDto.getClick() == KindOfClick.SINGLE) {
            actions.click(element);
        } else if (this.actionDto.getClick() == KindOfClick.DOUBLE) {
            actions.doubleClick(element);
        }

        try {
            actions.perform();
        } catch (ElementClickInterceptedException exception) {
            log.warn("Why can't click to this element: " + this.actionDto.getId(), exception);
        }
    }

}

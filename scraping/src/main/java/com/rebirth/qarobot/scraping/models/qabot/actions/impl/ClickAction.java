package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.impl.SeleniumHelperImpl;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
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
    public void execute() {

        if (this.actionDto.getClick() == KindOfClick.SINGLE) {
            element.click();
        } else if (this.actionDto.getClick() == KindOfClick.DOUBLE) {
            new Actions(((SeleniumHelperImpl) this.seleniumHelper).getDriver())
                    .doubleClick(element).perform();
        }
    }

}

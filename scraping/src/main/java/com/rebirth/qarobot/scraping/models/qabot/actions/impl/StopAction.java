package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.exceptions.StopActionException;
import com.rebirth.qarobot.commons.models.dtos.qarobot.StopActionType;

import javax.inject.Inject;
import java.awt.*;


@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public final class StopAction extends Action<StopActionType> {

    @Inject
    public StopAction(SeleniumHelper seleniumHelper) {
        super(seleniumHelper);
    }

    @Override
    public void execute() throws StopActionException {
        if (this.actionDto.isKill()) {
            log.info("Se termino el flujo de la accion " + this.getClass().getSimpleName() + " con la informacion con el id: " + this.actionDto.getId());
            seleniumHelper.sendAction2View(this.actionDto, Color.GREEN, null);
            throw new StopActionException(this.actionDto);
        } else {
            this.seleniumHelper.pauseFromAction();
        }
    }
}

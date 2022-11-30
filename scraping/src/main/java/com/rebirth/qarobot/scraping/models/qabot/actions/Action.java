package com.rebirth.qarobot.scraping.models.qabot.actions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.WebElement;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionTypeWithSelectorAndTimeOut;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionTypeWithTimeout;
import com.rebirth.qarobot.scraping.SeleniumHelper;

import java.awt.*;

@Data
@Log4j2
@EqualsAndHashCode(callSuper = true)
public abstract class Action<T extends BaseActionType> extends ActionLifeCycle<T> {

    protected final SeleniumHelper seleniumHelper;

    protected WebElement element;
    protected T actionDto;

    protected Action(SeleniumHelper seleniumHelper) {
        this.seleniumHelper = seleniumHelper;
    }

    @Override
    public void setAction(T action) {
        this.actionDto = action;
    }

    @Override
    public void run() {
        this.seleniumHelper.actionLog(this.actionDto);
        if (!this.actionDto.isSkip()) {
            beforeExecute();
            delayPreExecutor();
            execute();
            afterExecute();
        } else {
            seleniumHelper.sendAction2View(this.actionDto, Color.YELLOW, null);
        }
    }

    @Override
    public void delayPreExecutor() {
        if (actionDto instanceof BaseActionTypeWithTimeout baseActionTypeWithTimeout) {
            log.info("Retardo en la ejecucion de la accion");
            this.seleniumHelper.delay((baseActionTypeWithTimeout).getTimeout().longValue());
        }
        if (actionDto instanceof BaseActionTypeWithSelectorAndTimeOut baseActionTypeWithSelectorAndTimeOut) {
            log.info("Buscando elemento");
            this.element = this.seleniumHelper.getWebElement(baseActionTypeWithSelectorAndTimeOut);
        }

    }

    @Override
    public void beforeExecute() {
        log.info("<Action id=\"{}\" typo=\"{}\" />", this.actionDto.getId(), this.actionDto.getClass().getSimpleName());
    }

    @Override
    public void afterExecute() {
        log.info("Se termino el flujo de la accion " + this.getClass().getSimpleName() + " con la informacion con el id: " + this.actionDto.getId());
        seleniumHelper.sendAction2View(this.actionDto, Color.GREEN, null);
        this.element = null;
        this.actionDto = null;
    }

    public void setActionasd(BaseActionType baseActionDto) {

    }
}

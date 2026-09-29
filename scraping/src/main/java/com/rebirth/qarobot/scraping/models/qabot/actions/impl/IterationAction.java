package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.exceptions.StopActionException;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.IterationActionType;
import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import com.rebirth.qarobot.scraping.utils.ActionRunner;
import com.rebirth.qarobot.scraping.utils.InterpolationResult;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import javax.inject.Provider;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public class IterationAction
        extends Action<IterationActionType> {

    private final Provider<Map<Class<? extends BaseActionType>, Action<? extends BaseActionType>>> providerActionMap;

    @Inject
    protected IterationAction(SeleniumHelper seleniumHelper,
                              Provider<Map<Class<? extends BaseActionType>, Action<? extends BaseActionType>>> providerActionMap) {
        super(seleniumHelper);
        this.providerActionMap = providerActionMap;
    }

    @Override
    public void execute() {
        String action = this.actionDto.getTimes();
        BigInteger timeout = this.actionDto.getTimeout();
        List<BaseActionType> actions = this.actionDto.getOpenOrDelayOrStop();

        InterpolationResult interpolation = this.seleniumHelper.getInterpolationOfValueIfExistsOrGetRawValue(action);
        String value = interpolation.getValue();
        int times = 1;
        try {
            times = Integer.parseInt(value);
        } catch (NumberFormatException numberFormatException) {
            log.info("NumberFormatException", numberFormatException);
        }

        for (int i = 0; i < times; i++) {
            this.seleniumHelper.addValue2Contexto("index", i);
            for (BaseActionType baseActionType : actions) {
                this.seleniumHelper.addValue2Contexto("iterationAction", baseActionType.getClass().getSimpleName());
                this.seleniumHelper.addValue2Contexto("iterationActionDesc", baseActionType.getDesc());
                this.seleniumHelper.sendQaContet2View();
                baseActionType.setIterationChild(true);
                ActionRunner actionRunner = new ActionRunner(baseActionType, providerActionMap.get());
                actionRunner.run();
            }
            this.seleniumHelper.delay(timeout.longValue());
        }
        this.actionDto.setDesc("FIN ITERACION X %d ciclos : %s".formatted(times, this.actionDto.getDesc()));
        this.seleniumHelper.actionLog(this.actionDto);
    }


}

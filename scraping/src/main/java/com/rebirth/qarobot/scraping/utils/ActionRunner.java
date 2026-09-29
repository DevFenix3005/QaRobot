package com.rebirth.qarobot.scraping.utils;

import com.rebirth.qarobot.commons.exceptions.StopActionException;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;

import java.util.Map;

public class ActionRunner implements Runnable {

    private final Map<Class<? extends BaseActionType>, Action<? extends BaseActionType>> actionMap;
    private final BaseActionType baseActionDto;

    public ActionRunner(
            BaseActionType baseActionDto,
            Map<Class<? extends BaseActionType>, Action<? extends BaseActionType>> actionMap) {
        this.baseActionDto = baseActionDto;
        this.actionMap = actionMap;
    }

    @Override
    public void run() {
        Action<? extends BaseActionType> action = this.runLifeCycle();
        try {
            action.run();
        } catch (StopActionException stop) {
            // An explicit stop is control flow, not a failed test.
            throw stop;
        } catch (RuntimeException | AssertionError failure) {
            action.getSeleniumHelper().captureFailure(baseActionDto, failure);
            throw failure;
        }
    }

    @SuppressWarnings(value = "unchecked")
    private <T extends BaseActionType> Action<T> runLifeCycle() throws StopActionException {
        Class<T> klassActionDto = (Class<T>) baseActionDto.getClass();
        Action<T> fetchedAction = (Action<T>) actionMap.get(klassActionDto);
        fetchedAction.setAction((T) baseActionDto);
        return fetchedAction;
    }


}

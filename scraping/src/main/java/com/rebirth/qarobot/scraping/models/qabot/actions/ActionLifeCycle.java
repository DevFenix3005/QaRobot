package com.rebirth.qarobot.scraping.models.qabot.actions;

import org.apache.logging.log4j.Logger;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;

public abstract class ActionLifeCycle<T extends BaseActionType> implements Runnable {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ActionLifeCycle.class);

    public abstract void setAction(T action);

    public void delayPreExecutor() {
        log.info("Esta accion no soporta el delay");
    }

    public void beforeExecute() {
        throw new UnsupportedOperationException("ActionLifeCycle::beforeExecute no implemented yet");
    }

    public void execute() {
        throw new UnsupportedOperationException("ActionLifeCycle::execute no implemented yet");

    }

    public void afterExecute() {
        throw new UnsupportedOperationException("ActionLifeCycle::afterExecute no implemented yet");
    }

}

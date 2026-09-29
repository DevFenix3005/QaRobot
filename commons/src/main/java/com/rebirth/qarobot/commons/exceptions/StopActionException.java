package com.rebirth.qarobot.commons.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.rebirth.qarobot.commons.models.dtos.qarobot.StopActionType;

@EqualsAndHashCode(callSuper = true)
@Data
public class StopActionException extends RuntimeException {
    private final transient StopActionType stopActionType;

    public StopActionException(StopActionType stopActionType) {
        this.stopActionType = stopActionType;
    }
}

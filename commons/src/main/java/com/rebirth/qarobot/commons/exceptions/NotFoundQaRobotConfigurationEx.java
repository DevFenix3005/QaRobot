package com.rebirth.qarobot.commons.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class NotFoundQaRobotConfigurationEx extends Exception {
    private final String mensaje;

    public NotFoundQaRobotConfigurationEx(String message, Throwable cause) {
        super(message, cause);
        this.mensaje = message;
    }
}

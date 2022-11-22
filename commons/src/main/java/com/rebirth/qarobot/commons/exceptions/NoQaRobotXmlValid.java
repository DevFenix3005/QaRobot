package com.rebirth.qarobot.commons.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class NoQaRobotXmlValid extends RuntimeException {

    private final List<String> errores;

    public NoQaRobotXmlValid(List<String> errores) {
        super("Error en la validacion con el XSD");
        this.errores = errores;
    }
}

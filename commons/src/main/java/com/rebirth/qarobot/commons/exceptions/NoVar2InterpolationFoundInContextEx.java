package com.rebirth.qarobot.commons.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class NoVar2InterpolationFoundInContextEx extends RuntimeException {

    private String varname;
    private String action;
    private String webElement;
    private String webElementPath;
    private String id;


    public NoVar2InterpolationFoundInContextEx(String varname) {
        super(processMsg2Exception(varname));
    }

    private static String processMsg2Exception(String varname) {
        return "La variable " + varname + " no fue encontrada en el contexto de la aplicacion";
    }

}

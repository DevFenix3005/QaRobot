package com.rebirth.qarobot.commons.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SelectorType;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class NotFoundWebElement extends RuntimeException {
    private final String id;
    private final transient List<SelectorType> selectors;
    private final String mensaje;

    public NotFoundWebElement(String id, String mensaje, List<SelectorType> selectors) {
        super(mensaje);
        this.id = id;
        this.mensaje = mensaje;
        this.selectors = selectors;
    }
}

package com.rebirth.qarobot.commons.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class NotFoundQaXmlFile extends Exception {
    private final String desc;
    private final String file;

    public NotFoundQaXmlFile(String desc, String file) {
        this.desc = desc;
        this.file = file;
    }


    public NotFoundQaXmlFile(String message, Throwable cause, String desc, String file) {
        super(message, cause);
        this.desc = desc;
        this.file = file;
    }
}

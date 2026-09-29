package com.rebirth.qarobot.commons.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChaiAssertionError extends RuntimeException {

    private final String assertion;

    public ChaiAssertionError(String message) {
        super(message);
        this.assertion = message;
    }
}

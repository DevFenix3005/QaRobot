package com.rebirth.qarobot.commons.models.dtos.toggle;

public enum PauseOrResumeState {

    PAUSE("Pausar"),
    RESUME("Resumen"),
    NONE("NONE");

    private final String literalValue;

    private PauseOrResumeState(String literalValue) {
        this.literalValue = literalValue;
    }

    public String getLiteralValue() {
        return literalValue;
    }
}

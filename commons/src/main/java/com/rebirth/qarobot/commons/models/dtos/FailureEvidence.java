package com.rebirth.qarobot.commons.models.dtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** A snapshot taken at the failing step, before later actions change the browser. */
@Data
public class FailureEvidence {
    private String id;
    private String actionId;
    private String description;
    private String actionType;
    private String capturedAt;
    private String url;
    private String selectorUsed;
    private String exceptionType;
    private String message;
    private String screenshotPath;
    private String detailsPath;
    private List<String> selectors = new ArrayList<>();
    private List<String> captureProblems = new ArrayList<>();
    private List<Verificador> verifications = new ArrayList<>();
}

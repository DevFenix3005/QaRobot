package com.rebirth.qarobot.scraping.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "create")
public class InterpolationResult {
    private String key;
    private String value;
    private boolean interpolation;
}

package com.rebirth.qarobot.scraping.di.annotations;

import dagger.MapKey;
import com.rebirth.qarobot.commons.di.enums.PatternEnum;

@MapKey
public @interface PatternKey {
    PatternEnum value();
}

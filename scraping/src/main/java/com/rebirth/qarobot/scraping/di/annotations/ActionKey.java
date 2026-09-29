package com.rebirth.qarobot.scraping.di.annotations;

import dagger.MapKey;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@MapKey()
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ActionKey {
    Class<? extends BaseActionType> value();
}

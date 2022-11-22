package com.rebirth.qarobot.commons.di.annotations.keys;

import dagger.MapKey;
import com.rebirth.qarobot.commons.di.enums.MapperType;

@MapKey
public @interface MappersKeys {
    MapperType value();
}

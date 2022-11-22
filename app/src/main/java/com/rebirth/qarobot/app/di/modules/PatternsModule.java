package com.rebirth.qarobot.app.di.modules;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import com.rebirth.qarobot.scraping.di.annotations.PatternKey;
import com.rebirth.qarobot.commons.di.enums.PatternEnum;

import java.util.regex.Pattern;

@Module
public interface PatternsModule {


    @Provides
    @IntoMap
    @PatternKey(PatternEnum.STRING_PATTERN)
    static Pattern randomStringPattern() {
        return Pattern.compile("^(?<label>RANDOM_STRING)(?<dots>:)(?<quantity>\\d+|NAME)$");
    }

    @Provides
    @IntoMap
    @PatternKey(PatternEnum.INTEGER_PATTERN)
    static Pattern randomIntegerPattern() {
        return Pattern.compile("^(?<label>RANDOM_INTEGER)(?<dots>:)(?<q1>\\d+)(?<opq2>(?<slash>-)(?<q2>\\d+))?(?<optimes>(?<times>X)(?<timesval>\\d+))?$");
    }

    @Provides
    @IntoMap
    @PatternKey(PatternEnum.ATTRIBUTE_PATTERN)
    static Pattern attributesPattern() {
        return Pattern.compile("^(?<label>attr):(?<key>[\\w\\-.]+)((?<eqq>([=@]))(?<value>.+))?$");
    }

    @Provides
    @IntoMap
    @PatternKey(PatternEnum.INTERPOLATION_PATTERN)
    static Pattern interpolationPattern() {
        return Pattern.compile("^(?<interpolation>\\$\\{(?<value>[\\w\\-.]+)})$");
    }

    @Provides
    @IntoMap
    @PatternKey(PatternEnum.VERIFYELEMENT_PATTERN)
    static Pattern verifyElementPattern() {
        return Pattern.compile("^(?<label>attr|text)(?<attribute>:[\\w\\-.]+)?(?<eqq>=)(?<logic>eq|contain|gt|lt|ge|le|noempty)(?<payload>(?<dot>\\.)(?<content>[{}$\\w-.]+))?$");
    }

    @Provides
    @IntoMap
    @PatternKey(PatternEnum.NUMBER_PATTERN)
    static Pattern numberPattern() {
        return Pattern.compile("^\\d+$");
    }

    @Provides
    @IntoMap
    @PatternKey(PatternEnum.MONEY_PATTERN)
    static Pattern moneyPattern() {
        return Pattern.compile("^[0-9,.]+$");
    }


}

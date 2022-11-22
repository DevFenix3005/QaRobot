package com.rebirth.qarobot.scraping.enums;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public enum MyLogicSimbols {
    EQ,
    NOEMPTY,
    CONTAIN,
    GT,
    LT,
    GE,
    LE;

    private static final Map<MyLogicSimbols, Function<String[], Boolean>> FUNCTION_HASH_MAP = new EnumMap<>(MyLogicSimbols.class);

    static {
        FUNCTION_HASH_MAP.put(MyLogicSimbols.EQ, array -> {
            String value0 = array[0];
            String value1 = array[1];
            try {
                double actualValue = Double.parseDouble(value0);
                double evalDouble = Double.parseDouble(value1);
                return actualValue == evalDouble;
            } catch (NumberFormatException ex) {
                return value0.equals(value1);
            }
        });
        FUNCTION_HASH_MAP.put(MyLogicSimbols.NOEMPTY, array -> !array[0].isEmpty());
        FUNCTION_HASH_MAP.put(MyLogicSimbols.CONTAIN, array -> array[0].contains(array[1]));
        FUNCTION_HASH_MAP.put(MyLogicSimbols.GT, array -> {
            try {
                double actualValue = Double.parseDouble(array[0]);
                double evalDouble = Double.parseDouble(array[1]);
                return actualValue > evalDouble;
            } catch (NumberFormatException ex) {
                throw new RuntimeException();
            }
        });
        FUNCTION_HASH_MAP.put(MyLogicSimbols.LT, array -> {
            try {
                double actualValue = Double.parseDouble(array[0]);
                double evalDouble = Double.parseDouble(array[1]);
                return actualValue < evalDouble;
            } catch (NumberFormatException ex) {
                throw new RuntimeException();
            }
        });
        FUNCTION_HASH_MAP.put(MyLogicSimbols.GE, array -> {
            try {
                double actualValue = Double.parseDouble(array[0]);
                double evalDouble = Double.parseDouble(array[1]);
                return actualValue >= evalDouble;
            } catch (NumberFormatException ex) {
                throw new RuntimeException();
            }
        });
        FUNCTION_HASH_MAP.put(MyLogicSimbols.LE, array -> {
            try {
                double actualValue = Double.parseDouble(array[0]);
                double evalDouble = Double.parseDouble(array[1]);
                return actualValue <= evalDouble;
            } catch (NumberFormatException ex) {
                throw new RuntimeException();
            }
        });
    }

    public boolean runLogic(String actualValue, String evalValue) {
        return FUNCTION_HASH_MAP.get(this).apply(new String[]{actualValue, evalValue});
    }

}

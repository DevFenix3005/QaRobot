package com.rebirth.qarobot.commons.models.dtos;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rebirth.qarobot.commons.utils.PausableExecutor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;

import javax.inject.Inject;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Data()
@Log4j2
@ChildComponent
public class QaRobotContext {

    private List<Verificador> verificadores = Lists.newArrayList();
    private Map<String, Object> mapContainer = Maps.newTreeMap();
    private AtomicReference<Boolean> validacionGlobal = new AtomicReference<>(Boolean.TRUE);
    private AtomicReference<BaseActionType> currentAction = new AtomicReference<>();
    private AtomicLong currentTime = new AtomicLong();
    private PausableExecutor pausableExecutor = new PausableExecutor(1, Executors.defaultThreadFactory());

    private final Pattern moneyPattern;
    private final Pattern numberPattern;
    private final DecimalFormat decimalFormat;

    @Inject
    public QaRobotContext(Map<PatternEnum, Pattern> patternEnumPatternMap, DecimalFormat decimalFormat) {
        this.moneyPattern = patternEnumPatternMap.get(PatternEnum.MONEY_PATTERN);
        this.numberPattern = patternEnumPatternMap.get(PatternEnum.NUMBER_PATTERN);
        this.decimalFormat = decimalFormat;
    }

    public void addVerificador(Verificador verificador) {
        if (Objects.isNull(this.verificadores)) {
            this.verificadores = Lists.newArrayList();
        }
        this.verificadores.add(verificador);
    }

    public void addValue(String key, Object value) {
        String valor = value.toString();
        if (numberPattern.matcher(valor).matches()) {
            this.mapContainer.put(key, Integer.parseInt(valor));
        } else if (moneyPattern.matcher(valor).matches()) {
            try {
                this.mapContainer.put(key, numberFormat(valor));
            } catch (ParseException e) {
                log.error("ParseException::QaRobotContext", e);
            }
        } else {
            this.mapContainer.put(key, valor);
        }
    }

    private Number numberFormat(String number) throws ParseException {
        return this.decimalFormat.parse(number);
    }

    public Object getValue(String key) {
        return this.mapContainer.get(key);
    }

    public boolean allValidacionesOK() {
        if (this.verificadores == null) return true;
        List<Verificador> verificacionesSinSkips = this.verificadores.stream()
                .filter(v -> !v.isSkip())
                .toList();

        if (verificacionesSinSkips.isEmpty()) return true;
        else return verificacionesSinSkips.stream().allMatch(Verificador::isOk);
    }

    public Future<Object> submitCallableInExecutor(Callable<Object> callable) {
        return pausableExecutor.submit(callable);
    }

    public void pauseExecutor() {
        this.pausableExecutor.pause();
    }

    public void resumenExecutor() {
        this.pausableExecutor.resume();
    }

    public void shuwdownExecutor() {
        this.pausableExecutor.shutdown();
    }

    public void reset() {
        this.verificadores = Lists.newArrayList();
        this.mapContainer = Maps.newHashMap();
        this.validacionGlobal = new AtomicReference<>(Boolean.TRUE);
        this.currentAction = new AtomicReference<>();
        this.currentTime = new AtomicLong();
        this.pausableExecutor = new PausableExecutor(1, Executors.defaultThreadFactory());
    }
}

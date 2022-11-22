package com.rebirth.qarobot.scraping.utils.predicates;

import com.rebirth.qarobot.commons.models.dtos.Verificador;

import java.util.function.Predicate;

public class SkipPredicate implements Predicate<Verificador> {
    @Override
    public boolean test(Verificador verificador) {
        return verificador.isSkip();
    }
}

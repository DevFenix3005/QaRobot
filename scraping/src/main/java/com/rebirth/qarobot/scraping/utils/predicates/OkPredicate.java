package com.rebirth.qarobot.scraping.utils.predicates;

import com.rebirth.qarobot.commons.models.dtos.Verificador;

import java.util.function.Predicate;

public class OkPredicate implements Predicate<Verificador> {
    @Override
    public boolean test(Verificador verificador) {
        return verificador.isOk();
    }
}

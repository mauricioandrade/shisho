package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Kata;

import java.util.List;

public final class Katas {

    private Katas() {
    }

    /** Every kata, in a fixed order so reports are reproducible. */
    public static List<Kata> all() {
        return List.of(
                new FieldInjectionKata(),
                new LombokConstructorKata(),
                new NonFinalDependencyKata(),
                new SystemOutKata(),
                new ValueOutsideConstructorKata(),
                new RequestBodyWithoutValidKata(),
                new DtoNotRecordKata(),
                new EntityInControllerKata(),
                new TransactionalReadOnlyKata());
    }
}

package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.AvoidedDependency;
import dev.mauricioandrade.shisho.domain.model.Kata;

import java.util.List;

public final class Katas {

    private Katas() {
    }

    /**
     * Every kata, in a fixed order so reports are reproducible.
     *
     * @param avoided the catalog's dependencies to avoid
     */
    public static List<Kata> all(List<AvoidedDependency> avoided) {
        return List.of(
                new FieldInjectionKata(),
                new LombokConstructorKata(),
                new NonFinalDependencyKata(),
                new SystemOutKata(),
                new SwallowedExceptionKata(),
                new ValueOutsideConstructorKata(),
                new RequestBodyWithoutValidKata(),
                new DtoNotRecordKata(),
                new EntityInControllerKata(),
                new EntityWithLombokDataKata(),
                new EntityWithoutDefaultConstructorKata(),
                new EagerCollectionKata(),
                new ToOneWithoutLazyKata(),
                new TransactionalReadOnlyKata(),
                new UnproxyableMethodKata(),
                new TransactionalControllerKata(),
                new DestructiveSchemaGenerationKata(),
                new SqlLoggingKata(),
                new HardcodedSecretKata(),
                new OpenInViewKata(),
                new MissingControllerAdviceKata(),
                new MissingActuatorKata(),
                new MissingSpringdocKata(),
                new AvoidedDependencyKata(avoided),
                new ConfigurationPropertiesNotRecordKata(),
                new PreferConfigurationPropertiesKata(),
                new PreferVirtualThreadsKata(),
                new PreferScopedValuesKata(),
                new QuarkusPrivateInjectionKata());
    }
}

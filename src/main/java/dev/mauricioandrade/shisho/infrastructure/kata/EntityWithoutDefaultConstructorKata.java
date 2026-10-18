package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;

/**
 * A JPA {@code @Entity} or {@code @Embeddable} class that declares constructors, none of them
 * without parameters, and has no Lombok {@code @NoArgsConstructor}: the provider cannot
 * instantiate it when loading it from the database. A record is left out: Hibernate 6.2 maps
 * records as embeddables through their canonical constructor.
 */
final class EntityWithoutDefaultConstructorKata implements ClassKata {

    private static final Set<String> MAPPED = Set.of("Entity", "Embeddable");
    private static final Set<String> NO_ARGS = Set.of("NoArgsConstructor");

    @Override
    public String id() {
        return "entity-without-default-constructor";
    }

    @Override
    public Severity severity() {
        return Severity.GRAVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        boolean lacksDefault = unit.kind() == ClassUnit.Kind.CLASS
                && Annotations.has(unit.annotations(), MAPPED)
                && !Annotations.has(unit.annotations(), NO_ARGS)
                && !unit.constructors().isEmpty()
                && unit.constructors().stream().noneMatch(constructor -> constructor.parameters().isEmpty());
        return lacksDefault ? List.of(lesson(unit, unit.line(), unit.name())) : List.of();
    }
}

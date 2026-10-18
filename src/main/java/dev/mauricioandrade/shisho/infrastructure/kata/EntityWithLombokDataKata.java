package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;

/**
 * A JPA {@code @Entity} whose equals and hashCode Lombok generates from every field: {@code @Data},
 * or a bare {@code @EqualsAndHashCode}. One that names its fields ({@code of},
 * {@code onlyExplicitlyIncluded}) made a choice and is left alone.
 */
final class EntityWithLombokDataKata implements ClassKata {

    private static final Set<String> ENTITY = Set.of("Entity");
    private static final Set<String> ALL_FIELDS = Set.of("Data", "EqualsAndHashCode");

    @Override
    public String id() {
        return "entity-with-lombok-data";
    }

    @Override
    public Severity severity() {
        return Severity.MEDIA;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        if (!Annotations.has(unit.annotations(), ENTITY)) {
            return List.of();
        }
        return unit.annotations().stream()
                .filter(annotation -> ALL_FIELDS.contains(annotation.simpleName()) && annotation.attributes().isEmpty())
                .map(annotation -> lesson(unit, annotation.line(), unit.name(), annotation.simpleName()))
                .toList();
    }
}

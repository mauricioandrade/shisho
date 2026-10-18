package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Kind;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** A class, not a record, annotated with {@code @ConfigurationProperties}: bound through setters. */
final class ConfigurationPropertiesNotRecordKata implements ClassKata {

    private static final Set<String> CONFIGURATION_PROPERTIES = Set.of("ConfigurationProperties");

    @Override
    public String id() {
        return "configuration-properties-not-record";
    }

    @Override
    public Severity severity() {
        return Severity.LEVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return new JavaVersion(16);
    }

    @Override
    public Optional<String> javaFeature() {
        return Optional.of("records");
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        if (unit.kind() != Kind.CLASS) {
            return List.of();
        }
        return Annotations.find(unit.annotations(), CONFIGURATION_PROPERTIES)
                .map(annotation -> lesson(unit, annotation.line(), unit.name()))
                .stream()
                .toList();
    }
}

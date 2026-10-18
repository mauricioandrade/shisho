package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;

/**
 * Lombok generating the constructor that declares a bean's dependencies (Spring or CDI). On a class
 * that is not a bean, such as a DTO with {@code @Data}, the constructor holds data, not
 * dependencies: whether it should be a record is {@code dto-not-record}'s question.
 */
final class LombokConstructorKata implements ClassKata {

    private static final Set<String> LOMBOK_CONSTRUCTORS = Set.of("RequiredArgsConstructor", "AllArgsConstructor");

    @Override
    public String id() {
        return "lombok-constructor";
    }

    @Override
    public Severity severity() {
        return Severity.MEDIA;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.BASELINE;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        if (!Annotations.has(unit.annotations(), Annotations.BEANS)) {
            return List.of();
        }
        return unit.annotations().stream()
                .filter(annotation -> LOMBOK_CONSTRUCTORS.contains(annotation.simpleName()))
                .map(annotation -> lesson(unit, annotation.line(), annotation.simpleName()))
                .toList();
    }
}

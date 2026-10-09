package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;

/** Lombok generating the constructor that declares the class's dependencies. */
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
        return unit.annotations().stream()
                .filter(annotation -> LOMBOK_CONSTRUCTORS.contains(annotation.simpleName()))
                .map(annotation -> lesson(unit, annotation.line(), annotation.simpleName()))
                .toList();
    }
}

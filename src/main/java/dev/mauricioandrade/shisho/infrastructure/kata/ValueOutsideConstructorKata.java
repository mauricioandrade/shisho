package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** Spring's {@code @Value} on a field, a method or a method parameter instead of a constructor parameter. */
final class ValueOutsideConstructorKata implements ClassKata {

    private static final Set<String> VALUE = Set.of("Value");

    @Override
    public String id() {
        return "value-outside-constructor";
    }

    @Override
    public Severity severity() {
        return Severity.LEVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.BASELINE;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        Stream<Lesson> fields = unit.fields().stream()
                .flatMap(field -> lessonFor(unit, field.annotations(), field.name()));
        Stream<Lesson> methods = unit.methods().stream()
                .flatMap(method -> Stream.concat(
                        lessonFor(unit, method.annotations(), method.name()),
                        method.parameters().stream()
                                .flatMap(parameter -> lessonFor(unit, parameter.annotations(), parameter.name()))));
        return Stream.concat(fields, methods).toList();
    }

    private Stream<Lesson> lessonFor(ClassUnit unit, List<Annotation> annotations, String member) {
        Optional<Annotation> value = Annotations.find(annotations, VALUE);
        return value.map(annotation -> lesson(unit, annotation.line(), member)).stream();
    }
}

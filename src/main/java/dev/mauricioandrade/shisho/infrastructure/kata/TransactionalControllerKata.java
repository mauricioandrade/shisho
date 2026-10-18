package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@code @Transactional} on a Spring {@code @Controller} or {@code @RestController}, on the class
 * or on a method. A Jakarta REST resource (Quarkus) is left out: there, a transactional resource
 * method is the idiom the Quarkus guides teach.
 */
final class TransactionalControllerKata implements ClassKata {

    private static final Set<String> CONTROLLER = Set.of("Controller", "RestController");
    private static final Set<String> TRANSACTIONAL = Set.of("Transactional");

    @Override
    public String id() {
        return "transactional-controller";
    }

    @Override
    public Severity severity() {
        return Severity.LEVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        if (!Annotations.has(unit.annotations(), CONTROLLER)) {
            return List.of();
        }
        Stream<Lesson> onClass = Annotations.find(unit.annotations(), TRANSACTIONAL).stream()
                .map(annotation -> lesson(unit, annotation.line(), unit.name()));
        Stream<Lesson> onMethods = unit.methods().stream()
                .flatMap(method -> Annotations.find(method.annotations(), TRANSACTIONAL).stream())
                .map(annotation -> lesson(unit, annotation.line(), unit.name()));
        return Stream.concat(onClass, onMethods).toList();
    }
}

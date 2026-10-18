package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Lombok generating the constructor that declares a bean's dependencies (Spring or CDI). On a class
 * that is not a bean, such as a DTO with {@code @Data}, the constructor holds data, not
 * dependencies: whether it should be a record is {@code dto-not-record}'s question. The project's
 * own stereotypes make beans too, so the kata needs the whole project.
 */
final class LombokConstructorKata implements ProjectKata {

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
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ProjectModel project) {
        Set<String> beans = Annotations.beanAnnotations(project.classes());
        return project.classes().stream()
                .filter(unit -> Annotations.has(unit.annotations(), beans))
                .flatMap(this::evaluate)
                .toList();
    }

    private Stream<Lesson> evaluate(ClassUnit unit) {
        return unit.annotations().stream()
                .filter(annotation -> LOMBOK_CONSTRUCTORS.contains(annotation.simpleName()))
                .map(annotation -> lesson(unit.file(), annotation.line(), annotation.simpleName()));
    }
}

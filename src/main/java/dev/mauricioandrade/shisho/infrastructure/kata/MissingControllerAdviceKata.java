package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;

/** A project with controllers and no {@code @ControllerAdvice}; the lesson points at the first controller. */
final class MissingControllerAdviceKata implements ProjectKata {

    private static final Set<String> CONTROLLER = Set.of("Controller", "RestController");
    private static final Set<String> ADVICE = Set.of("ControllerAdvice", "RestControllerAdvice");

    @Override
    public String id() {
        return "missing-controller-advice";
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
    public List<Lesson> evaluate(ProjectModel project) {
        if (project.classes().stream().anyMatch(unit -> Annotations.has(unit.annotations(), ADVICE))) {
            return List.of();
        }
        return project.classes().stream()
                .filter(unit -> Annotations.has(unit.annotations(), CONTROLLER))
                .findFirst()
                .map(controller -> lesson(controller.file(), controller.line(), controller.name()))
                .stream()
                .toList();
    }
}

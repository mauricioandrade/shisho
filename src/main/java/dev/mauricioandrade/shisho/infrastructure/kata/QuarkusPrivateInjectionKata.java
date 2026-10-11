package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;

/**
 * A private {@code @Inject} or {@code @ConfigProperty} field in a Quarkus application: ArC fills
 * package-private fields with generated code, and needs reflection for private ones.
 */
final class QuarkusPrivateInjectionKata implements ProjectKata {

    private static final Set<String> INJECTED = Set.of("Inject", "ConfigProperty");

    @Override
    public String id() {
        return "quarkus-private-injection";
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
    public List<Lesson> evaluate(ProjectModel project) {
        if (project.build().filter(QuarkusApplications::isQuarkusApplication).isEmpty()) {
            return List.of();
        }
        return project.classes().stream().flatMap(unit -> evaluate(unit).stream()).toList();
    }

    private List<Lesson> evaluate(ClassUnit unit) {
        return unit.fields().stream()
                .filter(ClassUnit.Field::isPrivate)
                .flatMap(field -> Annotations.find(field.annotations(), INJECTED)
                        .map(annotation -> lesson(unit.file(), annotation.line(), field.name()))
                        .stream())
                .toList();
    }
}

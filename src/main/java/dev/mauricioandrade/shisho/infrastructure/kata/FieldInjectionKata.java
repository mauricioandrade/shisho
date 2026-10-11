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
 * {@code @Autowired} (or {@code @Inject}) on a field. In a Quarkus application, {@code @Inject} on a
 * field is the documented style, so only {@code @Autowired} counts there.
 */
final class FieldInjectionKata implements ProjectKata {

    private static final Set<String> AUTOWIRED = Set.of("Autowired");

    @Override
    public String id() {
        return "field-injection";
    }

    @Override
    public Severity severity() {
        return Severity.GRAVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.BASELINE;
    }

    @Override
    public List<Lesson> evaluate(ProjectModel project) {
        Set<String> injection = project.build().filter(QuarkusApplications::isQuarkusApplication).isPresent()
                ? AUTOWIRED
                : Annotations.INJECTION;
        return project.classes().stream().flatMap(unit -> evaluate(unit, injection).stream()).toList();
    }

    private List<Lesson> evaluate(ClassUnit unit, Set<String> injection) {
        return unit.fields().stream()
                .flatMap(field -> Annotations.find(field.annotations(), injection)
                        .map(annotation -> lesson(unit.file(), annotation.line(), field.name()))
                        .stream())
                .toList();
    }
}

package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.Comparator;
import java.util.Optional;

/**
 * A {@code @Service} with at least {@value #MINIMUM_METHODS} methods or
 * {@value #MINIMUM_DEPENDENCIES} constructor dependencies: everything ends up in it.
 */
final class GodServiceDetector implements Detector {

    static final int MINIMUM_METHODS = 20;
    static final int MINIMUM_DEPENDENCIES = 8;

    @Override
    public String id() {
        return "service_deus";
    }

    @Override
    public Optional<Evidence> detect(ProjectModel project) {
        return project.classes().stream()
                .filter(unit -> unit.isAnnotatedWith("Service"))
                .filter(unit -> unit.methods().size() >= MINIMUM_METHODS || dependencies(unit) >= MINIMUM_DEPENDENCIES)
                .max(Comparator.comparingInt((ClassUnit unit) -> unit.methods().size() + dependencies(unit))
                        .thenComparing(ClassUnit::name, Comparator.reverseOrder()))
                .flatMap(unit -> evidence(unit.name(), unit.methods().size(), dependencies(unit)));
    }

    private static int dependencies(ClassUnit unit) {
        return unit.constructors().stream().mapToInt(constructor -> constructor.parameters().size()).max().orElse(0);
    }
}

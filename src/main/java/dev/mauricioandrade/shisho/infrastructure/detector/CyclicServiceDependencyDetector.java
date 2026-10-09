package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Two {@code @Service}s that depend on each other, through constructors or fields. */
final class CyclicServiceDependencyDetector implements Detector {

    @Override
    public String id() {
        return "dependencias_ciclicas_entre_services";
    }

    @Override
    public Optional<Evidence> detect(ProjectModel project) {
        Map<String, Set<String>> dependencies = project.classes().stream()
                .filter(unit -> unit.isAnnotatedWith("Service"))
                .collect(Collectors.toMap(ClassUnit::name, Types::dependencies, (first, _) -> first));
        return dependencies.keySet().stream().sorted()
                .flatMap(service -> dependencies.get(service).stream().sorted()
                        .filter(other -> service.compareTo(other) < 0)
                        .filter(other -> dependencies.getOrDefault(other, Set.of()).contains(service))
                        .map(other -> new Evidence(id(), List.of(service, other))))
                .findFirst();
    }
}

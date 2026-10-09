package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Method;
import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@code *Mapper} method converting one project type into another with exactly the same
 * fields: a copy that adds a class and no meaning.
 */
final class MapperBetweenIdenticalStructuresDetector implements Detector {

    @Override
    public String id() {
        return "mappers_entre_estruturas_identicas";
    }

    @Override
    public Optional<Evidence> detect(ProjectModel project) {
        Map<String, ClassUnit> byName = project.classes().stream()
                .collect(Collectors.toMap(ClassUnit::name, Function.identity(), (first, _) -> first));
        return project.classes().stream()
                .filter(unit -> unit.name().endsWith("Mapper"))
                .flatMap(mapper -> mapper.methods().stream())
                .filter(method -> method.parameters().size() == 1)
                .flatMap(method -> identicalPair(method, byName))
                .findFirst();
    }

    private Stream<Evidence> identicalPair(Method method, Map<String, ClassUnit> byName) {
        ClassUnit source = byName.get(method.parameters().getFirst().type());
        ClassUnit target = byName.get(method.returnType());
        if (source == null || target == null || source == target) {
            return Stream.empty();
        }
        List<String> state = source.stateNames();
        boolean identical = !state.isEmpty() && new HashSet<>(state).equals(new HashSet<>(target.stateNames()));
        return identical ? Stream.of(new Evidence(id(), List.of(source.name(), target.name()))) : Stream.empty();
    }
}

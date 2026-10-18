package dev.mauricioandrade.shisho.infrastructure.concept;

import dev.mauricioandrade.shisho.domain.model.ClassUnit.Kind;
import dev.mauricioandrade.shisho.domain.model.ConceptDetector;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding.Occurrence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An interface of the project with {@value #MINIMUM_IMPLEMENTATIONS} or more implementations in
 * it: interchangeable behaviors behind one contract.
 */
final class StrategyDetector implements ConceptDetector {

    static final int MINIMUM_IMPLEMENTATIONS = 2;

    @Override
    public String id() {
        return "estrategia";
    }

    @Override
    public List<Occurrence> find(ProjectModel project) {
        Map<String, Long> implementations = implementations(project);
        return project.classes().stream()
                .filter(unit -> unit.kind() == Kind.INTERFACE)
                .filter(contract -> implementations.getOrDefault(contract.name(), 0L) >= MINIMUM_IMPLEMENTATIONS)
                .map(contract -> new Occurrence(contract.file(), contract.line()))
                .toList();
    }

    /** How many classes name each supertype, counted once for the whole project. */
    private static Map<String, Long> implementations(ProjectModel project) {
        return project.classes().stream()
                .filter(unit -> unit.kind() != Kind.INTERFACE)
                .flatMap(unit -> unit.supertypes().stream().distinct())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}

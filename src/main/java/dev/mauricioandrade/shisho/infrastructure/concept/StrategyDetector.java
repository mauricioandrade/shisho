package dev.mauricioandrade.shisho.infrastructure.concept;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Kind;
import dev.mauricioandrade.shisho.domain.model.ConceptDetector;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding.Occurrence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.List;

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
        return project.classes().stream()
                .filter(unit -> unit.kind() == Kind.INTERFACE)
                .filter(contract -> implementations(project, contract) >= MINIMUM_IMPLEMENTATIONS)
                .map(contract -> new Occurrence(contract.file(), contract.line()))
                .toList();
    }

    private static long implementations(ProjectModel project, ClassUnit contract) {
        return project.classes().stream()
                .filter(unit -> unit.kind() != Kind.INTERFACE && unit.supertypes().contains(contract.name()))
                .count();
    }
}

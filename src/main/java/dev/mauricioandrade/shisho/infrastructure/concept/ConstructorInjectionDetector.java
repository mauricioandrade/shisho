package dev.mauricioandrade.shisho.infrastructure.concept;

import dev.mauricioandrade.shisho.domain.model.ClassUnit.Kind;
import dev.mauricioandrade.shisho.domain.model.ConceptDetector;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding.Occurrence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.List;
import java.util.Set;

/** A Spring bean whose constructor receives what it needs: dependency injection the recommended way. */
final class ConstructorInjectionDetector implements ConceptDetector {

    private static final Set<String> BEANS = Set.of("Service", "Component", "Repository", "Controller", "RestController",
            "Configuration", "RestControllerAdvice", "ControllerAdvice");

    @Override
    public String id() {
        return "injecao_por_construtor";
    }

    @Override
    public List<Occurrence> find(ProjectModel project) {
        return project.classes().stream()
                .filter(unit -> unit.kind() == Kind.CLASS && unit.annotations().stream().anyMatch(annotation -> BEANS.contains(annotation.simpleName())))
                .flatMap(unit -> unit.constructors().stream()
                        .filter(constructor -> !constructor.parameters().isEmpty())
                        .map(constructor -> new Occurrence(unit.file(), constructor.line())))
                .toList();
    }
}

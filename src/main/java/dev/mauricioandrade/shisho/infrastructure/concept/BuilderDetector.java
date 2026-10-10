package dev.mauricioandrade.shisho.infrastructure.concept;

import dev.mauricioandrade.shisho.domain.model.ConceptDetector;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding.Occurrence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.List;
import java.util.stream.Stream;

/** A type named {@code Builder} or {@code *Builder}, or a {@code builder()} method with no parameters. */
final class BuilderDetector implements ConceptDetector {

    private static final String BUILDER = "Builder";
    private static final String BUILDER_METHOD = "builder";

    @Override
    public String id() {
        return "builder";
    }

    @Override
    public List<Occurrence> find(ProjectModel project) {
        return project.classes().stream()
                .flatMap(unit -> Stream.concat(
                        unit.name().endsWith(BUILDER) ? Stream.of(new Occurrence(unit.file(), unit.line())) : Stream.empty(),
                        unit.methods().stream()
                                .filter(method -> method.name().equals(BUILDER_METHOD) && method.parameters().isEmpty())
                                .map(method -> new Occurrence(unit.file(), method.line()))))
                .toList();
    }
}

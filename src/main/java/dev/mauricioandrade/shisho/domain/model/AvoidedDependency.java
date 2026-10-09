package dev.mauricioandrade.shisho.domain.model;

import java.util.List;

/**
 * A library the catalog advises against, by any of its {@code coordinates}: abandoned,
 * superseded or moved. {@code summary} and {@code alternative} complete the lesson;
 * {@code markdown} is the didactic explanation.
 */
public record AvoidedDependency(String id, String name, List<Dependency> coordinates, String summary, String alternative,
                                List<String> references, String markdown) {

    public AvoidedDependency {
        coordinates = List.copyOf(coordinates);
        references = List.copyOf(references);
    }
}

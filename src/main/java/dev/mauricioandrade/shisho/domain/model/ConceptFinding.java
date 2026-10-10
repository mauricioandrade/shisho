package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;

/** A concept found in the project, and every place it appears, in file and line order. */
public record ConceptFinding(String conceptId, List<Occurrence> occurrences) {

    public ConceptFinding {
        occurrences = List.copyOf(occurrences);
    }

    /** {@code file} is relative to the analyzed project root. */
    public record Occurrence(Path file, int line) {
    }
}

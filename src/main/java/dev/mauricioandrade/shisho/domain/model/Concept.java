package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Something a beginner meets in the code, such as a record, a transaction or the Strategy
 * pattern: how to recognize it and how to explain it. See docs/adr/0009-shoshin.md.
 *
 * @param order       its place in the group, from the first thing a beginner should meet
 * @param signals     any of them is enough to recognize the concept
 * @param explanation what it is, one string per paragraph
 * @param example     code the signals must recognize
 */
public record Concept(String id, String name, Group group, int order, List<Signal> signals, List<String> references,
                      List<String> explanation, KataGuide.Snippet example) {

    public Concept {
        signals = List.copyOf(signals);
        references = List.copyOf(references);
        explanation = List.copyOf(explanation);
    }

    public enum Group { JAVA, SPRING, QUARKUS, JAKARTA, PATTERN }

    /**
     * Conditions on one type, all of which must hold. {@code detector} names Java code that finds
     * what one type cannot show, such as an interface with several implementations.
     */
    public record Signal(Optional<String> annotation, Optional<String> kind, Optional<String> modifier,
                         Optional<String> importPrefix, Optional<String> supertype, Optional<String> call,
                         Optional<String> suffix, Optional<String> detector) {
    }
}

package dev.mauricioandrade.shisho.domain.model;

import java.util.List;

/**
 * The catalog architecture the project looks like the most. {@code signals} are the signature
 * entries found in the project; {@code confidence} is the share of the signature they cover, 0–100.
 * {@code violations} are lessons for imports that break the architecture's dependency rules.
 */
public record ArchitectureMatch(ArchitectureProfile architecture, List<String> signals, int confidence, List<Lesson> violations) {

    /** The kata id of dependency rule violations, so shisho.yaml can configure them like any kata. */
    public static final String DEPENDENCY_RULE = "dependency-rule";
    public static final Severity DEPENDENCY_RULE_SEVERITY = Severity.MEDIA;

    public ArchitectureMatch {
        signals = List.copyOf(signals);
        violations = List.copyOf(violations);
    }
}

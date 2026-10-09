package dev.mauricioandrade.shisho.domain.model;

import java.util.List;

/**
 * The catalog architecture the project looks like the most. {@code signals} are the signature
 * entries found in the project; {@code confidence} is the share of the signature they cover, 0–100.
 */
public record ArchitectureMatch(ArchitectureProfile architecture, List<String> signals, int confidence) {

    public ArchitectureMatch {
        signals = List.copyOf(signals);
    }
}

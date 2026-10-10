package dev.mauricioandrade.shisho.infrastructure.concept;

import dev.mauricioandrade.shisho.domain.model.ConceptDetector;

import java.util.List;

/** The detectors the catalog's concepts may name. See docs/adr/0009-shoshin.md. */
public final class ConceptDetectors {

    private ConceptDetectors() {
    }

    public static List<ConceptDetector> all() {
        return List.of(new StrategyDetector(), new BuilderDetector(), new ConstructorInjectionDetector());
    }
}

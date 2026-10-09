package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Whether the detected architecture fits the project, judged from the signals the detectors
 * measured and the context the author declared. See docs/adr/0002-architecture-classification.md.
 */
public sealed interface Verdict {

    /** More architecture than the project needs. {@code unjustified}: nothing in the declared context pays for its cost. */
    record Overengineering(List<Evidence> evidence, boolean unjustified, Optional<String> simplerAlternative) implements Verdict {

        public Overengineering {
            evidence = List.copyOf(evidence);
        }
    }

    /** The project outgrew the architecture. */
    record Underengineering(List<Evidence> evidence, List<String> evolvesTo) implements Verdict {

        public Underengineering {
            evidence = List.copyOf(evidence);
            evolvesTo = List.copyOf(evolvesTo);
        }
    }

    /** No signal measured. {@code justifications}: the declared context keys that pay for the architecture's cost. */
    record Adequate(boolean contextDeclared, List<String> justifications) implements Verdict {

        public Adequate {
            justifications = List.copyOf(justifications);
        }
    }
}

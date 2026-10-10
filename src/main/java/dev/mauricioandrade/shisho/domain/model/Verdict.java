package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Whether the detected architecture fits the project, judged from the signals the detectors
 * measured and the context the author declared. See docs/adr/0002-architecture-classification.md.
 */
public sealed interface Verdict {

    /**
     * More architecture than the project needs. {@code unjustified}: nothing in the declared context
     * pays for its cost. {@code justifiedBy}: the declared context keys that do pay for it, when
     * the code still shows the signals; the architecture fits the project, the code does not use it yet.
     */
    record Overengineering(List<Evidence> evidence, boolean unjustified, Optional<String> simplerAlternative,
                           List<String> justifiedBy) implements Verdict {

        public Overengineering {
            evidence = List.copyOf(evidence);
            justifiedBy = List.copyOf(justifiedBy);
        }

        public Overengineering(List<Evidence> evidence, boolean unjustified, Optional<String> simplerAlternative) {
            this(evidence, unjustified, simplerAlternative, List.of());
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

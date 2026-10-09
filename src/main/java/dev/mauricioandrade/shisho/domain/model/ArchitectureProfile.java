package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * One architectural style from the catalog: how to recognize it, which dependencies it forbids,
 * what it costs and when that cost pays off. {@code markdown} is the didactic explanation.
 */
public record ArchitectureProfile(
        String id,
        String name,
        Family family,
        Signature signature,
        List<DependencyRule> dependencyRules,
        Cost cost,
        List<String> makesSenseWhen,
        List<String> overengineeringSignals,
        List<String> underengineeringSignals,
        List<String> evolvesTo,
        Optional<String> simplerAlternative,
        List<String> references,
        String markdown) {

    public ArchitectureProfile {
        dependencyRules = List.copyOf(dependencyRules);
        makesSenseWhen = List.copyOf(makesSenseWhen);
        overengineeringSignals = List.copyOf(overengineeringSignals);
        underengineeringSignals = List.copyOf(underengineeringSignals);
        evolvesTo = List.copyOf(evolvesTo);
        references = List.copyOf(references);
    }

    /** The declared context keys that pay for this architecture's cost. */
    public List<String> justifiedBy(Optional<ProjectContext> context) {
        return makesSenseWhen.stream().filter(key -> context.map(declared -> declared.keys().contains(key)).orElse(false)).toList();
    }

    public enum Family { SIMPLE, MODULAR, DOMAIN_ISOLATION, DISTRIBUTED }

    public enum Complexity { LOW, MEDIUM, HIGH }

    public record Signature(List<String> packages, List<String> annotations, int minimumSignals) {

        public Signature {
            packages = List.copyOf(packages);
            annotations = List.copyOf(annotations);
        }
    }

    public record DependencyRule(String from, List<String> mustNotDependOn) {

        public DependencyRule {
            mustNotDependOn = List.copyOf(mustNotDependOn);
        }
    }

    public record Cost(ClassRange classesPerFeature, Complexity complexity) {
    }

    public record ClassRange(int min, int max) {
    }
}

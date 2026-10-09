package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.ProjectContext;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Verdict;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The verdict, in this order: any underengineering signal measured is underengineering; any
 * overengineering signal measured, or a declared context with no key that pays for a non-trivial
 * architecture, is overengineering; anything else is adequate.
 */
final class ArchitectureJudge {

    private static final System.Logger LOG = System.getLogger(ArchitectureJudge.class.getName());

    private final Map<String, Detector> detectors;

    ArchitectureJudge(List<Detector> detectors) {
        this.detectors = detectors.stream().collect(Collectors.toMap(Detector::id, Function.identity()));
    }

    Verdict judge(ArchitectureProfile architecture, ProjectModel project, Optional<ProjectContext> context) {
        List<Evidence> under = measure(architecture.underengineeringSignals(), project);
        if (!under.isEmpty()) {
            return new Verdict.Underengineering(under, architecture.evolvesTo());
        }
        List<String> justifications = architecture.makesSenseWhen().stream()
                .filter(key -> context.map(declared -> declared.keys().contains(key)).orElse(false))
                .toList();
        boolean unjustified = context.isPresent()
                && architecture.cost().complexity() != Complexity.LOW
                && justifications.isEmpty();
        List<Evidence> over = measure(architecture.overengineeringSignals(), project);
        if (!over.isEmpty() || unjustified) {
            return new Verdict.Overengineering(over, unjustified, architecture.simplerAlternative());
        }
        return new Verdict.Adequate(context.isPresent(), justifications);
    }

    /** The catalog may name signals before their detector exists: those are skipped, never an error. */
    private List<Evidence> measure(List<String> signals, ProjectModel project) {
        return signals.stream()
                .flatMap(signal -> {
                    Detector detector = detectors.get(signal);
                    if (detector == null) {
                        LOG.log(Level.DEBUG, "No detector for catalog signal {0} yet", signal);
                        return Stream.empty();
                    }
                    return detector.detect(project).stream();
                })
                .toList();
    }
}

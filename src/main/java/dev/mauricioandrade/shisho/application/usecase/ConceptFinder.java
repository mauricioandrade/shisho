package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.Concept;
import dev.mauricioandrade.shisho.domain.model.Concept.Signal;
import dev.mauricioandrade.shisho.domain.model.ConceptDetector;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding.Occurrence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.lang.System.Logger.Level;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Where each catalog concept appears in the project. A signal is a set of conditions on one type;
 * the line is the annotation's, the call's or the import's when the signal names one, and the
 * type's otherwise. See docs/adr/0009-shoshin.md.
 */
final class ConceptFinder {

    private static final System.Logger LOG = System.getLogger(ConceptFinder.class.getName());
    private static final Comparator<Occurrence> ORDER =
            Comparator.comparing((Occurrence occurrence) -> occurrence.file().toString()).thenComparingInt(Occurrence::line);

    private final List<Concept> concepts;
    private final Map<String, ConceptDetector> detectors;

    ConceptFinder(List<Concept> concepts, List<ConceptDetector> detectors) {
        this.concepts = List.copyOf(concepts);
        this.detectors = detectors.stream().collect(Collectors.toMap(ConceptDetector::id, Function.identity()));
    }

    /** The concepts the project shows, in catalog order; the ones it does not show are left out. */
    List<ConceptFinding> find(ProjectModel project) {
        return concepts.stream()
                .map(concept -> new ConceptFinding(concept.id(), occurrences(concept, project)))
                .filter(finding -> !finding.occurrences().isEmpty())
                .toList();
    }

    private List<Occurrence> occurrences(Concept concept, ProjectModel project) {
        Set<Occurrence> found = new TreeSet<>(ORDER);
        for (Signal signal : concept.signals()) {
            if (signal.detector().isPresent()) {
                found.addAll(detected(signal.detector().get(), project));
                continue;
            }
            project.classes().forEach(unit -> lines(signal, unit).forEach(line -> found.add(new Occurrence(unit.file(), line))));
        }
        return List.copyOf(found);
    }

    /** The catalog may name a detector before it exists: it is skipped, never an error. */
    private List<Occurrence> detected(String id, ProjectModel project) {
        ConceptDetector detector = detectors.get(id);
        if (detector == null) {
            LOG.log(Level.DEBUG, "No concept detector {0} yet", id);
            return List.of();
        }
        return detector.find(project);
    }

    /** Where the signal holds in this type, or nothing. */
    static List<Integer> lines(Signal signal, ClassUnit unit) {
        boolean typeMatches = signal.kind().map(kind -> unit.kind().name().toLowerCase(Locale.ROOT).equals(kind)).orElse(true)
                && signal.modifier().map(unit.modifiers()::contains).orElse(true)
                && signal.supertype().map(unit.supertypes()::contains).orElse(true)
                && signal.suffix().map(suffix -> unit.name().endsWith(suffix) && unit.name().length() > suffix.length()).orElse(true);
        if (!typeMatches) {
            return List.of();
        }
        Optional<List<Integer>> annotated = signal.annotation().map(name -> annotations(unit)
                .filter(annotation -> annotation.simpleName().equals(name)).map(Annotation::line).toList());
        Optional<List<Integer>> called = signal.call().map(name -> unit.invocations().stream()
                .filter(invocation -> invocation.name().equals(name)).map(ClassUnit.Invocation::line).toList());
        Optional<List<Integer>> imported = signal.importPrefix().map(prefix -> unit.imports().stream()
                .filter(declaration -> declaration.name().equals(prefix) || declaration.name().startsWith(prefix + "."))
                .map(ClassUnit.Import::line).toList());
        List<Optional<List<Integer>>> conditions = List.of(annotated, called, imported);
        if (conditions.stream().anyMatch(condition -> condition.map(List::isEmpty).orElse(false))) {
            return List.of();
        }
        return conditions.stream().flatMap(Optional::stream).findFirst().orElse(List.of(unit.line()));
    }

    private static Stream<Annotation> annotations(ClassUnit unit) {
        return Stream.of(
                        unit.annotations().stream(),
                        unit.fields().stream().flatMap(field -> field.annotations().stream()),
                        unit.constructors().stream().flatMap(constructor -> Stream.concat(constructor.annotations().stream(),
                                constructor.parameters().stream().flatMap(parameter -> parameter.annotations().stream()))),
                        unit.methods().stream().flatMap(method -> Stream.concat(method.annotations().stream(),
                                method.parameters().stream().flatMap(parameter -> parameter.annotations().stream()))))
                .flatMap(annotations -> annotations);
    }
}

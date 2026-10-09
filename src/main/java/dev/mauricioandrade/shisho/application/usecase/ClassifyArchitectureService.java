package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.PackagePattern;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.port.in.ClassifyArchitecture;
import dev.mauricioandrade.shisho.domain.port.out.CatalogRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Matches the project against the signature of every catalog architecture and picks the one
 * with the highest share of signals found. See docs/adr/0002-architecture-classification.md.
 */
public final class ClassifyArchitectureService implements ClassifyArchitecture {

    /** A placeholder pattern must bind this many different segments: two features, two modules. */
    static final int MINIMUM_PLACEHOLDER_BINDINGS = 2;

    private final CatalogRepository catalog;

    public ClassifyArchitectureService(CatalogRepository catalog) {
        this.catalog = catalog;
    }

    @Override
    public Optional<ArchitectureMatch> classify(ProjectModel project) {
        Set<String> packages = project.classes().stream().map(ClassUnit::packageName).collect(Collectors.toSet());
        return catalog.architectures().stream()
                .map(architecture -> match(architecture, packages, project))
                .filter(match -> match.signals().size() >= match.architecture().signature().minimumSignals())
                .min(Comparator.comparingInt((ArchitectureMatch match) -> -match.confidence())
                        .thenComparingInt(match -> -match.signals().size())
                        .thenComparing(match -> match.architecture().id()));
    }

    private static ArchitectureMatch match(ArchitectureProfile architecture, Set<String> packages, ProjectModel project) {
        Signature signature = architecture.signature();
        List<String> signals = Stream.concat(
                        signature.packages().stream().filter(pattern -> occurs(PackagePattern.parse(pattern), packages)),
                        signature.annotations().stream().filter(marker -> occurs(marker, project)))
                .toList();
        int total = signature.packages().size() + signature.annotations().size();
        int confidence = total == 0 ? 0 : Math.round(100f * signals.size() / total);
        return new ArchitectureMatch(architecture, signals, confidence);
    }

    private static boolean occurs(PackagePattern pattern, Set<String> packages) {
        if (!pattern.hasPlaceholder()) {
            return packages.stream().anyMatch(pattern::matches);
        }
        long distinctBindings = packages.stream().flatMap(name -> pattern.bindings(name).stream()).distinct().count();
        return distinctBindings >= MINIMUM_PLACEHOLDER_BINDINGS;
    }

    /**
     * {@code @KafkaListener} or {@code CommandHandler}: an annotation; {@code org.axonframework}: an
     * import or groupId prefix; {@code spring-kafka}: an artifactId prefix. Anything else, such as a
     * detector id, never occurs.
     */
    private static boolean occurs(String marker, ProjectModel project) {
        String name = marker.startsWith("@") ? marker.substring(1) : marker;
        if (name.isEmpty() || name.contains("_")) {
            return false;
        }
        if (Character.isUpperCase(name.charAt(0))) {
            return project.classes().stream().flatMap(ClassifyArchitectureService::annotations)
                    .anyMatch(annotation -> annotation.simpleName().equals(name));
        }
        if (name.contains(".")) {
            return project.classes().stream().flatMap(unit -> unit.imports().stream())
                    .anyMatch(declaration -> declaration.name().startsWith(name))
                    || project.build().stream().flatMap(build -> build.dependencies().stream())
                    .anyMatch(dependency -> dependency.groupId().startsWith(name));
        }
        return project.build().stream().flatMap(build -> build.dependencies().stream())
                .anyMatch(dependency -> dependency.artifactId().startsWith(name));
    }

    private static Stream<Annotation> annotations(ClassUnit unit) {
        return Stream.of(
                        unit.annotations().stream(),
                        unit.fields().stream().flatMap(field -> field.annotations().stream()),
                        unit.methods().stream().flatMap(method -> method.annotations().stream()),
                        unit.methods().stream().flatMap(method -> method.parameters().stream())
                                .flatMap(parameter -> parameter.annotations().stream()))
                .flatMap(annotations -> annotations);
    }
}

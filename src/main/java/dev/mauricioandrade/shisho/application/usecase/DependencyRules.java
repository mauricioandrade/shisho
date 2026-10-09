package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.DependencyRule;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Import;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.PackagePattern;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Checks the imports of every class against an architecture's {@code regras_dependencia}. A
 * forbidden entry with a dot ({@code org.springframework}) is a prefix of the imported name; any
 * other entry is a package pattern of the project itself. Rules with placeholders or qualifiers
 * ({@code <modulo>}, {@code banco:...}) need more than imports and are not checked yet.
 */
final class DependencyRules {

    private final ArchitectureProfile architecture;
    private final Set<String> projectPackages;

    DependencyRules(ArchitectureProfile architecture, Set<String> projectPackages) {
        this.architecture = architecture;
        this.projectPackages = projectPackages;
    }

    List<Lesson> violations(List<ClassUnit> classes) {
        return architecture.dependencyRules().stream()
                .filter(DependencyRules::isCheckable)
                .flatMap(rule -> classes.stream()
                        .filter(unit -> PackagePattern.parse(rule.from()).matches(unit.packageName()))
                        .flatMap(unit -> violations(rule, unit)))
                .toList();
    }

    private Stream<Lesson> violations(DependencyRule rule, ClassUnit unit) {
        return unit.imports().stream()
                .filter(declaration -> !declaration.isModule())
                .flatMap(declaration -> rule.mustNotDependOn().stream()
                        .filter(forbidden -> breaks(declaration, forbidden))
                        .findFirst()
                        .map(forbidden -> new Lesson(
                                ArchitectureMatch.DEPENDENCY_RULE,
                                ArchitectureMatch.DEPENDENCY_RULE_SEVERITY,
                                unit.file(),
                                declaration.line(),
                                "architecture.dependency-rule.lesson",
                                List.of(unit.name(), rule.from(), declaration.name(), forbidden, architecture.name())))
                        .stream());
    }

    private boolean breaks(Import declaration, String forbidden) {
        if (forbidden.contains(".")) {
            return declaration.name().equals(forbidden) || declaration.name().startsWith(forbidden + ".");
        }
        return projectPackage(declaration).filter(PackagePattern.parse(forbidden)::matches).isPresent();
    }

    /** The project package an import points into: the longest project package it starts with. */
    private Optional<String> projectPackage(Import declaration) {
        String name = declaration.name();
        return projectPackages.stream()
                .filter(candidate -> name.equals(candidate) || name.startsWith(candidate + "."))
                .max(Comparator.comparingInt(String::length));
    }

    private static boolean isCheckable(DependencyRule rule) {
        return Stream.concat(Stream.of(rule.from()), rule.mustNotDependOn().stream())
                .noneMatch(entry -> entry.contains("<") || entry.contains(":"));
    }
}

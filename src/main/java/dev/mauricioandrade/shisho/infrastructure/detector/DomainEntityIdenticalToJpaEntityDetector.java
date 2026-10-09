package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A JPA entity named after a domain class ({@code UserEntity}, {@code UserJpaEntity} for
 * {@code User}) holding exactly the same fields: two copies of one structure plus a mapper.
 */
final class DomainEntityIdenticalToJpaEntityDetector implements Detector {

    private static final List<String> ENTITY_SUFFIXES = List.of("JpaEntity", "Entity", "Jpa");

    @Override
    public String id() {
        return "entidade_de_dominio_identica_a_entidade_jpa";
    }

    @Override
    public Optional<Evidence> detect(ProjectModel project) {
        Map<String, ClassUnit> byName = project.classes().stream()
                .filter(unit -> !unit.isAnnotatedWith("Entity"))
                .collect(Collectors.toMap(ClassUnit::name, Function.identity(), (first, _) -> first));
        return project.classes().stream()
                .filter(unit -> unit.isAnnotatedWith("Entity"))
                .flatMap(entity -> ENTITY_SUFFIXES.stream()
                        .filter(suffix -> entity.name().endsWith(suffix) && entity.name().length() > suffix.length())
                        .map(suffix -> byName.get(entity.name().substring(0, entity.name().length() - suffix.length())))
                        .filter(domain -> domain != null && sameState(domain, entity))
                        .map(domain -> new Evidence(id(), List.of(domain.name(), entity.name()))))
                .findFirst();
    }

    private static boolean sameState(ClassUnit domain, ClassUnit entity) {
        List<String> state = domain.stateNames();
        return !state.isEmpty() && new HashSet<>(state).equals(new HashSet<>(entity.stateNames()));
    }
}

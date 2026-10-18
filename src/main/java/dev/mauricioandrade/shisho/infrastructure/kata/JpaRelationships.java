package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** The relationship mappings of a JPA entity, on fields or on getters (property access). */
final class JpaRelationships {

    static final Set<String> TO_MANY = Set.of("OneToMany", "ManyToMany");
    static final Set<String> TO_ONE = Set.of("ManyToOne", "OneToOne");

    private JpaRelationships() {
    }

    /** The mapping annotations among {@code kinds}, with the name of the field or getter they sit on. */
    static Stream<Mapping> of(ClassUnit unit, Set<String> kinds) {
        Stream<Mapping> fields = unit.fields().stream()
                .flatMap(field -> field.annotations().stream().map(annotation -> new Mapping(field.name(), annotation)));
        Stream<Mapping> getters = unit.methods().stream()
                .flatMap(method -> method.annotations().stream().map(annotation -> new Mapping(method.name(), annotation)));
        return Stream.concat(fields, getters).filter(mapping -> kinds.contains(mapping.annotation().simpleName()));
    }

    /** {@code EAGER} or {@code LAZY}, as {@code FetchType.EAGER} or a static import writes it. */
    static Optional<String> fetch(Annotation annotation) {
        return annotation.attribute("fetch").map(fetch -> fetch.substring(fetch.lastIndexOf('.') + 1));
    }

    record Mapping(String member, Annotation annotation) {
    }
}

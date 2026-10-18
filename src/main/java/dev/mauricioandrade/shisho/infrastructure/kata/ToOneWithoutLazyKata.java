package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;

/**
 * A {@code @ManyToOne} or {@code @OneToOne} without {@code fetch = FetchType.LAZY}: JPA's default
 * for them is {@code EAGER}, so leaving it out loads the other side on every query. An explicit
 * {@code EAGER} is a choice the code states, and is left alone. A {@code @OneToOne(mappedBy = ...)}
 * is too: Hibernate cannot make the inverse side lazy without bytecode enhancement.
 */
final class ToOneWithoutLazyKata implements ClassKata {

    @Override
    public String id() {
        return "to-one-without-lazy";
    }

    @Override
    public Severity severity() {
        return Severity.LEVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        return JpaRelationships.of(unit, JpaRelationships.TO_ONE)
                .filter(mapping -> JpaRelationships.fetch(mapping.annotation()).isEmpty())
                .filter(mapping -> mapping.annotation().attribute("mappedBy").isEmpty())
                .map(mapping -> lesson(unit, mapping.annotation().line(), mapping.member(), mapping.annotation().simpleName()))
                .toList();
    }
}

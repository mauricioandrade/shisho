package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;

/** A {@code @OneToMany} or {@code @ManyToMany} mapped with {@code fetch = FetchType.EAGER}. */
final class EagerCollectionKata implements ClassKata {

    @Override
    public String id() {
        return "eager-collection";
    }

    @Override
    public Severity severity() {
        return Severity.MEDIA;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        return JpaRelationships.of(unit, JpaRelationships.TO_MANY)
                .filter(mapping -> JpaRelationships.fetch(mapping.annotation()).filter("EAGER"::equals).isPresent())
                .map(mapping -> lesson(unit, mapping.annotation().line(), mapping.member(), mapping.annotation().simpleName()))
                .toList();
    }
}

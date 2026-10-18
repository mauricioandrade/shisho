package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;

/**
 * A {@code catch} block with no statement and no comment: the exception disappears without a
 * trace. A comment saying why, or a variable named {@code ignored} or {@code expected} (or
 * {@code _}, from Java 22), says it is on purpose.
 */
final class SwallowedExceptionKata implements ClassKata {

    private static final Set<String> ON_PURPOSE = Set.of("ignored", "ignore", "expected", "_");

    @Override
    public String id() {
        return "swallowed-exception";
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
        return unit.catches().stream()
                .filter(clause -> clause.empty() && !clause.commented() && !ON_PURPOSE.contains(clause.variable()))
                .map(clause -> lesson(unit, clause.line(), String.join(" | ", clause.types())))
                .toList();
    }
}

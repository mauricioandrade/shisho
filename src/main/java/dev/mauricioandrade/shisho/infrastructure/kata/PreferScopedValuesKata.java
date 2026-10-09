package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A {@code ThreadLocal} field, usually request context passed around implicitly. Only a
 * suggestion: a per-thread cache is still a fair use of {@code ThreadLocal}.
 */
final class PreferScopedValuesKata implements ClassKata {

    private static final Pattern THREAD_LOCAL = Pattern.compile("(java\\.lang\\.)?(Inheritable)?ThreadLocal\\b.*");

    @Override
    public String id() {
        return "prefer-scoped-values";
    }

    @Override
    public Severity severity() {
        return Severity.INFO;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return new JavaVersion(25);
    }

    @Override
    public Optional<String> javaFeature() {
        return Optional.of("scoped-values");
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        return unit.fields().stream()
                .filter(field -> THREAD_LOCAL.matcher(field.type()).matches())
                .map(field -> lesson(unit, field.line(), field.name()))
                .toList();
    }
}

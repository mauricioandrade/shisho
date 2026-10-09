package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A pool of platform threads from {@code Executors}. Only a suggestion: a fixed pool is still
 * right for CPU-bound work, and the kata cannot tell what the tasks do.
 */
final class PreferVirtualThreadsKata implements ClassKata {

    private static final Set<String> EXECUTORS = Set.of("Executors", "java.util.concurrent.Executors");
    private static final Set<String> PLATFORM_POOLS = Set.of("newFixedThreadPool", "newCachedThreadPool");

    @Override
    public String id() {
        return "prefer-virtual-threads";
    }

    @Override
    public Severity severity() {
        return Severity.INFO;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return new JavaVersion(21);
    }

    @Override
    public Optional<String> javaFeature() {
        return Optional.of("virtual-threads");
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        return unit.invocations().stream()
                .filter(invocation -> PLATFORM_POOLS.contains(invocation.name()))
                .filter(invocation -> invocation.target().filter(EXECUTORS::contains).isPresent())
                .map(invocation -> lesson(unit, invocation.line(), invocation.name()))
                .toList();
    }
}

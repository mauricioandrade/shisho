package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Invocation;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;

/**
 * Calls on {@code System.out} / {@code System.err} and {@code printStackTrace()}. In a Quarkus
 * application, the lesson suggests the Quarkus logger.
 */
final class SystemOutKata implements ProjectKata {

    private static final Set<String> CONSOLE_STREAMS = Set.of(
            "System.out", "System.err", "java.lang.System.out", "java.lang.System.err");

    @Override
    public String id() {
        return "system-out";
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
    public List<Lesson> evaluate(ProjectModel project) {
        boolean quarkus = project.build().filter(QuarkusApplications::isQuarkusApplication).isPresent();
        return project.classes().stream()
                .flatMap(unit -> unit.invocations().stream()
                        .filter(SystemOutKata::writesToConsole)
                        .map(invocation -> quarkus
                                ? QuarkusApplications.lesson(this, unit.file(), invocation.line(), written(invocation))
                                : lesson(unit.file(), invocation.line(), written(invocation))))
                .toList();
    }

    private static boolean writesToConsole(Invocation invocation) {
        return invocation.target().filter(CONSOLE_STREAMS::contains).isPresent()
                || invocation.name().equals("printStackTrace");
    }

    private static String written(Invocation invocation) {
        return invocation.target().map(target -> target + ".").orElse("") + invocation.name();
    }
}

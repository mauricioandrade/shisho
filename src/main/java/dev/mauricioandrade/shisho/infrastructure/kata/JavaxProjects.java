package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Projects still on Java EE's {@code javax.*} packages, which Jakarta EE 9 renamed to
 * {@code jakarta.*}: Spring Boot 2 and Quarkus 2. The build answers when it pins the platform's
 * version (a parent, a BOM, a plugin); otherwise the code does, by what it imports.
 * {@code javax.annotation} is left out: JSR 305's {@code @Nullable} lives there in projects of any age.
 */
final class JavaxProjects {

    /** The groupIds whose version 3 moved to {@code jakarta.*}. */
    private static final List<String> PLATFORMS = List.of("org.springframework.boot", "io.quarkus.platform", "io.quarkus");
    private static final int FIRST_JAKARTA_MAJOR = 3;
    private static final Pattern MAJOR = Pattern.compile("^(\\d+)");

    private static final List<String> RENAMED = List.of(
            "persistence", "validation", "servlet", "ws.rs", "inject", "enterprise", "transaction");

    private JavaxProjects() {
    }

    /**
     * Below version 3 of the platform the build pins; without one, whether the code imports the
     * {@code javax.*} packages and none of their {@code jakarta.*} successors.
     */
    static boolean isJavax(ProjectModel project) {
        return project.build().flatMap(JavaxProjects::platformMajor)
                .map(major -> major < FIRST_JAKARTA_MAJOR)
                .orElseGet(() -> imports(project.classes(), "javax") && !imports(project.classes(), "jakarta"));
    }

    /** The {@code @Valid} the project's Bean Validation provides. */
    static String valid(ProjectModel project) {
        return (isJavax(project) ? "javax" : "jakarta") + ".validation.Valid";
    }

    /**
     * The lesson explained for Spring Boot 2, by the message key {@code kata.<id>.lesson.spring-boot-2}:
     * for advice that names what only Spring Boot 3 has.
     */
    static Lesson springBoot2Lesson(Kata kata, Path file, int line, Object... arguments) {
        return new Lesson(kata.id(), kata.severity(), file, line, "kata." + kata.id() + ".lesson.spring-boot-2", List.of(arguments));
    }

    private static Optional<Integer> platformMajor(BuildInfo build) {
        return PLATFORMS.stream()
                .flatMap(groupId -> build.platformVersion(groupId).stream())
                .map(MAJOR::matcher)
                .filter(Matcher::find)
                .map(major -> Integer.parseInt(major.group(1)))
                .findFirst();
    }

    private static boolean imports(List<ClassUnit> classes, String root) {
        List<String> packages = RENAMED.stream().map(renamed -> root + "." + renamed).toList();
        return classes.stream()
                .flatMap(unit -> unit.imports().stream())
                .map(ClassUnit.Import::name)
                .anyMatch(name -> packages.stream().anyMatch(prefix -> name.equals(prefix) || name.startsWith(prefix + ".")));
    }
}

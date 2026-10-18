package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.nio.file.Path;
import java.util.List;

/**
 * Projects still on Java EE's {@code javax.*} packages, which Jakarta EE 9 renamed to
 * {@code jakarta.*}: Spring Boot 2 and Quarkus 2. Recognized by what the code imports, since the
 * build of a Spring Boot project does not show the framework's version. {@code javax.annotation}
 * is left out: JSR 305's {@code @Nullable} lives there in projects of any age.
 */
final class JavaxProjects {

    private static final List<String> RENAMED = List.of(
            "persistence", "validation", "servlet", "ws.rs", "inject", "enterprise", "transaction");

    private JavaxProjects() {
    }

    /** Whether the code imports the {@code javax.*} packages and none of their {@code jakarta.*} successors. */
    static boolean isJavax(List<ClassUnit> classes) {
        return imports(classes, "javax") && !imports(classes, "jakarta");
    }

    /** The {@code @Valid} the project's Bean Validation provides. */
    static String valid(List<ClassUnit> classes) {
        return (isJavax(classes) ? "javax" : "jakarta") + ".validation.Valid";
    }

    /**
     * The lesson explained for Spring Boot 2, by the message key {@code kata.<id>.lesson.spring-boot-2}:
     * for advice that names what only Spring Boot 3 has.
     */
    static Lesson springBoot2Lesson(Kata kata, Path file, int line, Object... arguments) {
        return new Lesson(kata.id(), kata.severity(), file, line, "kata." + kata.id() + ".lesson.spring-boot-2", List.of(arguments));
    }

    private static boolean imports(List<ClassUnit> classes, String root) {
        List<String> packages = RENAMED.stream().map(renamed -> root + "." + renamed).toList();
        return classes.stream()
                .flatMap(unit -> unit.imports().stream())
                .map(ClassUnit.Import::name)
                .anyMatch(name -> packages.stream().anyMatch(prefix -> name.equals(prefix) || name.startsWith(prefix + ".")));
    }
}

package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.nio.file.Path;
import java.util.List;

/**
 * Quarkus applications, recognized by any {@code io.quarkus} dependency in the build. A bean
 * imports nothing from Quarkus ({@code @ApplicationScoped} and {@code @Inject} come from Jakarta),
 * so the build is the only place that tells. See {@code docs/adr/0012-quarkus.md}.
 */
final class QuarkusApplications {

    private QuarkusApplications() {
    }

    static final String QUARKUS = "io.quarkus";

    static boolean isQuarkusApplication(BuildInfo build) {
        return build.dependencies().stream().anyMatch(dependency -> dependency.groupId().equals(QUARKUS));
    }

    /** A REST server: Quarkus REST or RESTEasy Classic, and not only their clients. */
    static boolean isWebApplication(BuildInfo build) {
        return build.dependencies().stream()
                .filter(dependency -> dependency.groupId().equals(QUARKUS))
                .map(Dependency::artifactId)
                .anyMatch(artifact -> (artifact.startsWith("quarkus-rest") || artifact.startsWith("quarkus-resteasy"))
                        && !artifact.contains("client"));
    }

    /**
     * A lesson of {@code kata} explained the Quarkus way, by the message key
     * {@code kata.<id>.lesson.quarkus}: the same practice, with the APIs Quarkus uses for it.
     */
    static Lesson lesson(Kata kata, Path file, int line, Object... arguments) {
        return new Lesson(kata.id(), kata.severity(), file, line, "kata." + kata.id() + ".lesson.quarkus", List.of(arguments));
    }
}

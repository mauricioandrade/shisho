package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Quarkus applications, recognized by any {@code io.quarkus} dependency in the build. A bean
 * imports nothing from Quarkus ({@code @ApplicationScoped} and {@code @Inject} come from Jakarta),
 * so the build is the only place that tells. In a multi-module build, any module counts. See
 * {@code docs/adr/0012-quarkus.md}.
 */
final class QuarkusApplications {

    private QuarkusApplications() {
    }

    static final String QUARKUS = "io.quarkus";

    static boolean isQuarkusApplication(BuildInfo build) {
        return build.allDependencies().stream().anyMatch(dependency -> dependency.groupId().equals(QUARKUS));
    }

    /** A REST server: Quarkus REST or RESTEasy Classic, and not only their clients. */
    static boolean isWebApplication(BuildInfo build) {
        return restServer(build).isPresent();
    }

    /** The REST server extension of the build or of one of its modules. */
    static Optional<Dependency> restServer(BuildInfo build) {
        return build.allDependencies().stream()
                .filter(dependency -> dependency.groupId().equals(QUARKUS))
                .filter(dependency -> (dependency.artifactId().startsWith("quarkus-rest") || dependency.artifactId().startsWith("quarkus-resteasy"))
                        && !dependency.artifactId().contains("client"))
                .findFirst();
    }

    /**
     * A lesson of {@code kata} explained the Quarkus way, by the message key
     * {@code kata.<id>.lesson.quarkus}: the same practice, with the APIs Quarkus uses for it.
     */
    static Lesson lesson(Kata kata, Path file, int line, Object... arguments) {
        return new Lesson(kata.id(), kata.severity(), file, line, "kata." + kata.id() + ".lesson.quarkus", List.of(arguments));
    }
}

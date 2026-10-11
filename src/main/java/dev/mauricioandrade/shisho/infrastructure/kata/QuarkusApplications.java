package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;

/**
 * Quarkus applications, recognized by any {@code io.quarkus} dependency in the build. A bean
 * imports nothing from Quarkus ({@code @ApplicationScoped} and {@code @Inject} come from Jakarta),
 * so the build is the only place that tells. See {@code docs/adr/0012-quarkus.md}.
 */
final class QuarkusApplications {

    private static final String QUARKUS = "io.quarkus";

    private QuarkusApplications() {
    }

    static boolean isQuarkusApplication(BuildInfo build) {
        return build.dependencies().stream().anyMatch(dependency -> dependency.groupId().equals(QUARKUS));
    }
}

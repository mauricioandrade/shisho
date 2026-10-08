package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * What the analyzed project's build file declares. {@code javaVersion} is empty when the build
 * does not say it locally (e.g. it is inherited from a remote parent).
 */
public record BuildInfo(Optional<JavaVersion> javaVersion, List<Dependency> dependencies) {

    public BuildInfo {
        dependencies = List.copyOf(dependencies);
    }
}

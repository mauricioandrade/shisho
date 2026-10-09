package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * What the analyzed project's build file declares. {@code file} is relative to the project root.
 * {@code javaVersion} is empty when the build does not say it locally (e.g. it is inherited from a
 * remote parent).
 */
public record BuildInfo(Path file, Optional<JavaVersion> javaVersion, List<Dependency> dependencies) {

    public BuildInfo {
        dependencies = List.copyOf(dependencies);
    }

    public boolean dependsOn(String groupId, String artifactId) {
        return dependencies.contains(new Dependency(groupId, artifactId));
    }
}

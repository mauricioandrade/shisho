package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Everything the katas know about the analyzed project: its build (empty when there is no build
 * file the Shishō understands) and every class that parsed, ordered by file.
 */
public record ProjectModel(JavaVersion javaVersion, Optional<BuildInfo> build, List<ClassUnit> classes) {

    public ProjectModel {
        classes = List.copyOf(classes);
    }
}

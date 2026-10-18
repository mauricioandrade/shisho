package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Everything the katas know about the analyzed project: its build (empty when there is no build
 * file the Shishō understands), every class that parsed, ordered by file, and the application's
 * settings files.
 */
public record ProjectModel(JavaVersion javaVersion, Optional<BuildInfo> build, List<ClassUnit> classes, List<SettingsFile> settings) {

    public ProjectModel {
        classes = List.copyOf(classes);
        settings = List.copyOf(settings);
    }

    /** Without settings files. */
    public ProjectModel(JavaVersion javaVersion, Optional<BuildInfo> build, List<ClassUnit> classes) {
        this(javaVersion, build, classes, List.of());
    }
}

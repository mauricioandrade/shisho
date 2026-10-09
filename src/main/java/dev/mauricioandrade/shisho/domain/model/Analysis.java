package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Result of analyzing the project at {@code projectRoot}, the path as the user gave it. {@code javaVersionDeclared} is false when the build did not
 * state a version and the Shishō assumed {@link JavaVersion#BASELINE}; {@code build} is empty
 * without a build file the Shishō understands; {@code architecture} is
 * empty when the project resembles no catalog architecture.
 */
public record Analysis(
        Path projectRoot,
        String projectName,
        JavaVersion javaVersion,
        boolean javaVersionDeclared,
        Optional<BuildInfo> build,
        Optional<ArchitectureMatch> architecture,
        List<Lesson> lessons,
        List<SourceFile.Unparseable> unparseableFiles,
        int score,
        Rank rank) {

    public Analysis {
        lessons = List.copyOf(lessons);
        unparseableFiles = List.copyOf(unparseableFiles);
    }

    public long count(Severity severity) {
        return lessons.stream().filter(lesson -> lesson.severity() == severity).count();
    }
}

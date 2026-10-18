package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Result of analyzing the project at {@code projectRoot}, the path as the user gave it. {@code javaVersionDeclared} is false when the build did not
 * state a version and the Shishō assumed {@link JavaVersion#ASSUMED}; {@code build} is empty
 * without a build file the Shishō understands; {@code architecture} is
 * empty when the project resembles no catalog architecture. With a {@code baseline}, {@code lessons}
 * are only its new ones (its {@link Baseline.Comparison#fresh()}); the score and the rank still
 * count every lesson, the known ones included.
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
        Rank rank,
        List<ConceptFinding> concepts,
        Optional<Baseline.Comparison> baseline) {

    public Analysis {
        lessons = List.copyOf(lessons);
        unparseableFiles = List.copyOf(unparseableFiles);
        concepts = List.copyOf(concepts);
    }

    /** Without a baseline. */
    public Analysis(Path projectRoot, String projectName, JavaVersion javaVersion, boolean javaVersionDeclared,
                    Optional<BuildInfo> build, Optional<ArchitectureMatch> architecture, List<Lesson> lessons,
                    List<SourceFile.Unparseable> unparseableFiles, int score, Rank rank, List<ConceptFinding> concepts) {
        this(projectRoot, projectName, javaVersion, javaVersionDeclared, build, architecture, lessons, unparseableFiles,
                score, rank, concepts, Optional.empty());
    }

    /** Without the concepts the shoshin mode explains: most reports and tests do not need them. */
    public Analysis(Path projectRoot, String projectName, JavaVersion javaVersion, boolean javaVersionDeclared,
                    Optional<BuildInfo> build, Optional<ArchitectureMatch> architecture, List<Lesson> lessons,
                    List<SourceFile.Unparseable> unparseableFiles, int score, Rank rank) {
        this(projectRoot, projectName, javaVersion, javaVersionDeclared, build, architecture, lessons, unparseableFiles,
                score, rank, List.of());
    }

    /** Every lesson, the baseline's known ones included, in file and line order. */
    public List<Lesson> allLessons() {
        return Stream.concat(lessons.stream(), baseline.stream().flatMap(comparison -> comparison.known().stream()))
                .sorted(Comparator.comparing((Lesson lesson) -> lesson.file().toString()).thenComparingInt(Lesson::line))
                .toList();
    }

    public long count(Severity severity) {
        return lessons.stream().filter(lesson -> lesson.severity() == severity).count();
    }
}

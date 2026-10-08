package dev.mauricioandrade.shisho.domain.model;

import java.util.List;

/**
 * Result of analyzing a project. {@code javaVersionDeclared} is false when the build did not
 * state a version and the Shishō assumed {@link JavaVersion#BASELINE}.
 */
public record Analysis(
        String projectName,
        JavaVersion javaVersion,
        boolean javaVersionDeclared,
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

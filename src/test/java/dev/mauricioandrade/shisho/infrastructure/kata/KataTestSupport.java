package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.SourceFile.Parsed;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Runs a kata over Java source written inline in the test, through the real parser. */
final class KataTestSupport {

    private KataTestSupport() {
    }

    static List<Lesson> evaluate(ClassKata kata, String source) {
        return classes(source).stream().flatMap(unit -> kata.evaluate(unit).stream()).toList();
    }

    /** A project made of {@code source} alone, with no build file. */
    static List<Lesson> evaluate(ProjectKata kata, String source) {
        return kata.evaluate(new ProjectModel(new JavaVersion(25), Optional.empty(), classes(source)));
    }

    static List<ClassUnit> classes(String source) {
        try {
            Path project = Files.createTempDirectory("shisho-kata");
            try {
                Path file = Files.createDirectories(project.resolve("src/main/java")).resolve("Sample.java");
                Files.writeString(file, source);
                return new JavaParserSourceParser().parse(project, new JavaVersion(25)).stream()
                        .map(Parsed.class::cast)
                        .flatMap(parsed -> parsed.classes().stream())
                        .toList();
            } finally {
                try (Stream<Path> paths = Files.walk(project)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

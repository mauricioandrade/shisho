package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.SettingsFile;
import dev.mauricioandrade.shisho.domain.model.SourceFile.Parsed;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import dev.mauricioandrade.shisho.infrastructure.settings.ApplicationSettingsReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    /** A project with no classes and a {@code pom.xml} declaring these {@code groupId:artifactId} pairs. */
    static List<Lesson> evaluateBuild(ProjectKata kata, String... dependencies) {
        return kata.evaluate(new ProjectModel(new JavaVersion(25), Optional.of(pom(dependencies)), List.of()));
    }

    /** A project made of {@code source} and a {@code pom.xml} declaring these {@code groupId:artifactId} pairs. */
    static List<Lesson> evaluate(ProjectKata kata, String source, String... dependencies) {
        return kata.evaluate(new ProjectModel(new JavaVersion(25), Optional.of(pom(dependencies)), classes(source)));
    }

    /**
     * A project with no classes, {@code properties} as its {@code src/main/resources/application.properties},
     * read by the real reader, and a {@code pom.xml} declaring these {@code groupId:artifactId} pairs.
     */
    static List<Lesson> evaluateSettings(ProjectKata kata, String properties, String... dependencies) {
        return evaluateSettings(kata, Map.of("src/main/resources/application.properties", properties), pom(dependencies));
    }

    /** A project with no classes, these settings files by path, read by the real reader, and {@code build}. */
    static List<Lesson> evaluateSettings(ProjectKata kata, Map<String, String> files, BuildInfo build) {
        try {
            Path project = Files.createTempDirectory("shisho-settings");
            try {
                for (Map.Entry<String, String> file : files.entrySet()) {
                    Path path = project.resolve(file.getKey());
                    Files.createDirectories(path.getParent());
                    Files.writeString(path, file.getValue());
                }
                List<SettingsFile> settings = new ApplicationSettingsReader().read(project);
                return kata.evaluate(new ProjectModel(new JavaVersion(25), Optional.of(build), List.of(), settings));
            } finally {
                delete(project);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A project with no classes and {@code build}, such as a multi-module one. */
    static List<Lesson> evaluate(ProjectKata kata, BuildInfo build) {
        return kata.evaluate(new ProjectModel(new JavaVersion(25), Optional.of(build), List.of()));
    }

    /** A build in {@code file} declaring these {@code groupId:artifactId} pairs, without modules. */
    static BuildInfo build(String file, String... dependencies) {
        List<Dependency> declared = Arrays.stream(dependencies)
                .map(dependency -> dependency.split(":"))
                .map(parts -> new Dependency(parts[0], parts[1]))
                .toList();
        return new BuildInfo(Path.of(file), Optional.of(new JavaVersion(25)), declared);
    }

    private static BuildInfo pom(String... dependencies) {
        return build("pom.xml", dependencies);
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
                delete(project);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void delete(Path project) throws IOException {
        try (Stream<Path> paths = Files.walk(project)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }
}

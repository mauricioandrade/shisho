package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.SourceFile.Parsed;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** Builds a project from Java sources written inline in the test, through the real parser. */
final class DetectorTestSupport {

    private DetectorTestSupport() {
    }

    /** One file per entry: path relative to {@code src/main/java} → source. */
    static ProjectModel project(Map<String, String> sources) {
        try {
            Path root = Files.createTempDirectory("shisho-detector");
            try {
                for (Map.Entry<String, String> source : sources.entrySet()) {
                    Path file = root.resolve("src/main/java").resolve(source.getKey());
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, source.getValue());
                }
                var classes = new JavaParserSourceParser().parse(root, new JavaVersion(25)).stream()
                        .map(Parsed.class::cast)
                        .flatMap(parsed -> parsed.classes().stream())
                        .toList();
                return new ProjectModel(new JavaVersion(25), Optional.empty(), classes);
            } finally {
                try (Stream<Path> paths = Files.walk(root)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static ProjectModel project(String source) {
        return project(Map.of("app/Sample.java", source));
    }
}

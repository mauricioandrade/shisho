package dev.mauricioandrade.shisho.infrastructure.fix;

import dev.mauricioandrade.shisho.application.usecase.AnalyzeProjectService;
import dev.mauricioandrade.shisho.application.usecase.ClassifyArchitectureService;
import dev.mauricioandrade.shisho.application.usecase.RankCalculator;
import dev.mauricioandrade.shisho.domain.model.FileFix;
import dev.mauricioandrade.shisho.infrastructure.build.MavenBuildFileReader;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.config.YamlProjectConfigReader;
import dev.mauricioandrade.shisho.infrastructure.detector.Detectors;
import dev.mauricioandrade.shisho.infrastructure.kata.Katas;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Analyzes a project written to {@code root} with the real adapters, then plans the corrections. */
final class FixTestSupport {

    static final String POM = """
            <project>
                <properties><java.version>21</java.version></properties>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-validation</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """;

    private FixTestSupport() {
    }

    static List<FileFix> fix(Path root, String source) {
        return fix(root, POM, source);
    }

    static List<FileFix> fix(Path root, String pom, String source) {
        write(root, "pom.xml", pom);
        write(root, "src/main/java/app/Sample.java", source);
        return new JavaParserSourceFixer().fix(analyzer().analyze(root));
    }

    /** The corrected source, or empty when nothing could be fixed. */
    static Optional<String> fixed(Path root, String source) {
        return fix(root, source).stream().findFirst().map(FileFix::fixed);
    }

    static AnalyzeProjectService analyzer() {
        return new AnalyzeProjectService(new MavenBuildFileReader(), new YamlProjectConfigReader(), new JavaParserSourceParser(),
                Katas.all(MarkdownCatalogRepository.bundled().avoidedDependencies()), new ClassifyArchitectureService(MarkdownCatalogRepository.bundled(), Detectors.all()), new RankCalculator());
    }

    static void write(Path root, String file, String content) {
        try {
            Path path = root.resolve(file);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

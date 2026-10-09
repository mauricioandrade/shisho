package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.SourceFile.Parsed;
import dev.mauricioandrade.shisho.infrastructure.build.MavenBuildFileReader;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** The real catalog against real projects: the fixtures and the Shishō itself. */
class CatalogClassificationTest {

    private final ClassifyArchitectureService service = new ClassifyArchitectureService(MarkdownCatalogRepository.bundled());

    @Test
    void theShishoIsCleanArchitecture() {
        assertThat(classify(Path.of(""))).hasValueSatisfying(match -> {
            assertThat(match.architecture().id()).isEqualTo("clean");
            assertThat(match.confidence()).isEqualTo(100);
        });
    }

    @Test
    void theMessyStudentIsLayered() {
        assertThat(classify(Path.of("src/test/resources/fixtures/aluno-baguncado")))
                .hasValueSatisfying(match -> assertThat(match.architecture().id()).isEqualTo("layered"));
    }

    @Test
    void aSinglePackageResemblesNothing() {
        assertThat(classify(Path.of("src/test/resources/fixtures/aluno-exemplar"))).isEmpty();
    }

    private Optional<ArchitectureMatch> classify(Path root) {
        var classes = new JavaParserSourceParser().parse(root, new JavaVersion(25)).stream()
                .map(Parsed.class::cast)
                .flatMap(parsed -> parsed.classes().stream())
                .toList();
        return service.classify(new ProjectModel(new JavaVersion(25), new MavenBuildFileReader().read(root), classes));
    }
}

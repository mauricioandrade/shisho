package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.application.usecase.AnalyzeProjectService;
import dev.mauricioandrade.shisho.application.usecase.ClassifyArchitectureService;
import dev.mauricioandrade.shisho.application.usecase.RankCalculator;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.infrastructure.build.MavenBuildFileReader;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.detector.Detectors;
import dev.mauricioandrade.shisho.infrastructure.config.YamlProjectConfigReader;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** Every kata against whole projects: the two fixtures of the acceptance criteria and the Shishō itself. */
class KatasTest {

    private static final Path FIXTURES = Path.of("src/test/resources/fixtures");

    @Test
    void katasHaveUniqueIds() {
        assertThat(Katas.all()).extracting(Kata::id).doesNotHaveDuplicates().hasSize(14);
    }

    @Test
    void messyStudentGetsOneLessonPerViolation() {
        String controller = "src/main/java/com/exemplo/loja/controller/UsuarioController.java";
        String request = "src/main/java/com/exemplo/loja/dto/UsuarioRequest.java";
        String properties = "src/main/java/com/exemplo/loja/config/LojaProperties.java";
        String email = "src/main/java/com/exemplo/loja/service/EmailService.java";
        String pedido = "src/main/java/com/exemplo/loja/service/PedidoService.java";
        String usuario = "src/main/java/com/exemplo/loja/service/UsuarioService.java";

        assertThat(analyze(FIXTURES.resolve("aluno-baguncado")))
                .extracting(lesson -> lesson.file().toString(), Lesson::line, Lesson::kataId, Lesson::severity)
                .containsExactly(
                        tuple("pom.xml", 0, "missing-actuator", Severity.LEVE),
                        tuple("pom.xml", 0, "missing-springdoc", Severity.INFO),
                        tuple(properties, 5, "configuration-properties-not-record", Severity.LEVE),
                        tuple(controller, 11, "missing-controller-advice", Severity.MEDIA),
                        tuple(controller, 15, "field-injection", Severity.GRAVE),
                        tuple(controller, 19, "request-body-without-valid", Severity.MEDIA),
                        tuple(controller, 20, "system-out", Severity.MEDIA),
                        tuple(controller, 24, "entity-in-controller", Severity.MEDIA),
                        tuple(request, 3, "dto-not-record", Severity.LEVE),
                        tuple(email, 14, "prefer-configuration-properties", Severity.LEVE),
                        tuple(pedido, 3, "dependency-rule", Severity.MEDIA),
                        tuple(pedido, 13, "non-final-dependency", Severity.MEDIA),
                        tuple(pedido, 19, "transactional-read-only", Severity.LEVE),
                        tuple(usuario, 13, "lombok-constructor", Severity.MEDIA),
                        tuple(usuario, 16, "non-final-dependency", Severity.MEDIA),
                        tuple(usuario, 18, "value-outside-constructor", Severity.LEVE),
                        tuple(usuario, 25, "system-out", Severity.MEDIA));
    }

    @Test
    void modelStudentHasNothingToLearn() {
        assertThat(analyze(FIXTURES.resolve("aluno-exemplar"))).isEmpty();
    }

    @Test
    void shishoPassesItsOwnKatas() {
        assertThat(analyze(Path.of(""))).isEmpty();
    }

    /** Through the real adapters, in the order the report uses: file, then line, then kata order. */
    private static List<Lesson> analyze(Path project) {
        return new AnalyzeProjectService(
                new MavenBuildFileReader(), new YamlProjectConfigReader(), new JavaParserSourceParser(), Katas.all(),
                new ClassifyArchitectureService(MarkdownCatalogRepository.bundled(), Detectors.all()), new RankCalculator())
                .analyze(project)
                .lessons();
    }
}

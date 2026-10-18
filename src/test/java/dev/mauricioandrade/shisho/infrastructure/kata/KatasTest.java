package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.application.usecase.AnalyzeProjectService;
import dev.mauricioandrade.shisho.application.usecase.ClassifyArchitectureService;
import dev.mauricioandrade.shisho.application.usecase.RankCalculator;
import dev.mauricioandrade.shisho.domain.model.JavaFeature;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** Every kata against whole projects: the two fixtures of the acceptance criteria and the Shishō itself. */
class KatasTest {

    private static final Path FIXTURES = Path.of("src/test/resources/fixtures");

    @Test
    void katasHaveUniqueIds() {
        assertThat(Katas.all(MarkdownCatalogRepository.bundled().avoidedDependencies())).extracting(Kata::id).doesNotHaveDuplicates().hasSize(24);
    }

    @Test
    void katasSuggestOnlyFinalJavaFeaturesSinceTheVersionTheCatalogStates() {
        Map<String, JavaFeature> features = MarkdownCatalogRepository.bundled().javaFeatures().stream()
                .collect(Collectors.toMap(JavaFeature::id, Function.identity()));

        assertThat(Katas.all(MarkdownCatalogRepository.bundled().avoidedDependencies())).filteredOn(kata -> kata.javaFeature().isPresent()).isNotEmpty().allSatisfy(kata -> {
            JavaFeature feature = features.get(kata.javaFeature().orElseThrow());
            assertThat(feature).as(kata.id()).isNotNull();
            assertThat(feature.status()).as(kata.id()).isEqualTo(JavaFeature.Status.FINAL);
            assertThat(kata.minJavaVersion()).as(kata.id()).isEqualTo(feature.since());
        });
    }

    @Test
    void katasWithoutAJavaFeatureRunOnEveryAnalyzedVersion() {
        assertThat(Katas.all(MarkdownCatalogRepository.bundled().avoidedDependencies()))
                .filteredOn(kata -> kata.javaFeature().isEmpty())
                .as("a kata below its minJavaVersion only says what an upgrade would bring")
                .allSatisfy(kata -> assertThat(kata.minJavaVersion()).as(kata.id()).isEqualTo(JavaVersion.MINIMUM));
    }

    @Test
    void messyStudentGetsOneLessonPerViolation() {
        String controller = "src/main/java/com/exemplo/loja/controller/UsuarioController.java";
        String request = "src/main/java/com/exemplo/loja/dto/UsuarioRequest.java";
        String properties = "src/main/java/com/exemplo/loja/config/LojaProperties.java";
        String email = "src/main/java/com/exemplo/loja/service/EmailService.java";
        String notificacao = "src/main/java/com/exemplo/loja/service/NotificacaoService.java";
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
                        tuple(request, 5, "dto-not-record", Severity.LEVE),
                        tuple(email, 14, "prefer-configuration-properties", Severity.LEVE),
                        tuple(notificacao, 11, "prefer-scoped-values", Severity.INFO),
                        tuple(notificacao, 13, "prefer-virtual-threads", Severity.INFO),
                        tuple(pedido, 3, "dependency-rule", Severity.MEDIA),
                        tuple(pedido, 13, "non-final-dependency", Severity.MEDIA),
                        tuple(pedido, 19, "transactional-read-only", Severity.LEVE),
                        tuple(pedido, 25, "unproxyable-method", Severity.GRAVE),
                        tuple(usuario, 13, "lombok-constructor", Severity.MEDIA),
                        tuple(usuario, 16, "non-final-dependency", Severity.MEDIA),
                        tuple(usuario, 18, "value-outside-constructor", Severity.LEVE),
                        tuple(usuario, 25, "system-out", Severity.MEDIA));
    }

    @Test
    void messyQuarkusStudentLearnsTheQuarkusWay() {
        String pedido = "src/main/java/com/exemplo/loja/pedido/";

        assertThat(analyze(FIXTURES.resolve("aluno-quarkus-baguncado")))
                .extracting(lesson -> lesson.file().toString(), Lesson::line, Lesson::kataId, Lesson::messageKey)
                .containsExactly(
                        tuple("pom.xml", 0, "missing-actuator", "kata.missing-actuator.lesson.quarkus"),
                        tuple("pom.xml", 0, "missing-springdoc", "kata.missing-springdoc.lesson.quarkus"),
                        tuple(pedido + "CriarPedidoRequest.java", 5, "dto-not-record", "kata.dto-not-record.lesson"),
                        tuple(pedido + "FreteService.java", 9, "prefer-configuration-properties",
                                "kata.prefer-configuration-properties.lesson.quarkus"),
                        tuple(pedido + "FreteService.java", 9, "quarkus-private-injection",
                                "kata.quarkus-private-injection.lesson"),
                        tuple(pedido + "PedidoResource.java", 8, "missing-controller-advice",
                                "kata.missing-controller-advice.lesson.quarkus"),
                        tuple(pedido + "PedidoResource.java", 14, "entity-in-controller", "kata.entity-in-controller.lesson"),
                        tuple(pedido + "PedidoResource.java", 20, "entity-in-controller", "kata.entity-in-controller.lesson"),
                        tuple(pedido + "PedidoResource.java", 21, "request-body-without-valid",
                                "kata.request-body-without-valid.lesson.quarkus"),
                        tuple(pedido + "PedidoService.java", 9, "non-final-dependency", "kata.non-final-dependency.lesson"),
                        tuple(pedido + "PedidoService.java", 25, "system-out", "kata.system-out.lesson.quarkus"));
    }

    @Test
    void modelStudentHasNothingToLearn() {
        assertThat(analyze(FIXTURES.resolve("aluno-exemplar"))).isEmpty();
    }

    @Test
    void quarkusModelStudentHasNothingToLearn() {
        assertThat(analyze(FIXTURES.resolve("aluno-quarkus"))).isEmpty();
    }

    @Test
    void theArchitectureAstronautWritesCleanCode() {
        assertThat(analyze(FIXTURES.resolve("aluno-astronauta"))).isEmpty();
    }

    @Test
    void shishoPassesItsOwnKatas() {
        assertThat(analyze(Path.of(""))).isEmpty();
    }

    /** Through the real adapters, in the order the report uses: file, then line, then kata order. */
    private static List<Lesson> analyze(Path project) {
        return new AnalyzeProjectService(
                new MavenBuildFileReader(), new YamlProjectConfigReader(), new JavaParserSourceParser(), Katas.all(MarkdownCatalogRepository.bundled().avoidedDependencies()),
                new ClassifyArchitectureService(MarkdownCatalogRepository.bundled(), Detectors.all()), new RankCalculator())
                .analyze(project)
                .lessons();
    }
}

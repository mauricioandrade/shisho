package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.application.usecase.AnalyzeProjectService;
import dev.mauricioandrade.shisho.application.usecase.ClassifyArchitectureService;
import dev.mauricioandrade.shisho.application.usecase.RankCalculator;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.infrastructure.build.MavenBuildFileReader;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.detector.Detectors;
import dev.mauricioandrade.shisho.infrastructure.config.YamlProjectConfigReader;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.kata.Katas;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TerminalReportWriterTest {

    private static final Messages PORTUGUESE = new Messages(Messages.DEFAULT_LOCALE);
    private static final Messages ENGLISH = new Messages(Locale.ENGLISH);

    @Test
    void writesLessonsAndGraduation() {
        Analysis analysis = analysis(true, List.of(
                lesson("field-injection", Severity.GRAVE, 14, "usuarioService"),
                lesson("request-body-without-valid", Severity.MEDIA, 1234, "CriarUsuarioRequest")), List.of());

        assertThat(render(analysis, PORTUGUESE)).isEqualTo("""
                師匠 Shishō — análise de loja  (Java 21)
                Arquitetura: nenhuma do catálogo reconhecida

                ✗ GRAVE  [field-injection]             UsuarioController.java:14
                         Injeção em campo em usuarioService. Use construtor explícito com private final: a
                         dependência fica visível e o objeto, imutável.
                ! MEDIA  [request-body-without-valid]  UsuarioController.java:1234
                         @RequestBody CriarUsuarioRequest sem @Valid: as anotações de validação do DTO nunca são
                         verificadas.

                Graduação: 1º kyū  (2 lições: 1 grave, 1 média, 0 leves)
                """);
    }

    @Test
    void showsWholeFileLessonsWithoutLineAndCountsSuggestionsOnlyWhenThereAreAny() {
        Analysis analysis = analysis(true, List.of(
                new Lesson("missing-springdoc", Severity.INFO, Path.of("pom.xml"), 0, "kata.missing-springdoc.lesson", List.of()),
                lesson("dto-not-record", Severity.LEVE, 3, "UsuarioRequest")), List.of());

        assertThat(render(analysis, PORTUGUESE))
                .contains("i INFO   [missing-springdoc]  pom.xml\n")
                .contains("· LEVE   [dto-not-record]     UsuarioController.java:3\n")
                .endsWith("Graduação: 1º dan  (2 lições: 0 graves, 0 médias, 1 leve, 1 sugestão)\n");
    }

    @Test
    void celebratesAProjectWithoutLessons() {
        assertThat(render(analysis(true, List.of(), List.of()), PORTUGUESE)).isEqualTo("""
                師匠 Shishō — análise de loja  (Java 21)
                Arquitetura: nenhuma do catálogo reconhecida

                Graduação: 1º dan  (nenhuma lição)
                """);
    }

    @Test
    void warnsWhenTheVersionWasAssumedAndWhenFilesCouldNotBeParsed() {
        Analysis analysis = analysis(false, List.of(),
                List.of(new SourceFile.Unparseable(Path.of("src/main/java/Quebrado.java"), 4, "Parse error")));

        assertThat(render(analysis, PORTUGUESE)).isEqualTo("""
                師匠 Shishō — análise de loja  (Java 21, presumido: o build não declara a versão)
                Arquitetura: nenhuma do catálogo reconhecida

                ⚠ src/main/java/Quebrado.java:4 não pôde ser analisado: Parse error

                Graduação: 1º dan  (nenhuma lição)
                """);
    }

    @Test
    void speaksEnglishAsFallback() {
        Analysis analysis = analysis(true, List.of(lesson("dto-not-record", Severity.LEVE, 3, "UsuarioRequest")), List.of());

        assertThat(render(analysis, ENGLISH)).isEqualTo("""
                師匠 Shishō — analysis of loja  (Java 21)
                Architecture: none from the catalog recognized

                · MINOR   [dto-not-record]  UsuarioController.java:3
                          UsuarioRequest looks like a DTO but is not a record. A record is immutable and removes
                          hand-written getters, equals and hashCode.

                Rank: 1st dan  (1 lesson: 0 severe, 0 medium, 1 minor)
                """);
    }

    @Test
    void reportsTheMessyStudentFixture() {
        AnalyzeProjectService service = new AnalyzeProjectService(
                new MavenBuildFileReader(), new YamlProjectConfigReader(), new JavaParserSourceParser(), Katas.all(),
                new ClassifyArchitectureService(MarkdownCatalogRepository.bundled(), Detectors.all()), new RankCalculator());

        String report = render(service.analyze(Path.of("src/test/resources/fixtures/aluno-baguncado")), PORTUGUESE);

        assertThat(report)
                .startsWith("""
                        師匠 Shishō — análise de aluno-baguncado  (Java 21)
                        Arquitetura: Camadas técnicas (Layered)  (83% da assinatura: controller, service, repository, model, dto)

                        · LEVE   [missing-actuator]""")
                .endsWith("\nGraduação: 5º kyū  (17 lições: 1 grave, 9 médias, 6 leves, 1 sugestão)\n");
    }

    private static String render(Analysis analysis, Messages messages) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new TerminalReportWriter(out, messages).write(analysis);
        return buffer.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }

    private static Analysis analysis(boolean versionDeclared, List<Lesson> lessons, List<SourceFile.Unparseable> unparseable) {
        int score = new RankCalculator().score(lessons);
        return new Analysis("loja", new JavaVersion(21), versionDeclared, Optional.empty(), lessons, unparseable, score, Rank.forScore(score));
    }

    private static Lesson lesson(String kata, Severity severity, int line, String argument) {
        return new Lesson(kata, severity, Path.of("src/main/java/app/UsuarioController.java"), line,
                "kata." + kata + ".lesson", List.of(argument));
    }
}

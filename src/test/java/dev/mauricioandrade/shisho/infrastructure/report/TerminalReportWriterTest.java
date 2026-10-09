package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.application.usecase.AnalyzeProjectService;
import dev.mauricioandrade.shisho.application.usecase.ClassifyArchitectureService;
import dev.mauricioandrade.shisho.application.usecase.RankCalculator;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Cost;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.KataExample;
import dev.mauricioandrade.shisho.domain.model.KataExample.Snippet;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.model.Verdict;
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
    void explainsAnOverengineeringVerdict() {
        Verdict verdict = new Verdict.Overengineering(List.of(
                new Evidence("um_use_case_por_metodo_crud", List.of("Usuario", 4)),
                new Evidence("entidade_de_dominio_identica_a_entidade_jpa", List.of("Usuario", "UsuarioJpaEntity"))),
                true, Optional.of("package-by-feature"));

        assertThat(render(withArchitecture(verdict), PORTUGUESE)).isEqualTo("""
                師匠 Shishō — análise de loja  (Java 21)
                Arquitetura: Clean Architecture  (67% da assinatura: domain, application, usecase, domain/model)

                Veredito: overengineering. A arquitetura Clean Architecture custa de 7 a 12 classes por
                funcionalidade, e nada no contexto declarado paga esse custo. Ela vale a pena quando:
                dominio_com_regras_complexas, vida_util_longa.
                  · 4 use cases só para operações CRUD de Usuario: um use case por método, sem regra de negócio que
                    justifique cada um.
                  · Usuario e a entidade JPA UsuarioJpaEntity têm exatamente os mesmos campos: duas cópias da mesma
                    estrutura, mais o mapeamento entre elas.
                Considere package-by-feature.

                Graduação: 1º dan  (nenhuma lição)
                """);
    }

    @Test
    void explainsUnderengineeringAndAdequateVerdicts() {
        Verdict under = new Verdict.Underengineering(
                List.of(new Evidence("service_deus", List.of("PedidoService", 31, 9))), List.of("modular-monolith"));
        assertThat(render(withArchitecture(under), PORTUGUESE)).contains("""

                Veredito: subengenharia. O projeto já não cabe na arquitetura Clean Architecture:
                  · PedidoService tem 31 métodos e 9 dependências no construtor.
                Próximo passo natural: modular-monolith.

                """);

        assertThat(render(withArchitecture(new Verdict.Adequate(true, List.of("vida_util_longa"))), PORTUGUESE)).contains(
                "\nVeredito: a arquitetura Clean Architecture é adequada. O contexto declarado paga o custo dela:\nvida_util_longa.\n");
        assertThat(render(withArchitecture(new Verdict.Adequate(false, List.of())), PORTUGUESE)).contains(
                "\nVeredito: nenhum sinal de overengineering ou subengenharia foi medido na arquitetura Clean\nArchitecture. Declare o contexto");
    }

    @Test
    void theSenseiShowsHowToFixEachKataOnce() {
        Analysis analysis = analysis(true, List.of(
                lesson("field-injection", Severity.GRAVE, 14, "a"),
                lesson("field-injection", Severity.GRAVE, 20, "b")), List.of());
        KataExample example = new KataExample("field-injection",
                new Snippet("java", "@Autowired\nprivate Service service;", ""),
                new Snippet("java", "private final Service service;\n\nApp(Service service) {}", "O construtor declara a `dependência`."));

        assertThat(render(analysis, PORTUGUESE, List.of(example))).contains("""

                Como corrigir (modo sensei)

                [field-injection] Injeção de dependência em campo
                  Antes:
                      @Autowired
                      private Service service;
                  Depois:
                    O construtor declara a `dependência`.
                      private final Service service;

                      App(Service service) {}

                Graduação:""");
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
        return render(analysis, messages, List.of());
    }

    private static String render(Analysis analysis, Messages messages, List<KataExample> examples) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new TerminalReportWriter(out, messages, examples).write(analysis);
        return buffer.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }

    private static Analysis analysis(boolean versionDeclared, List<Lesson> lessons, List<SourceFile.Unparseable> unparseable) {
        int score = new RankCalculator().score(lessons);
        return new Analysis(Path.of("loja"), "loja", new JavaVersion(21), versionDeclared, Optional.empty(), lessons, unparseable, score, Rank.forScore(score));
    }

    private static Analysis withArchitecture(Verdict verdict) {
        ArchitectureProfile clean = new ArchitectureProfile("clean", "Clean Architecture", Family.DOMAIN_ISOLATION,
                new Signature(List.of("domain", "application", "infrastructure", "usecase", "domain/model", "domain/port"), List.of(), 3),
                List.of(), new Cost(new ClassRange(7, 12), Complexity.HIGH), List.of("dominio_com_regras_complexas", "vida_util_longa"),
                List.of(), List.of(), List.of(), Optional.of("package-by-feature"), List.of(), "");
        ArchitectureMatch match = new ArchitectureMatch(
                clean, List.of("domain", "application", "usecase", "domain/model"), 67, List.of(), verdict);
        return new Analysis(Path.of("loja"), "loja", new JavaVersion(21), true, Optional.of(match), List.of(), List.of(), 100, Rank.DAN_1);
    }

    private static Lesson lesson(String kata, Severity severity, int line, String argument) {
        return new Lesson(kata, severity, Path.of("src/main/java/app/UsuarioController.java"), line,
                "kata." + kata + ".lesson", List.of(argument));
    }
}

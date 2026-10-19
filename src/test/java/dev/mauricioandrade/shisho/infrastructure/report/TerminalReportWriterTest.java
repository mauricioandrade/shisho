package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.domain.model.Baseline;
import dev.mauricioandrade.shisho.application.usecase.AnalyzeProjectService;
import dev.mauricioandrade.shisho.application.usecase.ClassifyArchitectureService;
import dev.mauricioandrade.shisho.application.usecase.RankCalculator;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Concept;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding.Occurrence;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Cost;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.KataGuide.Snippet;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.model.Verdict;
import dev.mauricioandrade.shisho.infrastructure.settings.ApplicationSettingsReader;
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
                         usuarioService é injetado direto no campo. Assim, new cria a classe sem a dependência, o
                         erro só aparece como NullPointerException em uso e o teste precisa de Spring ou reflexão.
                         Receba a dependência pelo construtor, num campo private final.
                ! MEDIA  [request-body-without-valid]  UsuarioController.java:1234
                         @RequestBody CriarUsuarioRequest sem @Valid: as anotações de validação do DTO são
                         ignoradas, e dados inválidos chegam ao service como se fossem válidos. Adicione @Valid
                         antes do @RequestBody.

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
    void doesNotSuggestASimplerArchitectureWhenTheContextJustifiesIt() {
        Verdict verdict = new Verdict.Overengineering(List.of(
                new Evidence("entidade_de_dominio_identica_a_entidade_jpa", List.of("Usuario", "UsuarioJpaEntity"))),
                false, Optional.of("package-by-feature"), List.of("dominio_com_regras_complexas"));

        assertThat(render(withArchitecture(verdict), PORTUGUESE)).contains("""
                Veredito: overengineering, por enquanto. O contexto declarado justifica a arquitetura Clean
                Architecture (dominio_com_regras_complexas), mas o código ainda não usa o que ela oferece:
                """).contains("Esses sinais somem quando o domínio ganhar regras próprias.")
                .doesNotContain("Considere package-by-feature");
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
        KataGuide guide = new KataGuide("field-injection",
                List.of("O objeto nasce `completo`.", "E o teste não precisa de Spring."),
                List.of("Em testes."),
                new Snippet("java", "@Autowired\nprivate Service service;", ""),
                new Snippet("java", "private final Service service;\n\nApp(Service service) {}", "O construtor declara a `dependência`."),
                List.of("https://docs.spring.io/a", "https://example.com/b"));

        assertThat(render(analysis, PORTUGUESE, List.of(guide))).contains("""

                Por que e como corrigir (modo sensei)

                [field-injection] Injeção de dependência em campo
                  Por quê:
                    O objeto nasce `completo`.

                    E o teste não precisa de Spring.
                  Quando é aceitável:
                    Em testes.
                  Antes:
                      @Autowired
                      private Service service;
                  Depois:
                    O construtor declara a `dependência`.
                      private final Service service;

                      App(Service service) {}
                  Referências:
                    https://docs.spring.io/a
                    https://example.com/b

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
                          UsuarioRequest looks like a DTO but is not a record. As a record it becomes immutable and
                          drops the hand-written constructor, getters, equals, hashCode and toString.

                Rank: 1st dan  (1 lesson: 0 severe, 0 medium, 1 minor)
                """);
    }

    @Test
    void reportsTheMessyStudentFixture() {
        AnalyzeProjectService service = new AnalyzeProjectService(
                new MavenBuildFileReader(), new YamlProjectConfigReader(), new JavaParserSourceParser(), Katas.all(MarkdownCatalogRepository.bundled().avoidedDependencies()),
                new ClassifyArchitectureService(MarkdownCatalogRepository.bundled(), Detectors.all()), new RankCalculator(),
                List.of(), List.of(), _ -> Optional.empty(), new ApplicationSettingsReader());

        String report = render(service.analyze(Path.of("src/test/resources/fixtures/aluno-baguncado")), PORTUGUESE);

        assertThat(report)
                .startsWith("""
                        師匠 Shishō — análise de aluno-baguncado  (Java 21)
                        Arquitetura: Camadas técnicas (Layered)  (83% da assinatura: controller, service, repository, model, dto)

                        ! MEDIA  [open-in-view]""")
                .endsWith("\nGraduação: 6º kyū  (21 lições: 2 graves, 10 médias, 6 leves, 3 sugestões)\n");
    }

    @Test
    void explainsTheConceptsFoundInTheShoshinMode() {
        Concept record = new Concept("record", "Record", Concept.Group.JAVA, 10,
                List.of(), List.of("https://openjdk.org/jeps/395"), List.of("Uma classe feita só para carregar dados.", "Desde o Java 16."),
                new Snippet("java", "record R() {}", ""));
        Concept strategy = new Concept("strategy", "Strategy", Concept.Group.PATTERN, 30,
                List.of(), List.of("https://refactoring.guru/design-patterns/strategy"), List.of("Várias formas de fazer a mesma coisa."),
                new Snippet("java", "interface S {}", ""));
        Path dto = Path.of("src/main/java/app/dto");
        Analysis analysis = new Analysis(Path.of("loja"), "loja", new JavaVersion(21), true, Optional.empty(), Optional.empty(),
                List.of(), List.of(), 100, Rank.DAN_1, List.of(
                new ConceptFinding("record", List.of(new Occurrence(dto.resolve("A.java"), 3), new Occurrence(dto.resolve("B.java"), 3),
                        new Occurrence(dto.resolve("C.java"), 3), new Occurrence(dto.resolve("D.java"), 3))),
                new ConceptFinding("strategy", List.of(new Occurrence(Path.of("src/main/java/app/Frete.java"), 5)))));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new TerminalReportWriter(new PrintStream(buffer, true, StandardCharsets.UTF_8), PORTUGUESE, List.of(), List.of(record, strategy))
                .write(analysis);

        assertThat(buffer.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n")).isEqualTo("""
                師匠 Shishō — análise de loja  (Java 21)
                Arquitetura: nenhuma do catálogo reconhecida

                初心 Conceitos neste código (modo shoshin)

                Java
                  Record  (4 lugares: A.java:3, B.java:3, C.java:3 e mais 1)
                    Uma classe feita só para carregar dados.

                    Desde o Java 16.

                    Para ler mais: https://openjdk.org/jeps/395

                Padrões de projeto
                  Strategy  (1 lugar: Frete.java:5)
                    Várias formas de fazer a mesma coisa.

                    Para ler mais: https://refactoring.guru/design-patterns/strategy

                Graduação: 1º dan  (nenhuma lição)
                """);
    }

    @Test
    void saysSoWhenNoConceptShowsUp() {
        Concept record = new Concept("record", "Record", Concept.Group.JAVA, 10, List.of(), List.of("https://x.dev"),
                List.of("X."), new Snippet("java", "record R() {}", ""));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new TerminalReportWriter(new PrintStream(buffer, true, StandardCharsets.UTF_8), PORTUGUESE, List.of(), List.of(record))
                .write(analysis(true, List.of(), List.of()));

        assertThat(buffer.toString(StandardCharsets.UTF_8)).contains("Nenhum conceito do catálogo apareceu neste código.");
    }

    @Test
    void saysWhatTheBaselineHidAndWhatWasFixedBeforeTheRank() {
        Lesson fresh = lesson("field-injection", Severity.GRAVE, 14, "repository");
        Lesson known = lesson("system-out", Severity.MEDIA, 30, "System.out");
        Analysis analysis = new Analysis(Path.of("loja"), "loja", new JavaVersion(21), true, Optional.empty(), Optional.empty(),
                List.of(fresh), List.of(), 86, Rank.forScore(86), List.of(),
                Optional.of(new Baseline.Comparison(List.of(fresh), List.of(known), 2)));

        assertThat(render(analysis, PORTUGUESE))
                .contains("[field-injection]")
                .doesNotContain("[system-out]")
                .endsWith("""
                        1 lição conhecida da baseline não foi mostrada.
                        2 lições da baseline foram corrigidas: rode shisho baseline para atualizá-la.
                        Graduação: 1º kyū  (1 lição: 1 grave, 0 médias, 0 leves)
                        """);
    }

    private static String render(Analysis analysis, Messages messages) {
        return render(analysis, messages, List.of());
    }

    private static String render(Analysis analysis, Messages messages, List<KataGuide> guides) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new TerminalReportWriter(out, messages, guides).write(analysis);
        return buffer.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }

    private static Analysis analysis(boolean versionDeclared, List<Lesson> lessons, List<SourceFile.Unparseable> unparseable) {
        int score = new RankCalculator().score(lessons);
        return new Analysis(Path.of("loja"), "loja", new JavaVersion(21), versionDeclared, Optional.empty(), Optional.empty(), lessons, unparseable, score, Rank.forScore(score));
    }

    private static Analysis withArchitecture(Verdict verdict) {
        ArchitectureProfile clean = new ArchitectureProfile("clean", "Clean Architecture", Family.DOMAIN_ISOLATION,
                new Signature(List.of("domain", "application", "infrastructure", "usecase", "domain/model", "domain/port"), List.of(), 3),
                List.of(), new Cost(new ClassRange(7, 12), Complexity.HIGH), List.of("dominio_com_regras_complexas", "vida_util_longa"),
                List.of(), List.of(), List.of(), Optional.of("package-by-feature"), List.of(), "");
        ArchitectureMatch match = new ArchitectureMatch(
                clean, List.of("domain", "application", "usecase", "domain/model"), 67, List.of(), verdict);
        return new Analysis(Path.of("loja"), "loja", new JavaVersion(21), true, Optional.empty(), Optional.of(match), List.of(), List.of(), 100, Rank.DAN_1);
    }

    private static Lesson lesson(String kata, Severity severity, int line, String argument) {
        return new Lesson(kata, severity, Path.of("src/main/java/app/UsuarioController.java"), line,
                "kata." + kata + ".lesson", List.of(argument));
    }
}

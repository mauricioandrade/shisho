package dev.mauricioandrade.shisho.infrastructure.llm;

import dev.mauricioandrade.shisho.application.usecase.AnalyzeProjectService;
import dev.mauricioandrade.shisho.application.usecase.ClassifyArchitectureService;
import dev.mauricioandrade.shisho.application.usecase.RankCalculator;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.infrastructure.build.MavenBuildFileReader;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.config.YamlProjectConfigReader;
import dev.mauricioandrade.shisho.infrastructure.detector.Detectors;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.kata.Katas;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SenseiPromptTest {

    private static final MarkdownCatalogRepository CATALOG = MarkdownCatalogRepository.bundled();

    private final SenseiPrompt prompt = new SenseiPrompt(new Messages(Messages.DEFAULT_LOCALE), CATALOG.architectures());

    @Test
    void givesTheModelTheVerdictAndTheArchitecturesItPointsTo() {
        String facts = prompt.facts(analyze("aluno-astronauta"), Optional.empty());

        assertThat(facts)
                .startsWith("""
                        師匠 Shishō — análise de aluno-astronauta  (Java 21)
                        Arquitetura: Clean Architecture  (100% da assinatura: \
                        """)
                .contains("\n- 4 use cases só para operações CRUD de Usuario")
                .contains("""
                        Graduação: 1º dan  (nenhuma lição)
                        Nota: 100 de 100.

                        ## Veredito

                        Veredito: overengineering. A arquitetura Clean Architecture custa de 7 a 12 classes\
                        """)
                .contains("""
                        Considere package-by-feature.

                        ## Lições

                        Nenhuma lição.
                        """)
                .contains("\n## Arquiteturas do catálogo\n\n### Clean Architecture\n\n#### O que é\n")
                .contains("\n### Pacotes por funcionalidade (Package by Feature)\n\n#### O que é\n")
                .endsWith("""
                        ## Pergunta

                        Explique o veredito e a graduação deste projeto: o que significam, quais lições pesam mais e por \
                        onde o autor deveria começar.
                        """);
    }

    @Test
    void groupsTheLessonsBySeverityThenKataAndShowsOnlyTheFirstFew() {
        List<Lesson> lessons = Stream.concat(
                Stream.of(lesson("dto-not-record", Severity.LEVE, 9, "UsuarioRequest")),
                IntStream.rangeClosed(1, 5).mapToObj(line -> lesson("field-injection", Severity.GRAVE, line, "dependencia" + line))).toList();

        String facts = prompt.facts(analysis(lessons), Optional.of("Por onde começo?"));

        assertThat(facts).isEqualTo("""
                師匠 Shishō — análise de loja  (Java 21)
                Arquitetura: nenhuma do catálogo reconhecida
                Graduação: 5º kyū  (6 lições: 5 graves, 0 médias, 1 leve)
                Nota: 49 de 100.

                ## Veredito

                Sem veredito: o projeto não se parece com nenhuma arquitetura do catálogo.

                ## Lições

                ### GRAVE: 5 lições, cada uma tira 10 pontos

                - [field-injection] Injeção de dependência em campo: 5 lições
                  - Loja.java:1: Injeção em campo em dependencia1. Use construtor explícito com private final: a dependência fica visível e o objeto, imutável.
                  - Loja.java:2: Injeção em campo em dependencia2. Use construtor explícito com private final: a dependência fica visível e o objeto, imutável.
                  - Loja.java:3: Injeção em campo em dependencia3. Use construtor explícito com private final: a dependência fica visível e o objeto, imutável.
                  - e mais 2 lições desta kata.

                ### LEVE: 1 lição, cada uma tira 1 ponto

                - [dto-not-record] DTO que não é record: 1 lição
                  - Loja.java:9: %s

                ## Pergunta

                Por onde começo?
                """.formatted(new Messages(Messages.DEFAULT_LOCALE).text("kata.dto-not-record.lesson", "UsuarioRequest")));
    }

    @Test
    void saysWhichSuggestionsTakeNoPoints() {
        Lesson suggestion = new Lesson("missing-springdoc", Severity.INFO, Path.of("pom.xml"), 0, "kata.missing-springdoc.lesson", List.of());

        assertThat(prompt.facts(analysis(List.of(suggestion)), Optional.empty()))
                .contains("\n### INFO: 1 lição, sugestões que não tiram nota\n\n- [missing-springdoc] API sem documentação OpenAPI: 1 lição\n  - pom.xml: ");
    }

    @Test
    void explainsTheVerdictWhenTheQuestionIsBlank() {
        assertThat(prompt.facts(analysis(List.of()), Optional.of("  ")))
                .contains("\nNenhuma lição.\n")
                .doesNotContain("## Arquiteturas do catálogo")
                .endsWith("\nExplique o veredito e a graduação deste projeto: o que significam, quais lições pesam mais e por "
                        + "onde o autor deveria começar.\n");
    }

    @Test
    void tellsTheModelToExplainAndNeverToJudge() {
        assertThat(prompt.instructions())
                .contains("Não invente lições")
                .contains("na segunda pessoa (você)")
                .contains("Não mude o veredito, a graduação nem a severidade das lições")
                .contains("diga isso em vez de supor");
    }

    private static Analysis analyze(String fixture) {
        return new AnalyzeProjectService(
                new MavenBuildFileReader(), new YamlProjectConfigReader(), new JavaParserSourceParser(), Katas.all(CATALOG.avoidedDependencies()),
                new ClassifyArchitectureService(CATALOG, Detectors.all()), new RankCalculator())
                .analyze(Path.of("src/test/resources/fixtures", fixture));
    }

    private static Analysis analysis(List<Lesson> lessons) {
        int score = new RankCalculator().score(lessons);
        return new Analysis(Path.of("loja"), "loja", new JavaVersion(21), true, Optional.empty(), Optional.empty(), lessons, List.of(),
                score, Rank.forScore(score));
    }

    private static Lesson lesson(String kata, Severity severity, int line, String argument) {
        return new Lesson(kata, severity, Path.of("src/main/java/Loja.java"), line, "kata." + kata + ".lesson", List.of(argument));
    }
}

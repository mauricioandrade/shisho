package dev.mauricioandrade.shisho;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** End to end, from the command line arguments to the exit code. */
class ShishoBootstrapTest {

    private static final String FIXTURES = "src/test/resources/fixtures/";

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Test
    void exitsWithOneWhenThereAreGraveLessons() {
        assertThat(run("analyze", FIXTURES + "aluno-baguncado")).isEqualTo(1);
        assertThat(out()).contains("✗ GRAVE  [field-injection]", "\nGraduação: ");
        assertThat(err()).isEmpty();
    }

    @Test
    void exitsWithZeroForTheModelStudent() {
        assertThat(run("analyze", FIXTURES + "aluno-exemplar")).isZero();
        assertThat(out()).endsWith("Graduação: 1º dan  (nenhuma lição)\n");
    }

    @Test
    void exitsWithZeroForTheQuarkusModelStudent() {
        assertThat(run("analyze", FIXTURES + "aluno-quarkus")).isZero();
        assertThat(out()).endsWith("Graduação: 1º dan  (nenhuma lição)\n");
    }

    @Test
    void explainsTheQuarkusWayInEnglishToo() {
        assertThat(ShishoBootstrap.run(new String[] {"analyze", FIXTURES + "aluno-quarkus-baguncado"}, "en", out, err)).isZero();
        assertThat(out().replaceAll("\\s+", " ")).contains(
                "Web application without quarkus-smallrye-health.",
                "Group them in an interface with @ConfigMapping(prefix = \"loja.frete\")",
                "@ServerExceptionMapper methods",
                "Body CriarPedidoRequest without @Valid");
    }

    @Test
    void theSenseiShowsTheQuarkusWayToAQuarkusStudent() {
        assertThat(run("analyze", "--sensei", FIXTURES + "aluno-quarkus-baguncado")).isZero();
        assertThat(out()).contains("Log.debugf(", "@ServerExceptionMapper", "<artifactId>quarkus-smallrye-health</artifactId>")
                .doesNotContain("LoggerFactory", "spring-boot-starter-actuator");
    }

    @Test
    void aGradleBuildTeachesTheSameLessonsAsMaven(@TempDir Path temp) throws IOException {
        assertThat(run("analyze", FIXTURES + "aluno-quarkus-baguncado")).isZero();
        String maven = out();
        out.reset();
        Path gradle = temp.resolve("aluno-quarkus-baguncado");
        copyFixture("aluno-quarkus-baguncado", gradle);
        Files.delete(gradle.resolve("pom.xml"));
        Files.writeString(gradle.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("io.quarkus")
                }

                dependencies {
                    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.28.2"))
                    implementation("io.quarkus:quarkus-rest-jackson")
                    implementation("io.quarkus:quarkus-hibernate-validator")
                    implementation("io.quarkus:quarkus-hibernate-orm-panache")
                }

                java {
                    toolchain {
                        languageVersion = JavaLanguageVersion.of(21)
                    }
                }
                """);

        assertThat(run("analyze", gradle.toString())).isZero();
        assertThat(out()).contains("build.gradle.kts").isEqualTo(maven.replace("pom.xml", "build.gradle.kts"));
    }

    @Test
    void aMultiModuleProjectIsAnalyzedAsAWhole(@TempDir Path temp) throws IOException {
        Path project = temp.resolve("loja");
        copyFixture("aluno-baguncado", project.resolve("app"));
        Files.createDirectories(project.resolve("dominio/src/main/java/com/exemplo/loja/dominio"));
        Files.writeString(project.resolve("dominio/pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><artifactId>dominio</artifactId></project>
                """);
        Files.writeString(project.resolve("dominio/src/main/java/com/exemplo/loja/dominio/Preco.java"), """
                package com.exemplo.loja.dominio;

                public record Preco(long centavos) {
                    public Preco {
                        System.out.println(centavos);
                    }
                }
                """);
        Files.writeString(project.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><artifactId>loja</artifactId><packaging>pom</packaging>
                <modules><module>dominio</module><module>app</module></modules></project>
                """);

        assertThat(run("analyze", project.toString())).isEqualTo(1);
        assertThat(out())
                .contains("análise de loja  (Java 21)")
                .contains("Arquitetura: Camadas técnicas (Layered)")
                .contains("(20 lições: 1 grave, 10 médias, 6 leves, 3 sugestões)");

        Path sarif = temp.resolve("shisho.sarif");
        assertThat(run("analyze", "--format", "SARIF", "--output", sarif.toString(), project.toString())).isEqualTo(1);
        assertThat(Files.readString(sarif)).as("each lesson in the file of its module")
                .contains("\"uri\": \"app/pom.xml\"")
                .contains("\"uri\": \"dominio/src/main/java/com/exemplo/loja/dominio/Preco.java\"")
                .contains("\"uri\": \"app/src/main/java/com/exemplo/loja/controller/UsuarioController.java\"")
                .doesNotContain("\"uri\": \"pom.xml\"");
    }

    @Test
    void judgesAnOverengineeredProject() {
        assertThat(run("analyze", FIXTURES + "aluno-astronauta")).isZero();
        assertThat(out())
                .contains("Arquitetura: Clean Architecture  (100% da assinatura: ")
                .contains("\nVeredito: overengineering. A arquitetura Clean Architecture custa de 7 a 12 classes")
                .contains("  · Um mapper converte UsuarioJpaEntity em Usuario")
                .contains("  · Usuario e a entidade JPA UsuarioJpaEntity")
                .contains("  · 4 use cases só para operações CRUD de Usuario")
                .contains("\nConsidere package-by-feature.\n")
                .endsWith("Graduação: 1º dan  (nenhuma lição)\n");
    }

    @Test
    void theShishoAnalyzesItselfWithoutGraveLessons() {
        assertThat(run("analyze", ".")).isZero();
        assertThat(out()).startsWith("師匠 Shishō — análise de ").contains("  (Java 25)\n")
                .contains("\nVeredito: a arquitetura Clean Architecture é adequada. O contexto declarado paga o custo dela:\n"
                        + "vida_util_longa, troca_provavel_de_infra.\n");
    }

    @Test
    void exitsWithTwoForAPathThatIsNotADirectory() {
        assertThat(run("analyze", "/nao/existe")).isEqualTo(2);
        assertThat(err()).isEqualTo("/nao/existe não é um diretório.\n");
        assertThat(out()).isEmpty();
    }

    @Test
    void exitsWithTwoForMissingArguments() {
        assertThat(run("analyze")).isEqualTo(2);
        assertThat(err()).contains("Uso: shisho analyze");
    }

    @Test
    void exitsWithThreeWhenTheProjectCannotBeAnalyzed(@TempDir Path project) throws IOException {
        Files.writeString(project.resolve("pom.xml"), "<project><properties><java.version>1.7</java.version></properties></project>");

        assertThat(run("analyze", project.toString())).isEqualTo(3);
        assertThat(err()).isEqualTo("O projeto usa Java 7; o Shishō analisa Java 8 ou superior.\n");
    }

    @Test
    void followsTheProjectConfiguration(@TempDir Path project) throws IOException {
        Path controller = Files.createDirectories(project.resolve("src/main/java/app")).resolve("UserController.java");
        Files.writeString(controller, "package app; class UserController { @Autowired private UserService service; }");
        assertThat(run("analyze", project.toString())).isEqualTo(1);

        Files.writeString(project.resolve("shisho.yaml"), "katas:\n  field-injection:\n    severidade: leve\n");
        assertThat(run("analyze", project.toString())).isZero();
        assertThat(out()).contains("· LEVE   [field-injection]");
    }

    @Test
    void exitsWithThreeForAnInvalidConfiguration(@TempDir Path project) throws IOException {
        Files.writeString(project.resolve("shisho.yaml"), "katas:\n  feild-injection:\n    ativa: false\n");

        assertThat(run("analyze", project.toString())).isEqualTo(3);
        assertThat(err()).startsWith("shisho.yaml: kata desconhecida: \"feild-injection\". Katas disponíveis: ");
    }

    @Test
    void writesSarifToAFileAndKeepsTheExitCode(@TempDir Path directory) throws IOException {
        Path sarif = directory.resolve("shisho.sarif");

        assertThat(run("analyze", "--format", "SARIF", "--output", sarif.toString(), FIXTURES + "aluno-baguncado")).isEqualTo(1);
        assertThat(out()).isEmpty();
        assertThat(Files.readString(sarif))
                .startsWith("{\n  \"$schema\": \"https://json.schemastore.org/sarif-2.1.0.json\"")
                .contains("\"uri\": \"src/test/resources/fixtures/aluno-baguncado/src/main/java/com/exemplo/loja/controller/UsuarioController.java\"")
                .contains("\"ruleId\": \"dependency-rule\"");
    }

    @Test
    void theSenseiModeShowsTheCatalogGuides() {
        assertThat(run("analyze", "--sensei", FIXTURES + "aluno-baguncado")).isEqualTo(1);
        assertThat(out()).contains("\nPor que e como corrigir (modo sensei)\n\n[missing-actuator] Aplicação web sem health check\n  Por quê:\n")
                .contains("  Referências:\n    https://docs.spring.io/spring-boot/reference/actuator/endpoints.html\n");

        out.reset();
        run("analyze", FIXTURES + "aluno-baguncado");
        assertThat(out()).doesNotContain("modo sensei");
    }

    @Test
    void writesHtmlToTheScreen() {
        assertThat(run("analyze", "--format", "html", FIXTURES + "aluno-astronauta")).isZero();
        assertThat(out()).startsWith("<!doctype html>").contains("verdict-over").endsWith("</html>\n");
    }

    @Test
    void exitsWithTwoForAnUnknownFormatOrAnUnwritableOutput(@TempDir Path directory) {
        assertThat(run("analyze", "--format", "pdf", FIXTURES + "aluno-exemplar")).isEqualTo(2);
        assertThat(err()).contains("--format");

        err.reset();
        Path unwritable = directory.resolve("nao/existe/shisho.sarif");
        assertThat(run("analyze", "--output", unwritable.toString(), FIXTURES + "aluno-exemplar")).isEqualTo(2);
        assertThat(err()).isEqualTo("Não foi possível gravar o relatório em " + unwritable + ".\n");
    }

    @Test
    void asksOnlyWhenOllamaAnswersAndSaysHowToStartIt() {
        assertThat(run("ask", "--url", "http://127.0.0.1:1", FIXTURES + "aluno-exemplar")).isEqualTo(3);
        assertThat(out()).isEmpty();
        assertThat(err()).isEqualTo("""
                Perguntando ao gemma3:4b em http://127.0.0.1:1...
                Não foi possível falar com o Ollama em http://127.0.0.1:1. Ele está rodando? Inicie com: ollama serve
                """);
    }

    @Test
    void hasNothingToFixInTheShisho() {
        assertThat(run("fix", ".")).isZero();
        assertThat(out()).isEqualTo("Nenhuma lição: nada a corrigir.\n");
    }

    @Test
    void fixesTheMessyStudentOnlyWithApply(@TempDir Path copy) throws IOException {
        Path service = Path.of("src/main/java/com/exemplo/loja/service/PedidoService.java");
        Files.createDirectories(copy.resolve(service).getParent());
        Files.copy(Path.of(FIXTURES + "aluno-baguncado/pom.xml"), copy.resolve("pom.xml"));
        Files.copy(Path.of(FIXTURES + "aluno-baguncado").resolve(service), copy.resolve(service));

        assertThat(run("fix", copy.toString())).isZero();
        assertThat(out()).contains("-    private PedidoRepository pedidoRepository;\n+    private final PedidoRepository pedidoRepository;\n");
        assertThat(Files.readString(copy.resolve(service))).contains("    private PedidoRepository pedidoRepository;");

        out.reset();
        assertThat(run("fix", "--apply", copy.toString())).isZero();
        assertThat(Files.readString(copy.resolve(service))).contains("    private final PedidoRepository pedidoRepository;");
        assertThat(run("analyze", copy.toString())).isZero();
        assertThat(out()).doesNotContain("[non-final-dependency]");
    }

    @Test
    void showsUsageInPortugueseWithoutArguments() {
        assertThat(run()).isZero();
        assertThat(out()).startsWith("Uso: shisho").contains("Comandos:", "analyze");
    }

    /** The story docs/demo/demo.tape records: four lessons, three fixed by fix --apply, 1st dan. */
    @Test
    void theDemoProjectTellsTheDemoStory(@TempDir Path project) throws IOException {
        copyFixture("demo-loja", project);

        assertThat(run("analyze", project.toString())).isEqualTo(1);
        assertThat(out()).contains("[field-injection]", "[entity-in-controller]", "[request-body-without-valid]", "[transactional-read-only]")
                .endsWith("Graduação: 2º kyū  (4 lições: 1 grave, 2 médias, 1 leve)\n");
        out.reset();
        assertThat(run("fix", "--apply", project.toString())).isZero();
        assertThat(out()).startsWith("Corrigido: 3 lições em 2 arquivos.\n");
        out.reset();
        assertThat(run("analyze", project.toString())).isZero();
        assertThat(out()).endsWith("Graduação: 1º dan  (1 lição: 0 graves, 1 média, 0 leves)\n");
    }

    @Test
    void namesTheConceptsInTheCodeInTheShoshinMode() {
        assertThat(run("analyze", "--shoshin", FIXTURES + "aluno-astronauta")).isZero();
        assertThat(out()).contains("初心 Conceitos neste código (modo shoshin)", "\nPadrões de projeto\n", "  Repository  (")
                .endsWith("Graduação: 1º dan  (nenhuma lição)\n");
    }

    @Test
    void namesTheQuarkusConceptsInTheShoshinMode() {
        assertThat(run("analyze", "--shoshin", FIXTURES + "aluno-quarkus-baguncado")).isZero();
        assertThat(out()).contains("\nQuarkus\n  Beans CDI e escopos  (", "\nJakarta EE\n  Entidade JPA  (", "  Active Record  (");
    }

    @Test
    void speaksTheLanguageOfShishoIdioma() {
        assertThat(ShishoBootstrap.run(new String[] {"analyze", FIXTURES + "aluno-astronauta"}, "EN", out, err)).isZero();
        assertThat(out()).startsWith("師匠 Shishō — analysis of aluno-astronauta").contains("\nVerdict: overengineering.");
    }

    @Test
    void refusesALanguageItDoesNotSpeak() {
        assertThat(ShishoBootstrap.run(new String[] {"analyze", "."}, "es", out, err)).isEqualTo(2);
        assertThat(err()).startsWith("SHISHO_IDIOMA=es não é um idioma do Shishō: use pt ou en.");
        assertThat(out()).isEmpty();
    }

    private static void copyFixture(String fixture, Path target) throws IOException {
        Path source = Path.of(FIXTURES + fixture);
        try (Stream<Path> files = Files.walk(source)) {
            for (Path file : files.toList()) {
                Path copy = target.resolve(source.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(copy);
                } else {
                    Files.copy(file, copy);
                }
            }
        }
    }

    private int run(String... args) {
        return ShishoBootstrap.run(args, out, err);
    }

    private String out() {
        return out.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }

    private String err() {
        return err.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }
}

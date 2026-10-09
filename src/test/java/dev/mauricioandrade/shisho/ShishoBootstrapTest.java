package dev.mauricioandrade.shisho;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
        Files.writeString(project.resolve("pom.xml"), "<project><properties><java.version>17</java.version></properties></project>");

        assertThat(run("analyze", project.toString())).isEqualTo(3);
        assertThat(err()).isEqualTo("O projeto usa Java 17; o Shishō analisa Java 21 ou superior.\n");
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
    void exitsWithTwoForAnUnknownFormatOrAnUnwritableOutput(@TempDir Path directory) {
        assertThat(run("analyze", "--format", "pdf", FIXTURES + "aluno-exemplar")).isEqualTo(2);
        assertThat(err()).contains("--format");

        err.reset();
        Path unwritable = directory.resolve("nao/existe/shisho.sarif");
        assertThat(run("analyze", "--output", unwritable.toString(), FIXTURES + "aluno-exemplar")).isEqualTo(2);
        assertThat(err()).isEqualTo("Não foi possível gravar o relatório em " + unwritable + ".\n");
    }

    @Test
    void showsUsageInPortugueseWithoutArguments() {
        assertThat(run()).isZero();
        assertThat(out()).startsWith("Uso: shisho").contains("Comandos:", "analyze");
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

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
    void theShishoAnalyzesItselfWithoutGraveLessons() {
        assertThat(run("analyze", ".")).isZero();
        assertThat(out()).startsWith("師匠 Shishō — análise de ").contains("  (Java 25)\n");
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

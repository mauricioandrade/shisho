package dev.mauricioandrade.shisho.infrastructure.config;

import dev.mauricioandrade.shisho.domain.exception.ConfigException;
import dev.mauricioandrade.shisho.domain.model.Baseline;
import dev.mauricioandrade.shisho.domain.model.Baseline.Entry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YamlBaselineFileTest {

    private final YamlBaselineFile file = new YamlBaselineFile();

    @TempDir
    Path project;

    @Test
    void aProjectWithoutBaselineHasNone() {
        assertThat(file.read(project)).isEmpty();
    }

    @Test
    void writesOneEntryPerLessonWithoutLines() throws IOException {
        Path written = file.write(project, new Baseline(List.of(
                new Entry("field-injection", "src/main/java/app/A.java", "kata.field-injection.lesson", List.of("repository")),
                new Entry("missing-springdoc", "pom.xml", "kata.missing-springdoc.lesson", List.of()))));

        assertThat(written).isEqualTo(Path.of("shisho-baseline.yaml"));
        assertThat(Files.readString(project.resolve(written))).isEqualTo("""
                licoes:
                - kata: missing-springdoc
                  arquivo: pom.xml
                  mensagem: kata.missing-springdoc.lesson
                  argumentos: []
                - kata: field-injection
                  arquivo: src/main/java/app/A.java
                  mensagem: kata.field-injection.lesson
                  argumentos: [repository]
                """);
    }

    @Test
    void readsBackWhatItWrote() {
        Baseline baseline = new Baseline(List.of(
                new Entry("request-body-without-valid", "src/main/java/app/Pedidos.java", "kata.request-body-without-valid.lesson",
                        List.of("List<PedidoDto>", "jakarta.validation.Valid")),
                new Entry("dto-not-record", "src/main/java/app/Ação.java", "kata.dto-not-record.lesson",
                        List.of("010", "true", "null", "a: b", "- x", "#1", ""))));

        file.write(project, baseline);

        assertThat(file.read(project)).contains(baseline);
    }

    @Test
    void anInvalidBaselineSaysWhereAndHowToRecordItAgain() throws IOException {
        Files.writeString(project.resolve("shisho-baseline.yaml"), """
                licoes:
                - kata: field-injection
                  arquivo: A.java
                  mensagem: kata.field-injection.lesson
                - kata: field-injection
                """);

        assertThatThrownBy(() -> file.read(project)).isInstanceOfSatisfying(ConfigException.class, e -> {
            assertThat(e.messageKey()).isEqualTo("baseline.error.invalid");
            assertThat(e.arguments()).containsExactly("shisho-baseline.yaml", "licoes[0]");
        });
    }

    @Test
    void aFileWithoutLessonsIsInvalid() throws IOException {
        Files.writeString(project.resolve("shisho-baseline.yaml"), "katas: {}\n");

        assertThatThrownBy(() -> file.read(project)).isInstanceOfSatisfying(ConfigException.class,
                e -> assertThat(e.arguments()).containsExactly("shisho-baseline.yaml", "licoes"));
    }
}

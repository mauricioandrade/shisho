package dev.mauricioandrade.shisho.infrastructure.config;

import dev.mauricioandrade.shisho.domain.exception.ConfigException;
import dev.mauricioandrade.shisho.domain.model.ProjectConfig;
import dev.mauricioandrade.shisho.domain.model.ProjectConfig.KataSetting;
import dev.mauricioandrade.shisho.domain.model.ProjectContext;
import dev.mauricioandrade.shisho.domain.model.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YamlProjectConfigReaderTest {

    private final YamlProjectConfigReader reader = new YamlProjectConfigReader();

    @TempDir
    Path project;

    @Test
    void usesTheDefaultsWithoutAFile() {
        assertThat(reader.read(project)).isEqualTo(ProjectConfig.DEFAULT);
    }

    @Test
    void usesTheDefaultsForAnEmptyFileOrNoKatas() throws IOException {
        write("# nada por enquanto\n");
        assertThat(reader.read(project)).isEqualTo(ProjectConfig.DEFAULT);

        write("katas: {}\n");
        assertThat(reader.read(project)).isEqualTo(ProjectConfig.DEFAULT);
    }

    @Test
    void readsSwitchesAndSeverities() throws IOException {
        write("""
                katas:
                  system-out:
                    ativa: false
                  dto-not-record:
                    severidade: INFO
                  field-injection:
                    ativa: true
                    severidade: media
                """);

        assertThat(reader.read(project).katas()).isEqualTo(Map.of(
                "system-out", new KataSetting(false, Optional.empty()),
                "dto-not-record", new KataSetting(true, Optional.of(Severity.INFO)),
                "field-injection", new KataSetting(true, Optional.of(Severity.MEDIA))));
    }

    @Test
    void turnsTheDeclaredContextIntoCatalogKeys() throws IOException {
        write("""
                contexto:
                  tipo: MVP
                  time: 12
                  vida_util: longa
                  caracteristicas: [dominio_com_regras_complexas, troca_provavel_de_infra]
                """);

        ProjectConfig config = reader.read(project);
        assertThat(config.katas()).isEmpty();
        assertThat(config.context()).contains(new ProjectContext(Set.of(
                "mvp", "time_grande", "vida_util_longa", "dominio_com_regras_complexas", "troca_provavel_de_infra")));
    }

    @Test
    void aSmallShortLivedPortfolioHasNoKeysButIsStillADeclaredContext() throws IOException {
        write("contexto:\n  tipo: portfolio\n  time: 1\n  vida_util: curta\n");

        assertThat(reader.read(project).context()).contains(new ProjectContext(Set.of()));
    }

    @Test
    void rejectsInvalidContexts() throws IOException {
        write("contexto:\n  tipo: startup\n");
        assertError("config.error.invalid-value", "shisho.yaml", "contexto.tipo", "startup");

        write("contexto:\n  time: 0\n");
        assertError("config.error.invalid-value", "shisho.yaml", "contexto.time", "0");

        write("contexto:\n  time: muitos\n");
        assertError("config.error.invalid-value", "shisho.yaml", "contexto.time", "muitos");

        write("contexto:\n  caracteristicas: [dominio_complexo]\n");
        assertError("config.error.invalid-value", "shisho.yaml", "contexto.caracteristicas", "dominio_complexo");

        write("contexto:\n  carga: alta\n");
        assertError("config.error.unknown-field", "shisho.yaml", "contexto.carga");
    }

    @Test
    void rejectsInvalidYaml() throws IOException {
        write("katas: [system-out\n");

        assertThatThrownBy(() -> reader.read(project))
                .isInstanceOfSatisfying(ConfigException.class, e ->
                        assertThat(e.messageKey()).isEqualTo("config.error.invalid-yaml"));
    }

    @Test
    void rejectsDuplicateKatas() throws IOException {
        write("""
                katas:
                  system-out:
                    ativa: false
                  system-out:
                    ativa: true
                """);

        assertThatThrownBy(() -> reader.read(project))
                .isInstanceOfSatisfying(ConfigException.class, e ->
                        assertThat(e.messageKey()).isEqualTo("config.error.invalid-yaml"));
    }

    @Test
    void rejectsADocumentWithoutFields() throws IOException {
        write("- system-out\n");

        assertError("config.error.invalid-document", "shisho.yaml");
    }

    @Test
    void rejectsUnknownFields() throws IOException {
        write("kata:\n  system-out:\n    ativa: false\n");
        assertError("config.error.unknown-field", "shisho.yaml", "kata");

        write("katas:\n  system-out:\n    ativo: false\n");
        assertError("config.error.unknown-field", "shisho.yaml", "katas.system-out.ativo");
    }

    @Test
    void rejectsInvalidValues() throws IOException {
        write("katas: system-out\n");
        assertError("config.error.invalid-value", "shisho.yaml", "katas", "system-out");

        write("katas:\n  system-out: false\n");
        assertError("config.error.invalid-value", "shisho.yaml", "katas.system-out", "false");

        write("katas:\n  system-out:\n    ativa: talvez\n");
        assertError("config.error.invalid-value", "shisho.yaml", "katas.system-out.ativa", "talvez");

        write("katas:\n  system-out:\n    severidade: altíssima\n");
        assertError("config.error.invalid-value", "shisho.yaml", "katas.system-out.severidade", "altíssima");
    }

    private void write(String content) throws IOException {
        Files.writeString(project.resolve("shisho.yaml"), content);
    }

    private void assertError(String messageKey, Object... arguments) {
        assertThatThrownBy(() -> reader.read(project))
                .isInstanceOfSatisfying(ConfigException.class, e -> {
                    assertThat(e.messageKey()).isEqualTo(messageKey);
                    assertThat(e.arguments()).containsExactly(arguments);
                });
    }
}

package dev.mauricioandrade.shisho;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** The Claude Code plugin's hook, end to end: the hook's JSON in, the edited file's lessons out. */
class ClaudeHookTest {

    private static final Path MESSY = Path.of("src/test/resources/fixtures/aluno-baguncado");
    private static final String CONTROLLER = "src/main/java/com/exemplo/loja/controller/UsuarioController.java";

    @TempDir
    Path project;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Test
    void showsClaudeTheLessonsOfTheFileItJustEdited() throws IOException {
        copy(MESSY, project);

        assertThat(hook(json(project.resolve(CONTROLLER).toString(), "/"))).isEqualTo(2);

        assertThat(err()).startsWith("師匠 Shishō: 5 lições em UsuarioController.java, que você acabou de editar:\n\n")
                .contains("[field-injection]", "[request-body-without-valid]", "[entity-in-controller]")
                .doesNotContain("UsuarioService.java", "pom.xml")
                .contains("\nCorrija o que fizer sentido.");
        assertThat(out.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void resolvesARelativePathAgainstTheSessionDirectory() throws IOException {
        copy(MESSY, project);

        assertThat(hook(json(CONTROLLER, project.toString()))).isEqualTo(2);
    }

    @Test
    void findsTheRootOfAGradleProject() throws IOException {
        copy(MESSY, project);
        Files.delete(project.resolve("pom.xml"));
        Files.writeString(project.resolve("build.gradle"), "dependencies { implementation 'org.springframework.boot:spring-boot-starter-web' }");

        assertThat(hook(json(project.resolve(CONTROLLER).toString(), "/"))).isEqualTo(2);
        assertThat(err()).contains("[field-injection]");
    }

    @Test
    void leavesTheSuggestionsOut() throws IOException {
        copy(MESSY, project);

        assertThat(hook(json(project.resolve("pom.xml").toString(), "/"))).isEqualTo(2);
        assertThat(err()).contains("[missing-actuator]").doesNotContain("[missing-springdoc]");
    }

    @Test
    void staysQuietWhenThereIsNothingToSay() throws IOException {
        copy(MESSY, project);
        Path clean = project.resolve("src/main/java/com/exemplo/loja/Limpa.java");
        Files.writeString(clean, "package com.exemplo.loja;\n\nrecord Limpa(String nome) {\n}\n");

        assertThat(hook(json(clean.toString(), "/"))).as("a file without lessons").isZero();
        assertThat(hook(json("/tmp/fora/de/um/projeto/A.java", "/"))).as("no build above").isZero();
        assertThat(hook("not json")).as("not the hook's JSON").isZero();
        assertThat(hook("{\"tool_input\": {\"command\": \"ls\"}}")).as("no file_path").isZero();
    }

    private int hook(String json) {
        out.reset();
        err.reset();
        return ShishoBootstrap.run(new String[] {"claude-hook"}, null,
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), out, err);
    }

    private String err() {
        return err.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }

    private static String json(String filePath, String cwd) {
        return """
                {"session_id": "s", "hook_event_name": "PostToolUse", "tool_name": "Edit", "cwd": "%s",
                 "tool_input": {"file_path": "%s", "old_string": "a", "new_string": "b"}}
                """.formatted(cwd, filePath);
    }

    private static void copy(Path source, Path target) throws IOException {
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
}

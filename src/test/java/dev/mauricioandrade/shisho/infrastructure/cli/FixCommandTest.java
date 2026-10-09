package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.FileFix;
import dev.mauricioandrade.shisho.domain.model.FixPlan;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FixCommandTest {

    private static final Lesson FIXED = lesson("non-final-dependency", "A.java");
    private static final Lesson UNFIXED = lesson("system-out", "B.java");

    @TempDir
    Path project;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Test
    void showsTheDiffAndWritesNothingByDefault() throws IOException {
        Files.writeString(project.resolve("A.java"), "class A {}\n");

        assertThat(run(plan("class A {}\n"))).isZero();

        assertThat(text(out)).isEqualTo("""
                --- a/A.java
                +++ b/A.java
                @@ -1,1 +1,1 @@
                -class A {}
                +final class A {}
                Correções possíveis: 1 lição em 1 arquivo. Rode de novo com --apply para gravá-las.
                1 lição sem correção automática. Veja como corrigir com: shisho analyze --sensei
                """);
        assertThat(Files.readString(project.resolve("A.java"))).isEqualTo("class A {}\n");
    }

    @Test
    void writesTheCorrectionsWithApply() throws IOException {
        Files.writeString(project.resolve("A.java"), "class A {}\n");

        assertThat(run(plan("class A {}\n"), "--apply")).isZero();

        assertThat(Files.readString(project.resolve("A.java"))).isEqualTo("final class A {}\n");
        assertThat(text(out)).startsWith("Corrigido: 1 lição em 1 arquivo.\n");
    }

    @Test
    void neverOverwritesAFileThatChangedSinceTheAnalysis() throws IOException {
        Files.writeString(project.resolve("A.java"), "class A { int edited; }\n");

        assertThat(run(plan("class A {}\n"), "--apply")).isEqualTo(3);

        assertThat(Files.readString(project.resolve("A.java"))).isEqualTo("class A { int edited; }\n");
        assertThat(text(err)).isEqualTo("A.java mudou desde a análise e não foi gravado.\n");
    }

    private FixPlan plan(String original) {
        Analysis analysis = new Analysis(project, "p", new JavaVersion(21), true, Optional.empty(), List.of(FIXED, UNFIXED),
                List.of(), 95, Rank.DAN_1);
        return new FixPlan(analysis, List.of(new FileFix(Path.of("A.java"), original, "final class A {}\n", List.of(FIXED))));
    }

    private int run(FixPlan plan, String... options) {
        Messages messages = new Messages(Messages.DEFAULT_LOCALE);
        FixCommand command = new FixCommand(root -> plan, messages,
                new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
        String[] arguments = new String[options.length + 1];
        System.arraycopy(options, 0, arguments, 0, options.length);
        arguments[options.length] = project.toString();
        return new CommandLine(command).setResourceBundle(messages.bundle()).execute(arguments);
    }

    private static String text(ByteArrayOutputStream stream) {
        return stream.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }

    private static Lesson lesson(String kata, String file) {
        return new Lesson(kata, Severity.MEDIA, Path.of(file), 1, "kata." + kata + ".lesson", List.of("x"));
    }
}

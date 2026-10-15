package dev.mauricioandrade.shisho.infrastructure.build;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BuildFileReadersTest {

    private final BuildFileReaders reader = BuildFileReaders.all();

    @TempDir
    Path project;

    @Test
    void readsGradleWhenThereIsNoPom() throws IOException {
        Files.writeString(project.resolve("build.gradle.kts"), "");

        assertThat(reader.read(project).orElseThrow().file()).isEqualTo(Path.of("build.gradle.kts"));
    }

    @Test
    void prefersThePom() throws IOException {
        Files.writeString(project.resolve("build.gradle"), "");
        Files.writeString(project.resolve("pom.xml"), "<project/>");

        assertThat(reader.read(project).orElseThrow().file()).isEqualTo(Path.of("pom.xml"));
    }

    @Test
    void returnsEmptyWithoutABuild() {
        assertThat(reader.read(project)).isEmpty();
    }
}

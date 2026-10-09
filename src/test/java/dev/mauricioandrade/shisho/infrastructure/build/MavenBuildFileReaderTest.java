package dev.mauricioandrade.shisho.infrastructure.build;

import dev.mauricioandrade.shisho.domain.exception.BuildFileException;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenBuildFileReaderTest {

    private final MavenBuildFileReader reader = new MavenBuildFileReader();

    @TempDir
    Path project;

    @Test
    void readsShishoItself() {
        BuildInfo build = reader.read(Path.of("")).orElseThrow();

        assertThat(build.file()).isEqualTo(Path.of("pom.xml"));
        assertThat(build.javaVersion()).contains(new JavaVersion(25));
        assertThat(build.dependencies()).contains(new Dependency("org.yaml", "snakeyaml"));
        assertThat(build.dependsOn("info.picocli", "picocli")).isTrue();
        assertThat(build.dependsOn("info.picocli", "picocli-codegen")).isFalse();
    }

    @Test
    void returnsEmptyWhenThereIsNoPom() {
        assertThat(reader.read(project)).isEmpty();
    }

    @Test
    void readsCompilerReleaseProperty() throws IOException {
        writePom(properties("<maven.compiler.release>21</maven.compiler.release>"));

        assertThat(javaVersion()).contains(new JavaVersion(21));
    }

    @Test
    void readsSpringBootJavaVersionProperty() throws IOException {
        writePom("""
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>3.5.6</version>
                </parent>
                """ + properties("<java.version>21</java.version>"));

        assertThat(javaVersion()).contains(new JavaVersion(21));
    }

    @Test
    void releaseWinsOverJavaVersion() throws IOException {
        writePom(properties("<java.version>21</java.version><maven.compiler.release>25</maven.compiler.release>"));

        assertThat(javaVersion()).contains(new JavaVersion(25));
    }

    @Test
    void compilerPluginReleaseWinsOverProperties() throws IOException {
        writePom(properties("<maven.compiler.release>21</maven.compiler.release><jdk>25</jdk>")
                + compilerPlugin("<release>${jdk}</release>"));

        assertThat(javaVersion()).contains(new JavaVersion(25));
    }

    @Test
    void fallsBackToSourceInLegacyNotation() throws IOException {
        writePom(compilerPlugin("<source>1.8</source><target>1.8</target>"));

        assertThat(javaVersion()).contains(new JavaVersion(8));
    }

    @Test
    void resolvesNestedPlaceholders() throws IOException {
        writePom(properties("""
                <lts>21</lts>
                <java.version>${lts}</java.version>
                <maven.compiler.release>${java.version}</maven.compiler.release>
                """));

        assertThat(javaVersion()).contains(new JavaVersion(21));
    }

    @Test
    void skipsPlaceholdersDefinedInARemoteParent() throws IOException {
        writePom(properties("<maven.compiler.release>${parent.java}</maven.compiler.release><maven.compiler.source>17</maven.compiler.source>"));

        assertThat(javaVersion()).contains(new JavaVersion(17));
    }

    @Test
    void survivesCyclicPlaceholders() throws IOException {
        writePom(properties("<a>${b}</a><b>${a}</b><maven.compiler.release>${a}</maven.compiler.release>"));

        assertThat(javaVersion()).isEmpty();
    }

    @Test
    void javaVersionIsEmptyWhenNotDeclared() throws IOException {
        writePom("");

        assertThat(javaVersion()).isEmpty();
    }

    @Test
    void rejectsUnrecognizedJavaVersion() throws IOException {
        writePom(properties("<java.version>vinte e um</java.version>"));

        assertThatThrownBy(() -> reader.read(project))
                .isInstanceOfSatisfying(BuildFileException.class, e -> {
                    assertThat(e.messageKey()).isEqualTo("build.error.invalid-java-version");
                    assertThat(e.arguments()).last().isEqualTo("vinte e um");
                });
    }

    @Test
    void readsOnlyDirectDependencies() throws IOException {
        writePom("""
                <dependencyManagement>
                    <dependencies>
                        <dependency><groupId>managed</groupId><artifactId>bom</artifactId></dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <optional>true</optional>
                    </dependency>
                </dependencies>
                <build>
                    <plugins>
                        <plugin>
                            <artifactId>some-plugin</artifactId>
                            <dependencies>
                                <dependency><groupId>plugin</groupId><artifactId>dep</artifactId></dependency>
                            </dependencies>
                        </plugin>
                    </plugins>
                </build>
                """);

        assertThat(reader.read(project).orElseThrow().dependencies()).containsExactly(
                new Dependency("org.springframework.boot", "spring-boot-starter-web"),
                new Dependency("org.projectlombok", "lombok"));
    }

    @Test
    void rejectsMalformedXml() throws IOException {
        Files.writeString(project.resolve("pom.xml"), "<project><dependencies></project>");

        assertError("build.error.invalid-xml");
    }

    @Test
    void rejectsRootOtherThanProject() throws IOException {
        Files.writeString(project.resolve("pom.xml"), "<settings/>");

        assertError("build.error.not-a-pom");
    }

    @Test
    void refusesDoctypeToPreventExternalEntities() throws IOException {
        Files.writeString(project.resolve("pom.xml"), """
                <?xml version="1.0"?>
                <!DOCTYPE project [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <project><properties><java.version>&secret;</java.version></properties></project>
                """);

        assertError("build.error.invalid-xml");
    }

    private Optional<JavaVersion> javaVersion() {
        return reader.read(project).orElseThrow().javaVersion();
    }

    private void assertError(String messageKey) {
        assertThatThrownBy(() -> reader.read(project))
                .isInstanceOfSatisfying(BuildFileException.class, e ->
                        assertThat(e.messageKey()).isEqualTo(messageKey));
    }

    private void writePom(String body) throws IOException {
        Files.writeString(project.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>aluno</artifactId>
                    <version>1</version>
                %s</project>
                """.formatted(body));
    }

    private static String properties(String content) {
        return "<properties>" + content + "</properties>\n";
    }

    private static String compilerPlugin(String configuration) {
        return """
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <configuration>%s</configuration>
                        </plugin>
                    </plugins>
                </build>
                """.formatted(configuration);
    }
}

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

class GradleBuildFileReaderTest {

    private final GradleBuildFileReader reader = new GradleBuildFileReader();

    @TempDir
    Path project;

    @Test
    void returnsEmptyWhenThereIsNoScript() {
        assertThat(reader.read(project)).isEmpty();
    }

    @Test
    void readsSpringInitializrKotlinScript() throws IOException {
        writeKotlin("""
                plugins {
                	java
                	id("org.springframework.boot") version "3.5.6"
                	id("io.spring.dependency-management") version "1.1.7"
                }

                java {
                	toolchain {
                		languageVersion = JavaLanguageVersion.of(21)
                	}
                }

                dependencies {
                	implementation("org.springframework.boot:spring-boot-starter-web")
                	compileOnly("org.projectlombok:lombok")
                	annotationProcessor("org.projectlombok:lombok")
                	testImplementation("org.springframework.boot:spring-boot-starter-test")
                	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                }
                """);

        BuildInfo build = reader.read(project).orElseThrow();

        assertThat(build.file()).isEqualTo(Path.of("build.gradle.kts"));
        assertThat(build.javaVersion()).contains(new JavaVersion(21));
        assertThat(build.dependencies()).containsExactly(
                new Dependency("org.springframework.boot", "spring-boot-starter-web"),
                new Dependency("org.projectlombok", "lombok"),
                new Dependency("org.springframework.boot", "spring-boot-starter-test"),
                new Dependency("org.junit.platform", "junit-platform-launcher"));
    }

    @Test
    void readsQuarkusGroovyScriptWithoutItsPlatform() throws IOException {
        writeGroovy("""
                plugins {
                    id 'java'
                    id 'io.quarkus'
                }

                dependencies {
                    implementation enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}")
                    implementation platform('io.quarkus.platform:quarkus-camel-bom:3.27.0')
                    implementation 'io.quarkus:quarkus-rest'
                    implementation 'io.quarkus:quarkus-arc'
                    testImplementation 'io.quarkus:quarkus-junit5'
                }

                java {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
                """);

        BuildInfo build = reader.read(project).orElseThrow();

        assertThat(build.file()).isEqualTo(Path.of("build.gradle"));
        assertThat(build.javaVersion()).contains(new JavaVersion(21));
        assertThat(build.dependencies()).containsExactly(
                new Dependency("io.quarkus", "quarkus-rest"),
                new Dependency("io.quarkus", "quarkus-arc"),
                new Dependency("io.quarkus", "quarkus-junit5"));
    }

    @Test
    void prefersTheKotlinScript() throws IOException {
        writeKotlin("java { sourceCompatibility = JavaVersion.VERSION_25 }");
        writeGroovy("java { sourceCompatibility = JavaVersion.VERSION_21 }");

        assertThat(javaVersion()).contains(new JavaVersion(25));
    }

    @Test
    void releaseWinsOverToolchain() throws IOException {
        writeKotlin("""
                java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
                tasks.withType<JavaCompile> { options.release.set(21) }
                """);

        assertThat(javaVersion()).contains(new JavaVersion(21));
    }

    @Test
    void toolchainWinsOverSourceCompatibility() throws IOException {
        writeGroovy("""
                sourceCompatibility = '17'
                java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
                """);

        assertThat(javaVersion()).contains(new JavaVersion(25));
    }

    @Test
    void readsSourceCompatibilityInItsSeveralForms() throws IOException {
        writeGroovy("sourceCompatibility = '21'");
        assertThat(javaVersion()).contains(new JavaVersion(21));

        writeGroovy("java.sourceCompatibility = JavaVersion.toVersion(24)");
        assertThat(javaVersion()).contains(new JavaVersion(24));

        writeGroovy("sourceCompatibility = JavaVersion.VERSION_1_8");
        assertThat(javaVersion()).contains(new JavaVersion(8));
    }

    @Test
    void javaVersionIsEmptyWhenComputedByTheScript() throws IOException {
        writeKotlin("java { toolchain { languageVersion = JavaLanguageVersion.of(property(\"jdk\").toString()) } }");

        assertThat(javaVersion()).isEmpty();
    }

    @Test
    void rejectsUnrecognizedJavaVersion() throws IOException {
        writeKotlin("java { toolchain { languageVersion = JavaLanguageVersion.of(0) } }");

        assertThatThrownBy(() -> reader.read(project))
                .isInstanceOfSatisfying(BuildFileException.class, e -> {
                    assertThat(e.messageKey()).isEqualTo("build.error.invalid-java-version");
                    assertThat(e.arguments()).last().isEqualTo("0");
                });
    }

    @Test
    void readsMapNotation() throws IOException {
        writeGroovy("""
                dependencies {
                    implementation group: 'org.springframework.boot', name: 'spring-boot-starter-web', version: '3.5.6'
                }
                """);
        assertThat(reader.read(project).orElseThrow().dependencies())
                .containsExactly(new Dependency("org.springframework.boot", "spring-boot-starter-web"));

        writeGroovy("""
                dependencies {
                    implementation(group = "org.springdoc", name = "springdoc-openapi-starter-webmvc-ui")
                }
                """);
        assertThat(reader.read(project).orElseThrow().dependencies())
                .containsExactly(new Dependency("org.springdoc", "springdoc-openapi-starter-webmvc-ui"));
    }

    @Test
    void readsOnlyTopLevelDependenciesOutsideComments() throws IOException {
        writeGroovy("""
                buildscript {
                    dependencies {
                        classpath 'org.flywaydb:flyway-gradle-plugin:11.0.0'
                    }
                }
                dependencies {
                    // implementation 'org.projectlombok:lombok'
                    /* implementation 'commented:out' */
                    implementation('org.springframework.boot:spring-boot-starter-web') {
                        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
                    }
                    implementation 'org.springframework.boot:spring-boot-starter-undertow' // the server
                    implementation project(':core')
                    implementation files('libs/legacy.jar')
                }
                """);

        assertThat(reader.read(project).orElseThrow().dependencies()).containsExactly(
                new Dependency("org.springframework.boot", "spring-boot-starter-web"),
                new Dependency("org.springframework.boot", "spring-boot-starter-undertow"));
    }

    @Test
    void urlsInStringsAreNotComments() throws IOException {
        writeGroovy("""
                repositories { maven { url 'https://repo.example.com/{releases}' } }
                dependencies {
                    implementation 'org.springframework.boot:spring-boot-starter-web'
                }
                """);

        assertThat(reader.read(project).orElseThrow().dependencies())
                .containsExactly(new Dependency("org.springframework.boot", "spring-boot-starter-web"));
    }

    private Optional<JavaVersion> javaVersion() {
        return reader.read(project).orElseThrow().javaVersion();
    }

    private void writeKotlin(String script) throws IOException {
        Files.writeString(project.resolve("build.gradle.kts"), script);
    }

    private void writeGroovy(String script) throws IOException {
        Files.writeString(project.resolve("build.gradle"), script);
    }
}

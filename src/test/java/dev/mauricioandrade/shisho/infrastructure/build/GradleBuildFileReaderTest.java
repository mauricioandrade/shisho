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

    @Test
    void readsLibrariesFromTheVersionCatalog() throws IOException {
        Files.createDirectories(project.resolve("gradle"));
        Files.writeString(project.resolve("gradle/libs.versions.toml"), """
                [versions]
                spring-boot = "3.5.6" # the [libraries] below is not this one

                [libraries]
                spring-boot-bom = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" }
                spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
                "lombok" = "org.projectlombok:lombok:1.18.42"
                springdoc_webmvc = { group = "org.springdoc", name = "springdoc-openapi-starter-webmvc-ui", version = "2.8.13" }
                unused = { module = "com.example:unused" }

                [bundles]
                web = ["spring-boot-starter-web", "springdoc_webmvc"]

                [plugins]
                spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
                """);
        writeKotlin("""
                plugins {
                    alias(libs.plugins.spring.boot)
                }
                dependencies {
                    implementation(platform(libs.spring.boot.bom))
                    implementation(libs.spring.boot.starter.web)
                    compileOnly(libs.lombok)
                    implementation(libs.springdoc.webmvc)
                    implementation(libs.not.in.the.catalog)
                }
                """);

        assertThat(reader.read(project).orElseThrow().dependencies()).containsExactly(
                new Dependency("org.springframework.boot", "spring-boot-starter-web"),
                new Dependency("org.projectlombok", "lombok"),
                new Dependency("org.springdoc", "springdoc-openapi-starter-webmvc-ui"));
    }

    @Test
    void readsTheLibrariesOfABundle() throws IOException {
        Files.createDirectories(project.resolve("gradle"));
        Files.writeString(project.resolve("gradle/libs.versions.toml"), """
                [bundles]
                web = [
                    "spring-boot-starter-web", # the bundles may come before the libraries
                    "springdoc_webmvc",
                    "not-a-library",
                ]
                ops = ["spring-boot-starter-actuator"]

                [libraries]
                spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
                spring-boot-starter-actuator = { module = "org.springframework.boot:spring-boot-starter-actuator" }
                springdoc_webmvc = { group = "org.springdoc", name = "springdoc-openapi-starter-webmvc-ui" }
                """);
        writeKotlin("""
                dependencies {
                    implementation(libs.bundles.web)
                    implementation(libs.bundles.ops)
                    implementation(libs.bundles.unknown)
                }
                """);

        assertThat(reader.read(project).orElseThrow().dependencies()).containsExactly(
                new Dependency("org.springframework.boot", "spring-boot-starter-web"),
                new Dependency("org.springdoc", "springdoc-openapi-starter-webmvc-ui"),
                new Dependency("org.springframework.boot", "spring-boot-starter-actuator"));
    }

    @Test
    void readsTheDependenciesTheRootSharesWithItsModules() throws IOException {
        writeGroovy("""
                buildscript {
                    dependencies { classpath 'org.springframework.boot:spring-boot-gradle-plugin:3.5.6' }
                }
                allprojects {
                    repositories { mavenCentral() }
                    dependencies { testImplementation 'org.junit.jupiter:junit-jupiter' }
                }
                subprojects {
                    buildscript {
                        dependencies { classpath 'com.example:plugin' }
                    }
                    dependencies {
                        implementation 'org.springframework.boot:spring-boot-starter-actuator'
                    }
                }
                configurations {
                    subprojects { }
                }
                """);

        assertThat(reader.read(project).orElseThrow().dependencies()).containsExactly(
                new Dependency("org.junit.jupiter", "junit-jupiter"),
                new Dependency("org.springframework.boot", "spring-boot-starter-actuator"));
    }

    @Test
    void readsTheCatalogsTheSettingsCreate() throws IOException {
        Files.writeString(project.resolve("settings.gradle.kts"), """
                dependencyResolutionManagement {
                    versionCatalogs {
                        create("spring") { from(files("gradle/spring.versions.toml")) }
                        create("remoto") { from("com.example:catalog:1.0") }
                        create("fora") { from(files("../outro/libs.versions.toml")) }
                    }
                }
                """);
        Files.createDirectories(project.resolve("gradle"));
        Files.writeString(project.resolve("gradle/libs.versions.toml"), """
                [libraries]
                slf4j = { module = "org.slf4j:slf4j-api" }
                """);
        Files.writeString(project.resolve("gradle/spring.versions.toml"), """
                [libraries]
                boot-web = { module = "org.springframework.boot:spring-boot-starter-web" }
                """);
        writeKotlin("""
                dependencies {
                    implementation(spring.boot.web)
                    implementation(libs.slf4j)
                    implementation(spring.slf4j)
                    implementation(fora.anything)
                }
                """);

        assertThat(reader.read(project).orElseThrow().dependencies()).as("each accessor in its own catalog")
                .containsExactly(new Dependency("org.springframework.boot", "spring-boot-starter-web"), new Dependency("org.slf4j", "slf4j-api"));
    }

    @Test
    void readsACatalogTheGroovySettingsCreate() throws IOException {
        Files.writeString(project.resolve("settings.gradle"), """
                dependencyResolutionManagement {
                    versionCatalogs {
                        quarkus { from(files('gradle/quarkus.versions.toml')) }
                    }
                }
                """);
        Files.createDirectories(project.resolve("gradle"));
        Files.writeString(project.resolve("gradle/quarkus.versions.toml"), """
                [libraries]
                rest = { module = "io.quarkus:quarkus-rest" }
                """);
        writeGroovy("dependencies { implementation quarkus.rest }");

        assertThat(reader.read(project).orElseThrow().dependsOn("io.quarkus", "quarkus-rest")).isTrue();
    }

    @Test
    void readsCatalogAccessorsInGroovy() throws IOException {
        Files.createDirectories(project.resolve("gradle"));
        Files.writeString(project.resolve("gradle/libs.versions.toml"), """
                [libraries]
                quarkus-rest = { module = "io.quarkus:quarkus-rest" }
                """);
        writeGroovy("""
                dependencies {
                    implementation libs.quarkus.rest
                }
                """);

        assertThat(reader.read(project).orElseThrow().dependencies()).containsExactly(new Dependency("io.quarkus", "quarkus-rest"));
    }

    @Test
    void readsTheProjectsTheSettingsInclude() throws IOException {
        Files.writeString(project.resolve("settings.gradle.kts"), """
                rootProject.name = "loja"
                include(
                    "dominio",
                    ":app:web", // the application
                )
                // include("antigo")
                include("..")
                includeBuild("plugins")
                """);
        Files.createDirectories(project.resolve("gradle"));
        Files.writeString(project.resolve("gradle/libs.versions.toml"), """
                [libraries]
                spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
                """);
        Files.createDirectories(project.resolve("dominio"));
        Files.writeString(project.resolve("dominio/build.gradle.kts"), "java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }");
        Files.createDirectories(project.resolve("app/web"));
        Files.writeString(project.resolve("app/web/build.gradle"), """
                dependencies { implementation libs.spring.boot.starter.web }
                """);
        Files.createDirectories(project.resolve("antigo"));
        Files.writeString(project.resolve("antigo/build.gradle"), "dependencies { implementation 'commented:out' }");

        BuildInfo build = reader.read(project).orElseThrow();

        assertThat(build.file()).as("a root without a script of its own").isEqualTo(Path.of("settings.gradle.kts"));
        assertThat(build.modules()).extracting(BuildInfo::file)
                .containsExactly(Path.of("dominio/build.gradle.kts"), Path.of("app/web/build.gradle"));
        assertThat(build.fileDeclaring(new Dependency("org.springframework.boot", "spring-boot-starter-web")))
                .as("the catalog is the root's").isEqualTo(Path.of("app/web/build.gradle"));
        assertThat(build.lowestJavaVersion()).contains(new JavaVersion(25));
    }

    @Test
    void aProjectDirInTheSettingsWinsOverTheProjectPath() throws IOException {
        Files.writeString(project.resolve("settings.gradle"), """
                include ':app', ':dominio', ':fora', ':absoluto'
                project(':app').projectDir = file('modulos/app')
                project(":dominio").projectDir = new File(settingsDir, "modulos/dominio")
                project(':fora').projectDir = file('../fora')
                project(':absoluto').projectDir = file('/tmp/absoluto')
                """);
        writeGroovy("");
        Files.createDirectories(project.resolve("modulos/app"));
        Files.writeString(project.resolve("modulos/app/build.gradle"), "dependencies { implementation 'io.quarkus:quarkus-rest' }");
        Files.createDirectories(project.resolve("modulos/dominio"));
        Files.writeString(project.resolve("modulos/dominio/build.gradle.kts"), "");
        Files.createDirectories(project.resolve("app"));
        Files.writeString(project.resolve("app/build.gradle"), "dependencies { implementation 'not:this' }");

        BuildInfo build = reader.read(project).orElseThrow();

        assertThat(build.modules()).as("outside the project: skipped").extracting(BuildInfo::file)
                .containsExactly(Path.of("modulos/app/build.gradle"), Path.of("modulos/dominio/build.gradle.kts"));
        assertThat(build.dependsOn("io.quarkus", "quarkus-rest")).isTrue();
    }

    @Test
    void readsTheConventionPluginsOfBuildSrcAndOfIncludedBuilds() throws IOException {
        Files.writeString(project.resolve("settings.gradle.kts"), """
                pluginManagement { includeBuild("build-logic") }
                includeBuild("../fora")
                include("app")
                """);
        Files.createDirectories(project.resolve("app"));
        Files.writeString(project.resolve("app/build.gradle.kts"), "plugins { id(\"loja.web\") }");
        Files.createDirectories(project.resolve("buildSrc/src/main/kotlin/loja"));
        Files.writeString(project.resolve("buildSrc/build.gradle.kts"), """
                dependencies { implementation("org.springframework.boot:spring-boot-gradle-plugin:3.5.0") }
                """);
        Files.writeString(project.resolve("buildSrc/src/main/kotlin/loja/web.gradle.kts"), """
                package loja
                java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
                dependencies { implementation("org.springframework.boot:spring-boot-starter-web") }
                """);
        Files.createDirectories(project.resolve("build-logic/src/main/groovy"));
        Files.writeString(project.resolve("build-logic/src/main/groovy/loja.java.gradle"), "dependencies { implementation 'org.slf4j:slf4j-api' }");

        BuildInfo build = reader.read(project).orElseThrow();

        assertThat(build.modules()).as("buildSrc's own script declares Gradle plugins").extracting(BuildInfo::file)
                .containsExactly(Path.of("app/build.gradle.kts"),
                        Path.of("buildSrc/src/main/kotlin/loja/web.gradle.kts"),
                        Path.of("build-logic/src/main/groovy/loja.java.gradle"));
        assertThat(build.fileDeclaring(new Dependency("org.springframework.boot", "spring-boot-starter-web")))
                .isEqualTo(Path.of("buildSrc/src/main/kotlin/loja/web.gradle.kts"));
        assertThat(build.dependsOn("org.slf4j", "slf4j-api")).isTrue();
        assertThat(build.dependsOn("org.springframework.boot", "spring-boot-gradle-plugin")).isFalse();
        assertThat(build.lowestJavaVersion()).as("the toolchain the convention sets").contains(new JavaVersion(21));
    }

    @Test
    void readsBuildSrcWithoutSettings() throws IOException {
        writeKotlin("plugins { `java-conventions` }");
        Files.createDirectories(project.resolve("buildSrc/src/main/kotlin"));
        Files.writeString(project.resolve("buildSrc/src/main/kotlin/java-conventions.gradle.kts"), "dependencies { implementation(\"io.quarkus:quarkus-rest\") }");

        assertThat(reader.read(project).orElseThrow().dependsOn("io.quarkus", "quarkus-rest")).isTrue();
    }

    @Test
    void aSingleProjectWithSettingsHasNoModules() throws IOException {
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'loja'");
        writeGroovy("dependencies { implementation 'io.quarkus:quarkus-rest' }");

        BuildInfo build = reader.read(project).orElseThrow();

        assertThat(build.file()).isEqualTo(Path.of("build.gradle"));
        assertThat(build.modules()).isEmpty();
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

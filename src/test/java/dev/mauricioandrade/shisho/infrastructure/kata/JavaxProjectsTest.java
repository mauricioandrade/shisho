package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.classes;
import static org.assertj.core.api.Assertions.assertThat;

class JavaxProjectsTest {

    @Test
    void aProjectImportingTheRenamedPackagesIsJavax() {
        assertThat(JavaxProjects.isJavax(project("import javax.validation.constraints.*; class A {}"))).isTrue();
        assertThat(JavaxProjects.valid(project("import javax.servlet.http.HttpServletRequest; class A {}")))
                .isEqualTo("javax.validation.Valid");
    }

    @Test
    void anyJakartaImportMakesItJakarta() {
        assertThat(JavaxProjects.isJavax(project("""
                import javax.persistence.Entity;
                import jakarta.validation.Valid;
                class A {}
                """))).as("halfway through the migration, the new namespace is already there").isFalse();
    }

    @Test
    void javaxAnnotationAndOtherJdkPackagesDoNotCount() {
        assertThat(JavaxProjects.isJavax(project("""
                import javax.annotation.Nullable;
                import javax.crypto.Cipher;
                class A {}
                """))).isFalse();
        assertThat(JavaxProjects.valid(project("class A {}"))).isEqualTo("jakarta.validation.Valid");
    }

    @Test
    void thePlatformVersionTheBuildPinsWinsOverTheImports() {
        String jdbcOnly = "import javax.sql.DataSource; class A {}";
        assertThat(JavaxProjects.isJavax(project(jdbcOnly, Map.of("org.springframework.boot", "2.2.0.M6"))))
                .as("ddd-by-examples/library: Boot 2.2, nothing renamed imported").isTrue();
        assertThat(JavaxProjects.isJavax(project("import javax.persistence.Entity; class A {}", Map.of("io.quarkus.platform", "3.15.1"))))
                .isFalse();
    }

    private static ProjectModel project(String source) {
        return new ProjectModel(new JavaVersion(17), Optional.empty(), classes(source));
    }

    private static ProjectModel project(String source, Map<String, String> platformVersions) {
        BuildInfo build = new BuildInfo(Path.of("pom.xml"), Optional.empty(), List.of(), List.of(), platformVersions);
        return new ProjectModel(new JavaVersion(17), Optional.of(build), classes(source));
    }
}
